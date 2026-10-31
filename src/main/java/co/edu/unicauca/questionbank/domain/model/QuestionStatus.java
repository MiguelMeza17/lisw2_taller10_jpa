package co.edu.unicauca.questionbank.domain.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Life cycle of a question:
 * DRAFT, PENDING_REVIEW, IN_REVIEW, APPROVED, PUBLISHED and ARCHIVED.
 * A question that is waiting for review or under review can be rejected and goes back to DRAFT.
 */
public enum QuestionStatus {
    DRAFT,
    PENDING_REVIEW,
    IN_REVIEW,
    APPROVED,
    PUBLISHED,
    ARCHIVED;

    /**
     * Returns the states that can be reached from this one.
     *
     * @return allowed target states
     */
    public Set<QuestionStatus> allowedTransitions() {
        return switch (this) {
            case DRAFT -> EnumSet.of(PENDING_REVIEW);
            case PENDING_REVIEW -> EnumSet.of(IN_REVIEW, DRAFT);
            case IN_REVIEW -> EnumSet.of(APPROVED, DRAFT);
            case APPROVED -> EnumSet.of(PUBLISHED);
            case PUBLISHED -> EnumSet.of(ARCHIVED);
            case ARCHIVED -> EnumSet.noneOf(QuestionStatus.class);
        };
    }

    /**
     * Checks whether a transition to the given state is allowed.
     *
     * @param target desired state
     * @return {@code true} if the transition is valid
     */
    public boolean canTransitionTo(QuestionStatus target) {
        return allowedTransitions().contains(target);
    }
}
