package co.edu.unicauca.questionbank.application;

import co.edu.unicauca.questionbank.application.exception.ResourceNotFoundException;
import co.edu.unicauca.questionbank.domain.model.Competency;
import co.edu.unicauca.questionbank.domain.model.Role;
import co.edu.unicauca.questionbank.domain.model.Topic;
import co.edu.unicauca.questionbank.domain.model.User;
import co.edu.unicauca.questionbank.domain.repository.CompetencyRepository;
import co.edu.unicauca.questionbank.domain.repository.TopicRepository;
import co.edu.unicauca.questionbank.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use cases of the reference data needed by the questions: users, topics and competencies.
 */
@Service
@Transactional
public class CatalogService {

    private final UserRepository users;
    private final TopicRepository topics;
    private final CompetencyRepository competencies;

    /**
     * Creates the service.
     *
     * @param users        user repository
     * @param topics       topic repository
     * @param competencies competency repository
     */
    public CatalogService(UserRepository users, TopicRepository topics, CompetencyRepository competencies) {
        this.users = users;
        this.topics = topics;
        this.competencies = competencies;
    }

    /**
     * Registers a user.
     *
     * @param fullName full name
     * @param email    unique email
     * @param role     role in the system
     * @return the stored user
     */
    public User registerUser(String fullName, String email, Role role) {
        return users.save(new User(fullName, email, role));
    }

    /**
     * Finds a user.
     *
     * @param id identifier
     * @return the user
     */
    @Transactional(readOnly = true)
    public User findUser(Long id) {
        return users.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    /**
     * Creates a topic.
     *
     * @param name topic name
     * @return the stored topic
     */
    public Topic createTopic(String name) {
        return topics.save(new Topic(name));
    }

    /**
     * Creates a competency.
     *
     * @param code unique code
     * @param name descriptive name
     * @return the stored competency
     */
    public Competency createCompetency(String code, String name) {
        return competencies.save(new Competency(code, name));
    }
}
