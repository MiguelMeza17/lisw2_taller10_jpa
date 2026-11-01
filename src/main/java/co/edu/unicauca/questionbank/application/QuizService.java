package co.edu.unicauca.questionbank.application;

import co.edu.unicauca.questionbank.application.exception.ResourceNotFoundException;
import co.edu.unicauca.questionbank.domain.exception.BusinessRuleException;
import co.edu.unicauca.questionbank.domain.model.Question;
import co.edu.unicauca.questionbank.domain.model.Quiz;
import co.edu.unicauca.questionbank.domain.repository.QuestionRepository;
import co.edu.unicauca.questionbank.domain.repository.QuizRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Use cases of the quizzes.
 */
@Service
@Transactional
public class QuizService {

    private final QuizRepository quizzes;
    private final QuestionRepository questions;

    /**
     * Creates the service.
     *
     * @param quizzes   quiz repository
     * @param questions question repository
     */
    public QuizService(QuizRepository quizzes, QuestionRepository questions) {
        this.quizzes = quizzes;
        this.questions = questions;
    }

    /**
     * Creates a quiz with the given questions, in that order. The domain only accepts published
     * questions, so a draft, a question under review or an archived one makes the whole operation fail
     * and nothing is stored.
     *
     * @param title       title of the quiz
     * @param questionIds questions of the quiz, at least one
     * @return the stored quiz
     */
    public Quiz create(String title, List<Long> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            throw new BusinessRuleException("A quiz needs at least one question.");
        }
        Quiz quiz = new Quiz(title);
        for (Long id : questionIds) {
            if (id == null) {
                throw new BusinessRuleException("The question identifier is required.");
            }
            Question question = questions.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Question", id));
            quiz.addQuestion(question);
        }
        return quizzes.save(quiz);
    }

    /**
     * Finds a quiz by its identifier.
     *
     * @param id identifier
     * @return the quiz
     * @throws ResourceNotFoundException if it does not exist
     */
    @Transactional(readOnly = true)
    public Quiz findById(Long id) {
        if (id == null) {
            throw new BusinessRuleException("The quiz identifier is required.");
        }
        return quizzes.findById(id).orElseThrow(() -> new ResourceNotFoundException("Quiz", id));
    }
}
