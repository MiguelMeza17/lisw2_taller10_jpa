package co.edu.unicauca.questionbank.application;

import co.edu.unicauca.questionbank.application.command.CreateQuestionCommand;
import co.edu.unicauca.questionbank.application.command.QuestionCriteria;
import co.edu.unicauca.questionbank.application.exception.ResourceNotFoundException;
import co.edu.unicauca.questionbank.domain.exception.BusinessRuleException;
import co.edu.unicauca.questionbank.domain.model.AnswerOption;
import co.edu.unicauca.questionbank.domain.model.Competency;
import co.edu.unicauca.questionbank.domain.model.MultipleChoiceQuestion;
import co.edu.unicauca.questionbank.domain.model.Question;
import co.edu.unicauca.questionbank.domain.model.QuestionMetadata;
import co.edu.unicauca.questionbank.domain.model.QuestionStatus;
import co.edu.unicauca.questionbank.domain.model.Topic;
import co.edu.unicauca.questionbank.domain.model.TrueFalseQuestion;
import co.edu.unicauca.questionbank.domain.model.User;
import co.edu.unicauca.questionbank.domain.repository.CompetencyRepository;
import co.edu.unicauca.questionbank.domain.repository.QuestionRepository;
import co.edu.unicauca.questionbank.domain.repository.TopicRepository;
import co.edu.unicauca.questionbank.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Use cases of the questions: create, search, classify and move them through their life cycle.
 * <p>
 * The service only coordinates: it loads what it needs through the repository contracts of the domain,
 * asks the domain objects to apply their rules and stores the result. It never depends on JPA.
 */
@Service
@Transactional
public class QuestionService {

    private final QuestionRepository questions;
    private final UserRepository users;
    private final TopicRepository topics;
    private final CompetencyRepository competencies;

    /**
     * Creates the service.
     *
     * @param questions    question repository
     * @param users        user repository
     * @param topics       topic repository
     * @param competencies competency repository
     */
    public QuestionService(QuestionRepository questions, UserRepository users, TopicRepository topics,
                           CompetencyRepository competencies) {
        this.questions = questions;
        this.users = users;
        this.topics = topics;
        this.competencies = competencies;
    }

    // ---------------------------------------------------------------- 1. create

    /**
     * Creates a draft question with its type, options, optional classification and optional metadata.
     *
     * @param command data of the question
     * @return the stored question
     */
    public Question create(CreateQuestionCommand command) {
        if (command == null) {
            throw new BusinessRuleException("The question data is required.");
        }
        if (command.type() == null) {
            throw new BusinessRuleException("The question type is required.");
        }
        User author = users.findById(requireId(command.authorId(), "author"))
                .orElseThrow(() -> new ResourceNotFoundException("User", command.authorId()));

        Question question = switch (command.type()) {
            case MULTIPLE_CHOICE -> newMultipleChoice(command, author);
            case TRUE_FALSE -> newTrueFalse(command, author);
        };

        boolean hasTopic = command.topicId() != null;
        boolean hasCompetencies = command.competencyIds() != null && !command.competencyIds().isEmpty();
        if (hasTopic || hasCompetencies) {
            question.classify(loadTopic(command.topicId()), loadCompetencies(command.competencyIds()));
        }
        if (command.metadata() != null) {
            CreateQuestionCommand.Metadata m = command.metadata();
            question.updateMetadata(new QuestionMetadata(null, m.cognitiveLevel(), m.estimatedTimeMinutes(),
                    m.editorialNotes()));
        }
        return questions.save(question);
    }

    private MultipleChoiceQuestion newMultipleChoice(CreateQuestionCommand command, User author) {
        if (command.correctAnswer() != null) {
            throw new BusinessRuleException("A multiple choice question does not use the true/false answer.");
        }
        MultipleChoiceQuestion question = new MultipleChoiceQuestion(command.statement(), author);
        if (command.options() != null) {
            for (CreateQuestionCommand.Option option : command.options()) {
                if (option == null) {
                    throw new BusinessRuleException("The answer option is required.");
                }
                question.addOption(new AnswerOption(option.text(), option.correct()));
            }
        }
        return question;
    }

    private TrueFalseQuestion newTrueFalse(CreateQuestionCommand command, User author) {
        if (command.options() != null && !command.options().isEmpty()) {
            throw new BusinessRuleException("A true/false question does not have answer options.");
        }
        if (command.correctAnswer() == null) {
            throw new BusinessRuleException("The expected answer of a true/false question is required.");
        }
        return new TrueFalseQuestion(command.statement(), author, command.correctAnswer());
    }

    // ---------------------------------------------------------------- 2. search

    /**
     * Finds a question by its identifier.
     *
     * @param id identifier
     * @return the question
     * @throws ResourceNotFoundException if it does not exist
     */
    @Transactional(readOnly = true)
    public Question findById(Long id) {
        return load(id);
    }

