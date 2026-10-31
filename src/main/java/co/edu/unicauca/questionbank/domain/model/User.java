package co.edu.unicauca.questionbank.domain.model;

import co.edu.unicauca.questionbank.domain.exception.BusinessRuleException;

/**
 * A person registered in the question bank.
 */
public class User {

    private final Long id;
    private final String fullName;
    private final String email;
    private final Role role;

    /**
     * Creates a user that has not been persisted yet.
     *
     * @param fullName full name of the user
     * @param email    unique email address
     * @param role     role in the system
     */
    public User(String fullName, String email, Role role) {
        this(null, fullName, email, role);
    }

    /**
     * Rebuilds a user with a known identifier.
     *
     * @param id       identifier, may be {@code null} for new users
     * @param fullName full name of the user
     * @param email    unique email address
     * @param role     role in the system
     */
    public User(Long id, String fullName, String email, Role role) {
        if (fullName == null || fullName.isBlank()) {
            throw new BusinessRuleException("The user name is required.");
        }
        if (email == null || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new BusinessRuleException("The user email is not valid.");
        }
        if (role == null) {
            throw new BusinessRuleException("The user role is required.");
        }
        this.id = id;
        this.fullName = fullName.trim();
        this.email = email.trim().toLowerCase();
        this.role = role;
    }

    /**
     * Indicates whether this user is allowed to author questions.
     *
     * @return {@code true} if the user has the author role
     */
    public boolean canAuthorQuestions() {
        return role == Role.AUTHOR;
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public Role getRole() {
        return role;
    }
}
