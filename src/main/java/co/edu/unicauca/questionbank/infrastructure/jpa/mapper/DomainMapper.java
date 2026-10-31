package co.edu.unicauca.questionbank.infrastructure.jpa.mapper;

import co.edu.unicauca.questionbank.domain.model.AnswerOption;
import co.edu.unicauca.questionbank.domain.model.Competency;
import co.edu.unicauca.questionbank.domain.model.MultipleChoiceQuestion;
import co.edu.unicauca.questionbank.domain.model.Question;
import co.edu.unicauca.questionbank.domain.model.QuestionMetadata;
import co.edu.unicauca.questionbank.domain.model.Quiz;
import co.edu.unicauca.questionbank.domain.model.Topic;
import co.edu.unicauca.questionbank.domain.model.TrueFalseQuestion;
import co.edu.unicauca.questionbank.domain.model.User;
import co.edu.unicauca.questionbank.infrastructure.jpa.record.AnswerOptionRecord;
import co.edu.unicauca.questionbank.infrastructure.jpa.record.CompetencyRecord;
import co.edu.unicauca.questionbank.infrastructure.jpa.record.MultipleChoiceQuestionRecord;
import co.edu.unicauca.questionbank.infrastructure.jpa.record.QuestionMetadataRecord;
import co.edu.unicauca.questionbank.infrastructure.jpa.record.QuestionRecord;
import co.edu.unicauca.questionbank.infrastructure.jpa.record.QuizRecord;
import co.edu.unicauca.questionbank.infrastructure.jpa.record.TopicRecord;
import co.edu.unicauca.questionbank.infrastructure.jpa.record.TrueFalseQuestionRecord;
import co.edu.unicauca.questionbank.infrastructure.jpa.record.UserRecord;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

/**
 * Translates between domain objects and JPA records in both directions.
 * <p>
 * Related records (author, topic, competencies) are resolved by the repositories and passed in,
 * so the mapper never queries the database.
 */
@Component
public class DomainMapper {

    // ---------------------------------------------------------------- users, topics, competencies

    /**
     * Converts a user record to the domain.
     *
     * @param r record
     * @return domain user, or {@code null}
     */
    public User toDomain(UserRecord r) {
        return r == null ? null : new User(r.getId(), r.getFullName(), r.getEmail(), r.getRole());
    }

    /**
     * Converts a domain user to a record.
     *
     * @param u domain user
     * @return record with the same data
     */
    public UserRecord toRecord(User u) {
        UserRecord r = new UserRecord();
        r.setId(u.getId());
        r.setFullName(u.getFullName());
        r.setEmail(u.getEmail());
        r.setRole(u.getRole());
        return r;
    }

    /**
     * Converts a topic record to the domain.
     *
     * @param r record
     * @return domain topic, or {@code null}
     */
    public Topic toDomain(TopicRecord r) {
        return r == null ? null : new Topic(r.getId(), r.getName());
    }

    /**
     * Converts a domain topic to a record.
     *
     * @param t domain topic
     * @return record with the same data
     */
    public TopicRecord toRecord(Topic t) {
        TopicRecord r = new TopicRecord();
        r.setId(t.getId());
        r.setName(t.getName());
        return r;
    }

    /**
     * Converts a competency record to the domain.
     *
     * @param r record
     * @return domain competency, or {@code null}
     */
    public Competency toDomain(CompetencyRecord r) {
        return r == null ? null : new Competency(r.getId(), r.getCode(), r.getName());
    }

    /**
     * Converts a domain competency to a record.
     *
     * @param c domain competency
     * @return record with the same data
     */
    public CompetencyRecord toRecord(Competency c) {
        CompetencyRecord r = new CompetencyRecord();
        r.setId(c.getId());
        r.setCode(c.getCode());
        r.setName(c.getName());
        return r;
    }

    // ---------------------------------------------------------------- questions

