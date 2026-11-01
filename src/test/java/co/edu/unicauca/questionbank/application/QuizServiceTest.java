package co.edu.unicauca.questionbank.application;

import co.edu.unicauca.questionbank.application.command.CreateQuestionCommand;
import co.edu.unicauca.questionbank.application.command.QuestionType;
import co.edu.unicauca.questionbank.application.exception.ResourceNotFoundException;
import co.edu.unicauca.questionbank.domain.exception.BusinessRuleException;
import co.edu.unicauca.questionbank.domain.model.Competency;
import co.edu.unicauca.questionbank.domain.model.Question;
import co.edu.unicauca.questionbank.domain.model.Quiz;
import co.edu.unicauca.questionbank.domain.model.Role;
import co.edu.unicauca.questionbank.domain.model.Topic;
import co.edu.unicauca.questionbank.domain.model.User;
import co.edu.unicauca.questionbank.support.InMemoryRepositories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests of {@link QuizService} with in-memory repositories.
 */
class QuizServiceTest {

    private QuizService service;
    private QuestionService questions;
    private InMemoryRepositories.Quizzes quizzes;
    private User author;
    private Topic topic;
    private Competency design;

    @BeforeEach
    void setUp() {
        var users = new InMemoryRepositories.Users();
        var topics = new InMemoryRepositories.Topics();
        var competencies = new InMemoryRepositories.Competencies();
        var questionRepository = new InMemoryRepositories.Questions();
        quizzes = new InMemoryRepositories.Quizzes();
        questions = new QuestionService(questionRepository, users, topics, competencies);
        service = new QuizService(quizzes, questionRepository);

        CatalogService catalog = new CatalogService(users, topics, competencies);
        author = catalog.registerUser("Ana Author", "ana@unicauca.edu.co", Role.AUTHOR);
        topic = catalog.createTopic("Databases");
        design = catalog.createCompetency("DESIGN", "Solution design");
    }

    private Long draft(String statement) {
        return questions.create(new CreateQuestionCommand(statement, author.getId(), QuestionType.TRUE_FALSE,
                topic.getId(), List.of(design.getId()), null, true, null)).getId();
    }

    private Long published(String statement) {
        Long id = draft(statement);
        questions.submitForReview(id);
        questions.startReview(id);
        questions.approve(id);
        questions.publish(id);
        return id;
    }

    @Test
    void createsAQuizWithPublishedQuestionsInOrder() {
        Long first = published("SQL is declarative");
        Long second = published("H2 can run in memory");

        Quiz quiz = service.create("Mock exam 1", List.of(second, first));

        assertNotNull(quiz.getId());
        assertEquals(List.of(second, first), quiz.getQuestions().stream().map(Question::getId).toList());
        assertEquals("Mock exam 1", service.findById(quiz.getId()).getTitle());
    }

    @Test
    void rejectsQuestionsThatAreNotPublishedAndStoresNothing() {
        Long ok = published("SQL is declarative");
        Long notPublished = draft("Draft question");
        Long approvedOnly = draft("Approved question");
        questions.submitForReview(approvedOnly);
        questions.startReview(approvedOnly);
        questions.approve(approvedOnly);

        assertThrows(BusinessRuleException.class, () -> service.create("Quiz", List.of(ok, notPublished)));
        assertThrows(BusinessRuleException.class, () -> service.create("Quiz", List.of(approvedOnly)));
        assertEquals(0, quizzes.count());
    }

    @Test
    void rejectsArchivedRepeatedOrUnknownQuestions() {
        Long archived = published("Old question");
        questions.archive(archived);
        Long ok = published("SQL is declarative");

        assertThrows(BusinessRuleException.class, () -> service.create("Quiz", List.of(archived)));
        assertThrows(BusinessRuleException.class, () -> service.create("Quiz", List.of(ok, ok)));
        assertThrows(ResourceNotFoundException.class, () -> service.create("Quiz", List.of(404L)));
        assertThrows(BusinessRuleException.class, () -> service.create("Quiz", List.of()));
        assertThrows(BusinessRuleException.class, () -> service.create(" ", List.of(ok)));
    }

    @Test
    void failsForUnknownQuiz() {
        assertThrows(ResourceNotFoundException.class, () -> service.findById(7L));
    }
}
