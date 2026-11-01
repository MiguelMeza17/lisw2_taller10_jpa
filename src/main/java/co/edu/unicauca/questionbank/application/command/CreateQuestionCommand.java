package co.edu.unicauca.questionbank.application.command;

import co.edu.unicauca.questionbank.domain.model.CognitiveLevel;

import java.util.List;

/**
 * Data needed to create a question.
 *
 * @param statement     text of the question
 * @param authorId      identifier of the author (must have the AUTHOR role)
 * @param type          question type
 * @param topicId       topic used to classify the question, optional
 * @param competencyIds competencies evaluated by the question, optional (required together with the topic)
 * @param options       answer options, only for {@link QuestionType#MULTIPLE_CHOICE}
 * @param correctAnswer expected answer, only for {@link QuestionType#TRUE_FALSE}
 * @param metadata      complementary metadata, optional
 */
public record CreateQuestionCommand(String statement,
                                    Long authorId,
                                    QuestionType type,
                                    Long topicId,
                                    List<Long> competencyIds,
                                    List<Option> options,
                                    Boolean correctAnswer,
                                    Metadata metadata) {

    /**
     * Answer option of a multiple choice question.
     *
     * @param text    text of the option
     * @param correct whether it is the right answer
     */
    public record Option(String text, boolean correct) {
    }

    /**
     * Complementary metadata of the question.
     *
     * @param cognitiveLevel       cognitive level
     * @param estimatedTimeMinutes estimated answer time
     * @param editorialNotes       editorial notes, optional
     */
    public record Metadata(CognitiveLevel cognitiveLevel, int estimatedTimeMinutes, String editorialNotes) {
    }
}