    /**
     * Converts a question record to the matching domain subtype.
     *
     * @param r record of any question type
     * @return domain question
     */
    public Question toDomain(QuestionRecord r) {
        if (r == null) {
            return null;
        }
        User author = toDomain(r.getAuthor());
        Topic topic = toDomain(r.getTopic());
        List<Competency> competencies = r.getCompetencies().stream().map(this::toDomain).toList();
        QuestionMetadata metadata = toDomain(r.getMetadata());

        if (r instanceof MultipleChoiceQuestionRecord mc) {
            List<AnswerOption> options = mc.getOptions().stream()
                    .map(o -> new AnswerOption(o.getText(), o.isCorrect()))
                    .toList();
            return new MultipleChoiceQuestion(r.getId(), r.getStatement(), author, topic, competencies,
                    metadata, r.getStatus(), options);
        }
        if (r instanceof TrueFalseQuestionRecord tf) {
            return new TrueFalseQuestion(r.getId(), r.getStatement(), author, topic, competencies,
                    metadata, r.getStatus(), tf.isCorrectAnswer());
        }
        throw new IllegalArgumentException("Unsupported question record: " + r.getClass().getName());
    }

    /**
     * Creates an empty record of the right subtype for a domain question.
     *
     * @param q domain question
     * @return new, empty record
     */
    public QuestionRecord newRecordFor(Question q) {
        if (q instanceof MultipleChoiceQuestion) {
            return new MultipleChoiceQuestionRecord();
        }
        if (q instanceof TrueFalseQuestion) {
            return new TrueFalseQuestionRecord();
        }
        throw new IllegalArgumentException("Unsupported question type: " + q.getClass().getName());
    }

    /**
     * Copies the state of a domain question into a record (new or already managed by JPA).
     *
     * @param q            domain question
     * @param target       record to update, of the same subtype
     * @param author       record of the author
     * @param topic        record of the topic, may be {@code null}
     * @param competencies records of the competencies
     */
    public void copyToRecord(Question q, QuestionRecord target, UserRecord author, TopicRecord topic,
                             Collection<CompetencyRecord> competencies) {
        target.setStatement(q.getStatement());
        target.setStatus(q.getStatus());
        target.setAuthor(author);
        target.setTopic(topic);
        target.getCompetencies().clear();
        target.getCompetencies().addAll(competencies);
        copyMetadata(q.getMetadata(), target);

        if (q instanceof MultipleChoiceQuestion mc && target instanceof MultipleChoiceQuestionRecord mcr) {
            mcr.clearOptions();
            int position = 0;
            for (AnswerOption option : mc.getOptions()) {
                AnswerOptionRecord or = new AnswerOptionRecord();
                or.setText(option.getText());
                or.setCorrect(option.isCorrect());
                or.setPosition(position++);
                mcr.addOption(or);
            }
        } else if (q instanceof TrueFalseQuestion tf && target instanceof TrueFalseQuestionRecord tfr) {
            tfr.setCorrectAnswer(tf.getCorrectAnswer());
        } else {
            throw new IllegalArgumentException("Question type and record type do not match.");
        }
    }

    private void copyMetadata(QuestionMetadata m, QuestionRecord target) {
        if (m == null) {
            target.setMetadata(null);
            return;
        }
        QuestionMetadataRecord mr = target.getMetadata() != null ? target.getMetadata() : new QuestionMetadataRecord();
        mr.setLastReviewDate(m.getLastReviewDate());
        mr.setCognitiveLevel(m.getCognitiveLevel());
        mr.setEstimatedTimeMinutes(m.getEstimatedTimeMinutes());
        mr.setEditorialNotes(m.getEditorialNotes());
        target.setMetadata(mr);
    }

    private QuestionMetadata toDomain(QuestionMetadataRecord r) {
        return r == null ? null : new QuestionMetadata(r.getLastReviewDate(), r.getCognitiveLevel(),
                r.getEstimatedTimeMinutes(), r.getEditorialNotes());
    }

    // ---------------------------------------------------------------- quizzes

    /**
     * Converts a quiz record to the domain.
     *
     * @param r record
     * @return domain quiz, or {@code null}
     */
    public Quiz toDomain(QuizRecord r) {
        if (r == null) {
            return null;
        }
        List<Question> questions = r.getQuestions().stream().map(this::toDomain).toList();
        return new Quiz(r.getId(), r.getTitle(), questions);
    }
}
