package co.edu.unicauca.questionbank.domain.repository;

import co.edu.unicauca.questionbank.domain.model.Quiz;

import java.util.Optional;

/**
 * Storage contract for quizzes.
 */
public interface QuizRepository {

    /**
     * Stores a quiz with its questions.
     *
     * @param quiz quiz to store
     * @return stored quiz with its identifier
     */
    Quiz save(Quiz quiz);

    /**
     * Finds a quiz by its identifier.
     *
     * @param id identifier
     * @return the quiz, if it exists
     */
    Optional<Quiz> findById(Long id);
}
