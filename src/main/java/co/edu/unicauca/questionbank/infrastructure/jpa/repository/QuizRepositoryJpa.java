package co.edu.unicauca.questionbank.infrastructure.jpa.repository;

import co.edu.unicauca.questionbank.domain.exception.BusinessRuleException;
import co.edu.unicauca.questionbank.domain.model.Question;
import co.edu.unicauca.questionbank.domain.model.Quiz;
import co.edu.unicauca.questionbank.domain.repository.QuizRepository;
import co.edu.unicauca.questionbank.infrastructure.jpa.mapper.DomainMapper;
import co.edu.unicauca.questionbank.infrastructure.jpa.record.QuestionRecord;
import co.edu.unicauca.questionbank.infrastructure.jpa.record.QuizRecord;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * JPA implementation of {@link QuizRepository}.
 */
@Repository
public class QuizRepositoryJpa implements QuizRepository {

    private final SpringQuizDataRepository quizzes;
    private final SpringQuestionDataRepository questions;
    private final DomainMapper mapper;

    /**
     * Creates the repository.
     *
     * @param quizzes   Spring Data repository of quizzes
     * @param questions Spring Data repository of questions
     * @param mapper    domain/record mapper
     */
    public QuizRepositoryJpa(SpringQuizDataRepository quizzes, SpringQuestionDataRepository questions,
                             DomainMapper mapper) {
        this.quizzes = quizzes;
        this.questions = questions;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public Quiz save(Quiz quiz) {
        QuizRecord record = quiz.getId() == null
                ? new QuizRecord()
                : quizzes.findById(quiz.getId()).orElseThrow(() ->
                        new BusinessRuleException("Quiz " + quiz.getId() + " does not exist."));
        record.setTitle(quiz.getTitle());
        record.getQuestions().clear();
        for (Question question : quiz.getQuestions()) {
            QuestionRecord qr = questions.findById(question.getId()).orElseThrow(() ->
                    new BusinessRuleException("Question " + question.getId() + " does not exist."));
            record.getQuestions().add(qr);
        }
        return mapper.toDomain(quizzes.save(record));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Quiz> findById(Long id) {
        return quizzes.findById(id).map(mapper::toDomain);
    }
}