    /**
     * Searches questions that match every non-null filter of the criteria.
     *
     * @param criteria filters, may be {@code null} to list everything
     * @return matching questions
     */
    @Transactional(readOnly = true)
    public List<Question> search(QuestionCriteria criteria) {
        QuestionCriteria c = criteria == null ? QuestionCriteria.any() : criteria;

        // Use the most selective query offered by the repository and filter the rest in memory.
        List<Question> base;
        if (c.competencyId() != null && c.status() == QuestionStatus.PUBLISHED) {
            base = questions.findPublishedByCompetency(c.competencyId());
        } else if (c.authorId() != null) {
            base = questions.findByAuthor(c.authorId());
        } else if (c.status() != null) {
            base = questions.findByStatus(c.status());
        } else {
            base = questions.findAll();
        }

        Predicate<Question> matches = q -> true;
        if (c.authorId() != null) {
            matches = matches.and(q -> Objects.equals(q.getAuthor().getId(), c.authorId()));
        }
        if (c.status() != null) {
            matches = matches.and(q -> q.getStatus() == c.status());
        }
        if (c.topicId() != null) {
            matches = matches.and(q -> q.getTopic() != null && Objects.equals(q.getTopic().getId(), c.topicId()));
        }
        if (c.competencyId() != null) {
            matches = matches.and(q -> q.getCompetencies().stream()
                    .anyMatch(comp -> Objects.equals(comp.getId(), c.competencyId())));
        }
        return base.stream().filter(matches).toList();
    }

    // ---------------------------------------------------------------- 3. classify

    /**
     * Classifies a draft question with a topic and its competencies.
     *
     * @param questionId    question to classify
     * @param topicId       topic
     * @param competencyIds competencies, at least one
     * @return the updated question
     */
    public Question classify(Long questionId, Long topicId, Collection<Long> competencyIds) {
        Question question = load(questionId);
        question.classify(loadTopic(topicId), loadCompetencies(competencyIds));
        return questions.save(question);
    }

    // ---------------------------------------------------------------- 4. life cycle

    /**
     * Sends a draft to review (DRAFT → PENDING_REVIEW). The domain checks the minimum conditions:
     * classification and a valid structure for the question type.
     *
     * @param questionId question to submit
     * @return the updated question
     */
    public Question submitForReview(Long questionId) {
        return transition(questionId, Question::submitForReview);
    }

    /**
     * Starts the review of a question (PENDING_REVIEW → IN_REVIEW).
     *
     * @param questionId question
     * @return the updated question
     */
    public Question startReview(Long questionId) {
        return transition(questionId, Question::startReview);
    }

    /**
     * Approves a question under review (IN_REVIEW → APPROVED).
     *
     * @param questionId question
     * @return the updated question
     */
    public Question approve(Long questionId) {
        return transition(questionId, Question::approve);
    }

    /**
     * Rejects a question and sends it back to draft.
     *
     * @param questionId question
     * @return the updated question
     */
    public Question reject(Long questionId) {
        return transition(questionId, Question::reject);
    }

    /**
     * Publishes an approved question (APPROVED → PUBLISHED) so it can be used in quizzes.
     *
     * @param questionId question
     * @return the updated question
     */
    public Question publish(Long questionId) {
        return transition(questionId, Question::publish);
    }

    /**
     * Archives a published question (PUBLISHED → ARCHIVED).
     *
     * @param questionId question
     * @return the updated question
     */
    public Question archive(Long questionId) {
        return transition(questionId, Question::archive);
    }

    private Question transition(Long questionId, Consumer<Question> change) {
        Question question = load(questionId);
        change.accept(question);
        return questions.save(question);
    }

    // ---------------------------------------------------------------- helpers

    private Question load(Long id) {
        return questions.findById(requireId(id, "question"))
                .orElseThrow(() -> new ResourceNotFoundException("Question", id));
    }

    private Topic loadTopic(Long topicId) {
        return topics.findById(requireId(topicId, "topic"))
                .orElseThrow(() -> new ResourceNotFoundException("Topic", topicId));
    }

    private Set<Competency> loadCompetencies(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessRuleException("At least one competency is required to classify the question.");
        }
        Set<Long> unique = new LinkedHashSet<>(ids);
        if (unique.contains(null)) {
            throw new BusinessRuleException("The competency identifier is required.");
        }
        List<Competency> found = competencies.findAllById(unique);
        if (found.size() != unique.size()) {
            Set<Long> foundIds = new LinkedHashSet<>(found.stream().map(Competency::getId).toList());
            Long missing = unique.stream().filter(id -> !foundIds.contains(id)).findFirst().orElse(null);
            throw new ResourceNotFoundException("Competency", missing);
        }
        return new LinkedHashSet<>(found);
    }

    private static Long requireId(Long id, String what) {
        if (id == null) {
            throw new BusinessRuleException("The " + what + " identifier is required.");
        }
        return id;
    }
}
