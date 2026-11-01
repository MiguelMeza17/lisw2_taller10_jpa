package co.edu.unicauca.questionbank.infrastructure.jpa.repository;

import co.edu.unicauca.questionbank.domain.model.QuestionStatus;
import co.edu.unicauca.questionbank.infrastructure.jpa.record.QuestionRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Spring Data interface for {@link QuestionRecord}. Internal to the infrastructure: only
 * {@link QuestionRepositoryJpa} uses it.
 */
public interface SpringQuestionDataRepository extends JpaRepository<QuestionRecord, Long> {

    /**
     * Questions of an author.
     *
     * @param authorId author identifier
     * @return records ordered by id
     */
    List<QuestionRecord> findByAuthorIdOrderByIdAsc(Long authorId);

    /**
     * Questions in a given state.
     *
     * @param status state
     * @return records ordered by id
     */
    List<QuestionRecord> findByStatusOrderByIdAsc(QuestionStatus status);

    /**
     * Questions in a given state that evaluate a competency.
     *
     * @param competencyId competency identifier
     * @param status       state
     * @return records ordered by id
     */
    List<QuestionRecord> findByCompetenciesIdAndStatusOrderByIdAsc(Long competencyId, QuestionStatus status);
}
