package co.edu.unicauca.questionbank.domain.exception;

/**
 * Base class for every exception raised by the domain model.
 */
public class DomainException extends RuntimeException {

    /**
     * Creates the exception with a descriptive message.
     *
     * @param message explanation of the problem
     */
    public DomainException(String message) {
        super(message);
    }
}
