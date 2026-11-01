package co.edu.unicauca.questionbank.api;

import co.edu.unicauca.questionbank.api.dto.Requests.QuizRequest;
import co.edu.unicauca.questionbank.api.dto.Responses.QuizResponse;
import co.edu.unicauca.questionbank.application.QuizService;
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
 * Endpoints of the quizzes.
 */
@RestController
@RequestMapping("/api/quizzes")
public class QuizController {

    private final QuizService quizzes;

    /**
     * Creates the controller.
     *
     * @param quizzes quiz use cases
     */
    public QuizController(QuizService quizzes) {
        this.quizzes = quizzes;
    }

    /**
     * Creates a quiz with published questions.
     *
     * @param body title and questions
     * @return the created quiz
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public QuizResponse create(@Valid @RequestBody QuizRequest body) {
        return QuizResponse.from(quizzes.create(body.title(), body.questionIds()));
    }

    /**
     * Finds a quiz.
     *
     * @param id identifier
     * @return the quiz
     */
    @GetMapping("/{id}")
    public QuizResponse findById(@PathVariable Long id) {
        return QuizResponse.from(quizzes.findById(id));
    }
}
