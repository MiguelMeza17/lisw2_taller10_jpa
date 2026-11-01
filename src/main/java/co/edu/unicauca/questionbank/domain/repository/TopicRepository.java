package co.edu.unicauca.questionbank.domain.repository;

import co.edu.unicauca.questionbank.domain.model.Topic;

import java.util.Optional;

/**
 * Storage contract for topics.
 */
public interface TopicRepository {

    /**
     * Stores a topic.
     *
     * @param topic topic to store
     * @return stored topic with its identifier
     */
    Topic save(Topic topic);

    /**
     * Finds a topic by its identifier.
     *
     * @param id identifier
     * @return the topic, if it exists
     */
    Optional<Topic> findById(Long id);
}
