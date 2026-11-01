package co.edu.unicauca.questionbank.api.dto;

import co.edu.unicauca.questionbank.application.command.CreateQuestionCommand;
import co.edu.unicauca.questionbank.application.command.QuestionType;
import co.edu.unicauca.questionbank.domain.model.CognitiveLevel;
import co.edu.unicauca.questionbank.domain.model.Role;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

/**
 * Request bodies accepted by the REST API. Bean Validation only checks the shape of the request;
 * business rules are still enforced by the domain.
 */
public final class Requests {

    private Requests() {
    }

    /**
     * Body to register a user.
     *
     * @param fullName full name
     * @param email    email
     * @param role     role
     */
    public record UserRequest(@NotBlank String fullName, @NotBlank String email, @NotNull Role role) {
    }

    /**
     * Body to create a topic.
     *
     * @param name topic name
     */
    public record TopicRequest(@NotBlank String name) {
    }

    /**
     * Body to create a competency.
     *
     * @param code unique code
     * @param name name
     */
    public record CompetencyRequest(@NotBlank String code, @NotBlank String name) {
    }

    /**
     * Body to create a question.
     *
     * @param statement     text of the question
     * @param authorId      author
     * @param type          question type
     * @param topicId       topic, optional
     * @param competencyIds competencies, optional
     * @param options       options of a multiple choice question
     * @param correctAnswer answer of a true/false question
     * @param metadata      metadata, optional
     */
    public record QuestionRequest(@NotBlank String statement,
                                  @NotNull Long authorId,
                                  @NotNull QuestionType type,
                                  Long topicId,
                                  List<Long> competencyIds,
                                  List<@Valid OptionRequest> options,
                                  Boolean correctAnswer,
                                  @Valid MetadataRequest metadata) {

        /**
         * Converts the body into the command of the use case.
         *
         * @return command
         */
        public CreateQuestionCommand toCommand() {
            List<CreateQuestionCommand.Option> commandOptions = options == null ? null
                    : options.stream().map(o -> new CreateQuestionCommand.Option(o.text(), o.correct())).toList();
            CreateQuestionCommand.Metadata commandMetadata = metadata == null ? null
                    : new CreateQuestionCommand.Metadata(metadata.cognitiveLevel(), metadata.estimatedTimeMinutes(),
                    metadata.editorialNotes());
            return new CreateQuestionCommand(statement, authorId, type, topicId, competencyIds, commandOptions,
                    correctAnswer, commandMetadata);
        }
    }

    /**
     * Answer option inside {@link QuestionRequest}.
     *
     * @param text    text
     * @param correct whether it is the right answer
     */
    public record OptionRequest(@NotBlank String text, boolean correct) {
    }

    /**
     * Metadata inside {@link QuestionRequest}.
     *
     * @param cognitiveLevel       cognitive level
     * @param estimatedTimeMinutes estimated answer time
     * @param editorialNotes       notes, optional
     */
    public record MetadataRequest(@NotNull CognitiveLevel cognitiveLevel, @Positive int estimatedTimeMinutes,
                                  String editorialNotes) {
    }

    /**
     * Body to classify a question.
     *
     * @param topicId       topic
     * @param competencyIds competencies
     */
    public record ClassificationRequest(@NotNull Long topicId, @NotEmpty List<@NotNull Long> competencyIds) {
    }

    /**
     * Body to create a quiz.
     *
     * @param title       title
     * @param questionIds questions, in order
     */
    public record QuizRequest(@NotBlank String title, @NotEmpty List<@NotNull Long> questionIds) {
    }
}
