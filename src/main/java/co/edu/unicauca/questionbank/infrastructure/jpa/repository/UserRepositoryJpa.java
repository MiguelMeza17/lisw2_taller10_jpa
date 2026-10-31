package co.edu.unicauca.questionbank.infrastructure.jpa.repository;

import co.edu.unicauca.questionbank.domain.model.User;
import co.edu.unicauca.questionbank.domain.repository.UserRepository;
import co.edu.unicauca.questionbank.infrastructure.jpa.mapper.DomainMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * JPA implementation of {@link UserRepository}.
 */
@Repository
public class UserRepositoryJpa implements UserRepository {

    private final SpringUserDataRepository users;
    private final DomainMapper mapper;

    /**
     * Creates the repository.
     *
     * @param users  Spring Data repository of users
     * @param mapper domain/record mapper
     */
    public UserRepositoryJpa(SpringUserDataRepository users, DomainMapper mapper) {
        this.users = users;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public User save(User user) {
        return mapper.toDomain(users.save(mapper.toRecord(user)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        return users.findById(id).map(mapper::toDomain);
    }
}
