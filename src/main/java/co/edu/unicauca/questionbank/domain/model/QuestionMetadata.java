package co.edu.unicauca.questionbank.domain.model;

import co.edu.unicauca.questionbank.domain.exception.BusinessRuleException;

import java.time.LocalDate;

/**
 * Complementary information of a question. Each question has at most one metadata record.
 * Immutable value object.
 */
public final class QuestionMetadata {

    private static final int MAX_NOTES_LENGTH = 1000;

    private final LocalDate lastReviewDate;
    private final CognitiveLevel cognitiveLevel;
    private final int estimatedTimeMinutes;
    private final String editorialNotes;

    /**
     * Creates the metadata.
     *
     * @param lastReviewDate       date of the last review, may be {@code null}
     * @param cognitiveLevel       cognitive level, required
     * @param estimatedTimeMinutes estimated time to answer, greater than zero
     * @param editorialNotes       editorial observations, may be {@code null}
     */
    public QuestionMetadata(LocalDate lastReviewDate, CognitiveLevel cognitiveLevel,
                            int estimatedTimeMinutes, String editorialNotes) {
        if (cognitiveLevel == null) {
            throw new BusinessRuleException("The cognitive level is required.");
        }
        if (estimatedTimeMinutes <= 0) {
            throw new BusinessRuleException("The estimated answer time must be greater than zero.");
        }
        if (editorialNotes != null && editorialNotes.length() > MAX_NOTES_LENGTH) {
            throw new BusinessRuleException("Editorial notes cannot exceed " + MAX_NOTES_LENGTH + " characters.");
        }
        if (lastReviewDate != null && lastReviewDate.isAfter(LocalDate.now())) {
            throw new BusinessRuleException("The last review date cannot be in the future.");
        }
        this.lastReviewDate = lastReviewDate;
        this.cognitiveLevel = cognitiveLevel;
        this.estimatedTimeMinutes = estimatedTimeMinutes;
        this.editorialNotes = editorialNotes;
    }

    public LocalDate getLastReviewDate() {
        return lastReviewDate;
    }

    public CognitiveLevel getCognitiveLevel() {
        return cognitiveLevel;
    }

    public int getEstimatedTimeMinutes() {
        return estimatedTimeMinutes;
    }

    public String getEditorialNotes() {
        return editorialNotes;
    }
}
