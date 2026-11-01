package co.edu.unicauca.questionbank.domain.model;

import co.edu.unicauca.questionbank.domain.exception.BusinessRuleException;

import java.util.Objects;

/**
 * Answer option of a multiple choice question. Immutable value object.
 */
public final class AnswerOption {

    private final String text;
    private final boolean correct;

    /**
     * Creates an option.
     *
     * @param text    text shown to the student
     * @param correct whether this option is the right answer
     */
    public AnswerOption(String text, boolean correct) {
        if (text == null || text.isBlank()) {
            throw new BusinessRuleException("The answer option text is required.");
        }
        this.text = text.trim();
        this.correct = correct;
    }

    public String getText() {
        return text;
    }

    public boolean isCorrect() {
        return correct;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AnswerOption other)) {
            return false;
        }
        return correct == other.correct && text.equals(other.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text, correct);
    }
}
