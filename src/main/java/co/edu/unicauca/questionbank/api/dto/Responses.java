package co.edu.unicauca.questionbank.api.dto;

import co.edu.unicauca.questionbank.domain.model.CognitiveLevel;
import co.edu.unicauca.questionbank.domain.model.Competency;
import co.edu.unicauca.questionbank.domain.model.MultipleChoiceQuestion;
import co.edu.unicauca.questionbank.domain.model.Question;
import co.edu.unicauca.questionbank.domain.model.QuestionMetadata;
import co.edu.unicauca.questionbank.domain.model.QuestionStatus;
import co.edu.unicauca.questionbank.domain.model.Quiz;
import co.edu.unicauca.questionbank.domain.model.Role;
import co.edu.unicauca.questionbank.domain.model.Topic;
import co.edu.unicauca.questionbank.domain.model.TrueFalseQuestion;
import co.edu.unicauca.questionbank.domain.model.User;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;
import java.util.List;

/**
 * Response bodies of the REST API. Domain objects are never serialized directly, so the API contract
 * does not change when the domain does.
 */
public final class Responses {

    private Responses() {
    }

    /**
     * A user.
     *
     * @param id       identifier
     * @param fullName name
     * @param email    email
     * @param role     role
     */
    public record UserResponse(Long id, String fullName, String email, Role role) {
        /**
         * Builds the response from the domain.
         *
         * @param u user
         * @return response
         */
        public static UserResponse from(User u) {
            return new UserResponse(u.getId(), u.getFullName(), u.getEmail(), u.getRole());
        }
    }

    /**
     * A topic.
     *
     * @param id   identifier
     * @param name name
     */
    public record TopicResponse(Long id, String name) {
        /**
         * Builds the response from the domain.
         *
         * @param t topic, may be {@code null}
         * @return response, or {@code null}
         */
        public static TopicResponse from(Topic t) {
            return t == null ? null : new TopicResponse(t.getId(), t.getName());
        }
    }

    /**
     * A competency.
     *
     * @param id   identifier
     * @param code code
     * @param name name
     */
    public record CompetencyResponse(Long id, String code, String name) {
        /**
         * Builds the response from the domain.
         *
         * @param c competency
         * @return response
         */
        public static CompetencyResponse from(Competency c) {
            return new CompetencyResponse(c.getId(), c.getCode(), c.getName());
        }
    }

    /**
     * An answer option.
     *
     * @param text    text
     * @param correct whether it is the right answer
     */
    public record OptionResponse(String text, boolean correct) {
    }

    /**
     * Metadata of a question.
     *
     * @param lastReviewDate       last review date
     * @param cognitiveLevel       cognitive level
     * @param estimatedTimeMinutes estimated time
     * @param editorialNotes       notes
     */
    public record MetadataResponse(LocalDate lastReviewDate, CognitiveLevel cognitiveLevel,
                                   int estimatedTimeMinutes, String editorialNotes) {
        static MetadataResponse from(QuestionMetadata m) {
            return m == null ? null : new MetadataResponse(m.getLastReviewDate(), m.getCognitiveLevel(),
                    m.getEstimatedTimeMinutes(), m.getEditorialNotes());
        }
    }

    /**
     * A question of any type. {@code options} only appears for multiple choice questions and
     * {@code correctAnswer} only for true/false ones.
     *
     * @param id            identifier
     * @param type          {@code MULTIPLE_CHOICE} or {@code TRUE_FALSE}
     * @param statement     statement
     * @param status        life cycle state
     * @param author        author
     * @param topic         topic
     * @param competencies  competencies
     * @param options       options (multiple choice)
     * @param correctAnswer expected answer (true/false)
     * @param metadata      metadata
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record QuestionResponse(Long id, String type, String statement, QuestionStatus status,
                                   UserResponse author, TopicResponse topic, List<CompetencyResponse> competencies,
                                   List<OptionResponse> options, Boolean correctAnswer, MetadataResponse metadata) {
        /**
         * Builds the response from the domain.
         *
         * @param q question
         * @return response
         */
        public static QuestionResponse from(Question q) {
            String type = null;
            List<OptionResponse> options = null;
            Boolean correctAnswer = null;
            if (q instanceof MultipleChoiceQuestion mc) {
                type = "MULTIPLE_CHOICE";
                options = mc.getOptions().stream().map(o -> new OptionResponse(o.getText(), o.isCorrect())).toList();
            } else if (q instanceof TrueFalseQuestion tf) {
                type = "TRUE_FALSE";
                correctAnswer = tf.getCorrectAnswer();
            }
            return new QuestionResponse(q.getId(), type, q.getStatement(), q.getStatus(),
                    UserResponse.from(q.getAuthor()), TopicResponse.from(q.getTopic()),
                    q.getCompetencies().stream().map(CompetencyResponse::from).toList(),
                    options, correctAnswer, MetadataResponse.from(q.getMetadata()));
        }
    }

    /**
     * A quiz with its questions in order.
     *
     * @param id        identifier
     * @param title     title
     * @param questions questions
     */
    public record QuizResponse(Long id, String title, List<QuestionResponse> questions) {
        /**
         * Builds the response from the domain.
         *
         * @param quiz quiz
         * @return response
         */
        public static QuizResponse from(Quiz quiz) {
            return new QuizResponse(quiz.getId(), quiz.getTitle(),
                    quiz.getQuestions().stream().map(QuestionResponse::from).toList());
        }
    }
}
