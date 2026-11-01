package co.edu.unicauca.questionbank.domain.exception;

/**
 * Raised when an operation would break a business rule of the question bank.
 */
public class BusinessRuleException extends DomainException {

    /**
     * Creates the exception with the violated rule.
     *
     * @param message description of the violated rule
     */
    public BusinessRuleException(String message) {
        super(message);
    }
}
