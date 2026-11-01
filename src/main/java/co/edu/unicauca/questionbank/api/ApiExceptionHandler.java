package co.edu.unicauca.questionbank.api;

import co.edu.unicauca.questionbank.application.exception.ResourceNotFoundException;
import co.edu.unicauca.questionbank.domain.exception.DomainException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Translates the exceptions of the inner layers into HTTP responses ({@link ProblemDetail}, RFC 9457).
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    /**
     * A referenced resource does not exist.
     *
     * @param e exception
     * @return 404
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail notFound(ResourceNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "Resource not found", e.getMessage());
    }

    /**
     * The request breaks a business rule of the domain.
     *
     * @param e exception
     * @return 422
     */
    @ExceptionHandler(DomainException.class)
    public ProblemDetail businessRule(DomainException e) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "Business rule violated", e.getMessage());
    }

    /**
     * The database rejected the data, for example a duplicated email or code.
     *
     * @param e exception
     * @return 409
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail conflict(DataIntegrityViolationException e) {
        return problem(HttpStatus.CONFLICT, "Data integrity violation",
                "The data conflicts with existing records (duplicated value or invalid reference).");
    }

    /**
     * The request body does not have the expected shape.
     *
     * @param e exception
     * @return 400
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail invalidRequest(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .sorted()
                .collect(Collectors.joining("; "));
        return problem(HttpStatus.BAD_REQUEST, "Invalid request", detail);
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
