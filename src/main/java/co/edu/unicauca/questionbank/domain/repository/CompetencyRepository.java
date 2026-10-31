package co.edu.unicauca.questionbank.domain.repository;

import co.edu.unicauca.questionbank.domain.model.Competency;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Storage contract for competencies.
 */
public interface CompetencyRepository {

    /**
     * Stores a competency.
     *
     * @param competency competency to store
     * @return stored competency with its identifier
     */
    Competency save(Competency competency);

    /**
     * Finds a competency by its identifier.
     *
     * @param id identifier
     * @return the competency, if it exists
     */
    Optional<Competency> findById(Long id);

    /**
     * Finds several competencies. Identifiers that do not exist are ignored.
     *
     * @param ids identifiers
     * @return competencies found
     */
    List<Competency> findAllById(Collection<Long> ids);
}
