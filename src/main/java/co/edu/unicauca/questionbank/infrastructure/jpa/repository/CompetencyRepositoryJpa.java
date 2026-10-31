package co.edu.unicauca.questionbank.infrastructure.jpa.repository;

import co.edu.unicauca.questionbank.domain.model.Competency;
import co.edu.unicauca.questionbank.domain.repository.CompetencyRepository;
import co.edu.unicauca.questionbank.infrastructure.jpa.mapper.DomainMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * JPA implementation of {@link CompetencyRepository}.
 */
@Repository
public class CompetencyRepositoryJpa implements CompetencyRepository {

    private final SpringCompetencyDataRepository competencies;
    private final DomainMapper mapper;

    /**
     * Creates the repository.
     *
     * @param competencies Spring Data repository of competencies
     * @param mapper       domain/record mapper
     */
    public CompetencyRepositoryJpa(SpringCompetencyDataRepository competencies, DomainMapper mapper) {
        this.competencies = competencies;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public Competency save(Competency competency) {
        return mapper.toDomain(competencies.save(mapper.toRecord(competency)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Competency> findById(Long id) {
        return competencies.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Competency> findAllById(Collection<Long> ids) {
        return competencies.findAllById(ids).stream().map(mapper::toDomain).toList();
    }
}
