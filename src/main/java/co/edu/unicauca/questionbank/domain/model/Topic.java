package co.edu.unicauca.questionbank.domain.model;

import co.edu.unicauca.questionbank.domain.exception.BusinessRuleException;

/**
 * Academic topic used to classify questions (for example "Software architecture").
 */
public class Topic {

    private final Long id;
    private final String name;

    /**
     * Creates a topic that has not been persisted yet.
     *
     * @param name topic name
     */
    public Topic(String name) {
        this(null, name);
    }

    /**
     * Rebuilds a topic with a known identifier.
     *
     * @param id   identifier, may be {@code null}
     * @param name topic name
     */
    public Topic(Long id, String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessRuleException("The topic name is required.");
        }
        this.id = id;
        this.name = name.trim();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
