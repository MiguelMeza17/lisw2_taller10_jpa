package co.edu.unicauca.questionbank.domain.repository;

import co.edu.unicauca.questionbank.domain.model.Question;
import co.edu.unicauca.questionbank.domain.model.QuestionStatus;

import java.util.List;
import java.util.Optional;

/**
 * Storage contract for questions. It does not depend on any persistence technology.
 */
public interface QuestionRepository {

    /**
     * Stores a new question or updates an existing one.
     *
     * @param question question to store
     * @return the stored question, with its identifier
     */
    Question save(Question question);

    /**
     * Finds a question by its identifier.
     *
     * @param id identifier
     * @return the question, if it exists
     */
    Optional<Question> findById(Long id);

    /**
     * Lists every question.
     *
     * @return all questions
     */
    List<Question> findAll();

    /**
     * Lists the questions written by an author.
     *
     * @param authorId identifier of the author
     * @return questions of the author
     */
    List<Question> findByAuthor(Long authorId);

    /**
     * Lists the questions in a given state.
     *
     * @param status life cycle state
     * @return questions in that state
     */
    List<Question> findByStatus(QuestionStatus status);

    /**
     * Lists the published questions that evaluate a competency.
     *
     * @param competencyId identifier of the competency
     * @return published questions of the competency
     */
    List<Question> findPublishedByCompetency(Long competencyId);
}
