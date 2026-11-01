package co.edu.unicauca.questionbank.application.command;

import co.edu.unicauca.questionbank.domain.model.QuestionStatus;

/**
 * Optional filters to search questions. A {@code null} field means "any".
 *
 * @param authorId     identifier of the author
 * @param status       life cycle state
 * @param topicId      identifier of the topic
 * @param competencyId identifier of an evaluated competency
 */
public record QuestionCriteria(Long authorId, QuestionStatus status, Long topicId, Long competencyId) {

    /**
     * Criteria without filters.
     *
     * @return criteria that matches every question
     */
    public static QuestionCriteria any() {
        return new QuestionCriteria(null, null, null, null);
    }
}
