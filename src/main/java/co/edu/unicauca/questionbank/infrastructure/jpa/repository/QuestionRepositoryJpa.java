package co.edu.unicauca.questionbank.infrastructure.jpa.repository;

import co.edu.unicauca.questionbank.domain.exception.BusinessRuleException;
import co.edu.unicauca.questionbank.domain.model.Competency;
import co.edu.unicauca.questionbank.domain.model.Question;
import co.edu.unicauca.questionbank.domain.model.QuestionStatus;
import co.edu.unicauca.questionbank.domain.repository.QuestionRepository;
import co.edu.unicauca.questionbank.infrastructure.jpa.mapper.DomainMapper;
import co.edu.unicauca.questionbank.infrastructure.jpa.record.CompetencyRecord;
import co.edu.unicauca.questionbank.infrastructure.jpa.record.QuestionRecord;
import co.edu.unicauca.questionbank.infrastructure.jpa.record.TopicRecord;
import co.edu.unicauca.questionbank.infrastructure.jpa.record.UserRecord;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * JPA implementation of {@link QuestionRepository}.
 * <p>
 * Every method runs inside a transaction, so lazy associations are loaded while the records are mapped
 * to domain objects and never escape the persistence context.
 */
@Repository
public class QuestionRepositoryJpa implements QuestionRepository {

    private final SpringQuestionDataRepository questions;
    private final SpringUserDataRepository users;
    private final SpringTopicDataRepository topics;
    private final SpringCompetencyDataRepository competencies;
    private final DomainMapper mapper;

    /**
     * Creates the repository.
     *
     * @param questions    Spring Data repository of questions
     * @param users        Spring Data repository of users
     * @param topics       Spring Data repository of topics
     * @param competencies Spring Data repository of competencies
     * @param mapper       domain/record mapper
     */
    public QuestionRepositoryJpa(SpringQuestionDataRepository questions, SpringUserDataRepository users,
                                 SpringTopicDataRepository topics, SpringCompetencyDataRepository competencies,
                                 DomainMapper mapper) {
        this.questions = questions;
        this.users = users;
        this.topics = topics;
        this.competencies = competencies;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public Question save(Question question) {
        UserRecord author = resolveAuthor(question);
        TopicRecord topic = resolveTopic(question);
        List<CompetencyRecord> competencyRecords = resolveCompetencies(question);

        QuestionRecord record = question.getId() == null
                ? mapper.newRecordFor(question)
                : questions.findById(question.getId()).orElseThrow(() ->
                        new BusinessRuleException("Question " + question.getId() + " does not exist."));

        mapper.copyToRecord(question, record, author, topic, competencyRecords);
        // Flushing right away lets Hibernate wrap the new collections, so later updates in the same
        // transaction detect removed options as orphans, and constraint violations surface here.
        return mapper.toDomain(questions.saveAndFlush(record));
    }

    private UserRecord resolveAuthor(Question question) {
        Long authorId = question.getAuthor().getId();
        if (authorId == null) {
            throw new BusinessRuleException("The author must be registered before writing questions.");
        }
        return users.findById(authorId)
                .orElseThrow(() -> new BusinessRuleException("Author " + authorId + " does not exist."));
    }

    private TopicRecord resolveTopic(Question question) {
        if (question.getTopic() == null) {
            return null;
        }
        Long topicId = question.getTopic().getId();
        if (topicId == null) {
            throw new BusinessRuleException("The topic must be registered before classifying questions.");
        }
        return topics.findById(topicId)
                .orElseThrow(() -> new BusinessRuleException("Topic " + topicId + " does not exist."));
    }

    private List<CompetencyRecord> resolveCompetencies(Question question) {
        List<Long> ids = question.getCompetencies().stream().map(Competency::getId).toList();
        if (ids.stream().anyMatch(java.util.Objects::isNull)) {
            throw new BusinessRuleException("Competencies must be registered before classifying questions.");
        }
        List<CompetencyRecord> found = competencies.findAllById(ids);
        if (found.size() != ids.size()) {
            throw new BusinessRuleException("One or more competencies do not exist.");
        }
        return found;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Question> findById(Long id) {
        return questions.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Question> findAll() {
        return questions.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Question> findByAuthor(Long authorId) {
        return questions.findByAuthorIdOrderByIdAsc(authorId).stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Question> findByStatus(QuestionStatus status) {
        return questions.findByStatusOrderByIdAsc(status).stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Question> findPublishedByCompetency(Long competencyId) {
        return questions.findByCompetenciesIdAndStatusOrderByIdAsc(competencyId, QuestionStatus.PUBLISHED)
                .stream().map(mapper::toDomain).toList();
    }
}
