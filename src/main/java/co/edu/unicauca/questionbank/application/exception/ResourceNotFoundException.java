package co.edu.unicauca.questionbank.application.exception;

/**
 * Raised by a use case when a referenced resource (question, user, topic...) does not exist.
 */
public class ResourceNotFoundException extends RuntimeException {

    /**
     * Creates the exception.
     *
     * @param resource name of the resource, for example "Question"
     * @param id       identifier that was not found
     */
    public ResourceNotFoundException(String resource, Object id) {
        super(resource + " " + id + " does not exist.");
    }
}
