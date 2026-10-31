package co.edu.unicauca.questionbank.infrastructure.jpa.repository;

import co.edu.unicauca.questionbank.domain.model.Topic;
import co.edu.unicauca.questionbank.domain.repository.TopicRepository;
import co.edu.unicauca.questionbank.infrastructure.jpa.mapper.DomainMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * JPA implementation of {@link TopicRepository}.
 */
@Repository
public class TopicRepositoryJpa implements TopicRepository {

    private final SpringTopicDataRepository topics;
    private final DomainMapper mapper;

    /**
     * Creates the repository.
     *
     * @param topics Spring Data repository of topics
     * @param mapper domain/record mapper
     */
    public TopicRepositoryJpa(SpringTopicDataRepository topics, DomainMapper mapper) {
        this.topics = topics;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public Topic save(Topic topic) {
        return mapper.toDomain(topics.save(mapper.toRecord(topic)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Topic> findById(Long id) {
        return topics.findById(id).map(mapper::toDomain);
    }
}
