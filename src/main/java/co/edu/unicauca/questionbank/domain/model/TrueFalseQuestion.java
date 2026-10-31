package co.edu.unicauca.questionbank.domain.model;

import java.util.Collection;

/**
 * Question whose answer is either true or false.
 */
public class TrueFalseQuestion extends Question {

    private boolean correctAnswer;

    /**
     * Creates a new draft true/false question.
     *
     * @param statement     text of the question
     * @param author        author of the question
     * @param correctAnswer expected answer
     */
    public TrueFalseQuestion(String statement, User author, boolean correctAnswer) {
        super(statement, author);
        this.correctAnswer = correctAnswer;
    }

    /**
     * Rebuilds a true/false question loaded from storage.
     *
     * @param id            identifier
     * @param statement     text of the question
     * @param author        author of the question
     * @param topic         topic, may be {@code null}
     * @param competencies  evaluated competencies
     * @param metadata      metadata, may be {@code null}
     * @param status        life cycle state
     * @param correctAnswer expected answer
     */
    public TrueFalseQuestion(Long id, String statement, User author, Topic topic,
                             Collection<Competency> competencies, QuestionMetadata metadata,
                             QuestionStatus status, boolean correctAnswer) {
        super(id, statement, author, topic, competencies, metadata, status);
        this.correctAnswer = correctAnswer;
    }

    /**
     * Changes the expected answer. Only allowed while the question is a draft.
     *
     * @param newCorrectAnswer expected answer
     */
    public void changeCorrectAnswer(boolean newCorrectAnswer) {
        requireDraft("change the correct answer");
        this.correctAnswer = newCorrectAnswer;
    }

    /**
     * A true/false question has no extra structure to validate: the statement and the expected
     * answer are always present.
     */
    @Override
    protected void validateStructure() {
        // Nothing else to validate for this type.
    }

    public boolean getCorrectAnswer() {
        return correctAnswer;
    }
}
