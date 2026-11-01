package co.edu.unicauca.questionbank.api;

import co.edu.unicauca.questionbank.api.dto.Requests.CompetencyRequest;
import co.edu.unicauca.questionbank.api.dto.Requests.TopicRequest;
import co.edu.unicauca.questionbank.api.dto.Requests.UserRequest;
import co.edu.unicauca.questionbank.api.dto.Responses.CompetencyResponse;
import co.edu.unicauca.questionbank.api.dto.Responses.TopicResponse;
import co.edu.unicauca.questionbank.api.dto.Responses.UserResponse;
import co.edu.unicauca.questionbank.application.CatalogService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints of the reference data: users, topics and competencies.
 */
@RestController
@RequestMapping("/api")
public class CatalogController {

    private final CatalogService catalog;

    /**
     * Creates the controller.
     *
     * @param catalog catalog use cases
     */
    public CatalogController(CatalogService catalog) {
        this.catalog = catalog;
    }

    /**
     * Registers a user.
     *
     * @param body user data
     * @return the created user
     */
    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse registerUser(@Valid @RequestBody UserRequest body) {
        return UserResponse.from(catalog.registerUser(body.fullName(), body.email(), body.role()));
    }

    /**
     * Finds a user.
     *
     * @param id identifier
     * @return the user
     */
    @GetMapping("/users/{id}")
    public UserResponse findUser(@PathVariable Long id) {
        return UserResponse.from(catalog.findUser(id));
    }

    /**
     * Creates a topic.
     *
     * @param body topic data
     * @return the created topic
     */
    @PostMapping("/topics")
    @ResponseStatus(HttpStatus.CREATED)
    public TopicResponse createTopic(@Valid @RequestBody TopicRequest body) {
        return TopicResponse.from(catalog.createTopic(body.name()));
    }

    /**
     * Creates a competency.
     *
     * @param body competency data
     * @return the created competency
     */
    @PostMapping("/competencies")
    @ResponseStatus(HttpStatus.CREATED)
    public CompetencyResponse createCompetency(@Valid @RequestBody CompetencyRequest body) {
        return CompetencyResponse.from(catalog.createCompetency(body.code(), body.name()));
    }
}
