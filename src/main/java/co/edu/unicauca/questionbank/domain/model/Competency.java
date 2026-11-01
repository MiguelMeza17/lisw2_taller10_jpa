package co.edu.unicauca.questionbank.domain.model;

import co.edu.unicauca.questionbank.domain.exception.BusinessRuleException;

import java.util.Objects;

/**
 * Competency evaluated by the Saber Pro exam (for example "Solution design").
 * Two competencies are equal when they share the same code.
 */
public class Competency {

    private final Long id;
    private final String code;
    private final String name;

    /**
     * Creates a competency that has not been persisted yet.
     *
     * @param code unique business code
     * @param name descriptive name
     */
    public Competency(String code, String name) {
        this(null, code, name);
    }

    /**
     * Rebuilds a competency with a known identifier.
     *
     * @param id   identifier, may be {@code null}
     * @param code unique business code
     * @param name descriptive name
     */
    public Competency(Long id, String code, String name) {
        if (code == null || code.isBlank()) {
            throw new BusinessRuleException("The competency code is required.");
        }
        if (name == null || name.isBlank()) {
            throw new BusinessRuleException("The competency name is required.");
        }
        this.id = id;
        this.code = code.trim().toUpperCase();
        this.name = name.trim();
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Competency other)) {
            return false;
        }
        return code.equals(other.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code);
    }
}
