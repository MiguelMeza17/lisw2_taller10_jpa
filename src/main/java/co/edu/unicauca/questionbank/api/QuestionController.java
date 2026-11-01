package co.edu.unicauca.questionbank.api;

import co.edu.unicauca.questionbank.api.dto.Requests.ClassificationRequest;
import co.edu.unicauca.questionbank.api.dto.Requests.QuestionRequest;
import co.edu.unicauca.questionbank.api.dto.Responses.QuestionResponse;
import co.edu.unicauca.questionbank.application.QuestionService;
import co.edu.unicauca.questionbank.application.command.QuestionCriteria;
import co.edu.unicauca.questionbank.domain.model.QuestionStatus;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints of the questions. The controller only translates HTTP to use case calls and the result
 * back to JSON; every rule lives in the application and domain layers.
 */
@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    private final QuestionService questions;

    /**
     * Creates the controller.
     *
     * @param questions question use cases
     */
    public QuestionController(QuestionService questions) {
        this.questions = questions;
    }

    /**
     * Creates a draft question.
     *
     * @param body question data
     * @return the created question
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public QuestionResponse create(@Valid @RequestBody QuestionRequest body) {
        return QuestionResponse.from(questions.create(body.toCommand()));
    }

    /**
     * Finds a question.
     *
     * @param id identifier
     * @return the question
     */
    @GetMapping("/{id}")
    public QuestionResponse findById(@PathVariable Long id) {
        return QuestionResponse.from(questions.findById(id));
    }

    /**
     * Searches questions. Every parameter is optional.
     *
     * @param authorId     author
     * @param status       state
     * @param topicId      topic
     * @param competencyId competency
     * @return matching questions
     */
    @GetMapping
    public List<QuestionResponse> search(@RequestParam(required = false) Long authorId,
                                         @RequestParam(required = false) QuestionStatus status,
                                         @RequestParam(required = false) Long topicId,
                                         @RequestParam(required = false) Long competencyId) {
        return questions.search(new QuestionCriteria(authorId, status, topicId, competencyId)).stream()
                .map(QuestionResponse::from).toList();
    }

    /**
     * Classifies a draft question.
     *
     * @param id   question
     * @param body topic and competencies
     * @return the updated question
     */
    @PutMapping("/{id}/classification")
    public QuestionResponse classify(@PathVariable Long id, @Valid @RequestBody ClassificationRequest body) {
        return QuestionResponse.from(questions.classify(id, body.topicId(), body.competencyIds()));
    }

    /**
     * Sends a draft to review (DRAFT → PENDING_REVIEW).
     *
     * @param id question
     * @return the updated question
     */
    @PostMapping("/{id}/submit")
    public QuestionResponse submitForReview(@PathVariable Long id) {
        return QuestionResponse.from(questions.submitForReview(id));
    }

    /**
     * Starts the review (PENDING_REVIEW → IN_REVIEW).
     *
     * @param id question
     * @return the updated question
     */
    @PostMapping("/{id}/start-review")
    public QuestionResponse startReview(@PathVariable Long id) {
        return QuestionResponse.from(questions.startReview(id));
    }

    /**
     * Approves the question (IN_REVIEW → APPROVED).
     *
     * @param id question
     * @return the updated question
     */
    @PostMapping("/{id}/approve")
    public QuestionResponse approve(@PathVariable Long id) {
        return QuestionResponse.from(questions.approve(id));
    }

    /**
     * Rejects the question and sends it back to draft.
     *
     * @param id question
     * @return the updated question
     */
    @PostMapping("/{id}/reject")
    public QuestionResponse reject(@PathVariable Long id) {
        return QuestionResponse.from(questions.reject(id));
    }

    /**
     * Publishes the question (APPROVED → PUBLISHED).
     *
     * @param id question
     * @return the updated question
     */
    @PostMapping("/{id}/publish")
    public QuestionResponse publish(@PathVariable Long id) {
        return QuestionResponse.from(questions.publish(id));
    }

    /**
     * Archives the question (PUBLISHED → ARCHIVED).
     *
     * @param id question
     * @return the updated question
     */
    @PostMapping("/{id}/archive")
    public QuestionResponse archive(@PathVariable Long id) {
        return QuestionResponse.from(questions.archive(id));
    }
}
