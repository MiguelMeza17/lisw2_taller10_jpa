package co.edu.unicauca.questionbank.domain.repository;

import co.edu.unicauca.questionbank.domain.model.User;

import java.util.Optional;

/**
 * Storage contract for users.
 */
public interface UserRepository {

    /**
     * Stores a user.
     *
     * @param user user to store
     * @return stored user with its identifier
     */
    User save(User user);

    /**
     * Finds a user by its identifier.
     *
     * @param id identifier
     * @return the user, if it exists
     */
    Optional<User> findById(Long id);
}
