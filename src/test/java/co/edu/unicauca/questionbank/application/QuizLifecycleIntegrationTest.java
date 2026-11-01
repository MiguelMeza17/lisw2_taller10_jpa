package co.edu.unicauca.questionbank.application;

import co.edu.unicauca.questionbank.application.command.CreateQuestionCommand;
import co.edu.unicauca.questionbank.application.command.QuestionType;
import co.edu.unicauca.questionbank.domain.exception.BusinessRuleException;
import co.edu.unicauca.questionbank.domain.model.Competency;
import co.edu.unicauca.questionbank.domain.model.QuestionStatus;
import co.edu.unicauca.questionbank.domain.model.Quiz;
import co.edu.unicauca.questionbank.domain.model.Role;
import co.edu.unicauca.questionbank.domain.model.Topic;
import co.edu.unicauca.questionbank.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Integration tests of the life cycle with the whole application (services + JPA + H2). The tests are
 * not transactional on purpose: every use case commits or rolls back its own transaction, exactly as
 * in production, and the tables are inspected afterwards with plain JDBC.
 */
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class QuizLifecycleIntegrationTest {

    @Autowired
    private QuestionService questions;
    @Autowired
    private QuizService quizzes;
    @Autowired
    private CatalogService catalog;
    @Autowired
    private JdbcTemplate jdbc;

    private User author;
    private Topic topic;
    private Competency design;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        author = catalog.registerUser("Ana Author", "ana." + suffix + "@unicauca.edu.co", Role.AUTHOR);
        topic = catalog.createTopic("Topic " + suffix);
        design = catalog.createCompetency("D" + suffix, "Solution design");
    }

    private Long draft() {
        return questions.create(new CreateQuestionCommand("Hibernate implements JPA", author.getId(),
                QuestionType.TRUE_FALSE, topic.getId(), List.of(design.getId()), null, true, null)).getId();
    }

    private long quizRows() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM quizzes", Long.class);
    }

    private String storedStatus(Long questionId) {
        return jdbc.queryForObject("SELECT status FROM questions WHERE id = ?", String.class, questionId);
    }

    @Test
    void everyTransitionIsCommittedToTheDatabase() {
        Long id = draft();
        assertEquals("DRAFT", storedStatus(id));

        questions.submitForReview(id);
        assertEquals("PENDING_REVIEW", storedStatus(id));
        questions.startReview(id);
        questions.approve(id);
        assertEquals("APPROVED", storedStatus(id));
        questions.publish(id);
        assertEquals("PUBLISHED", storedStatus(id));
    }

    @Test
    void anInvalidTransitionDoesNotChangeTheStoredState() {
        Long id = draft();

        assertThrows(BusinessRuleException.class, () -> questions.publish(id));
        assertEquals("DRAFT", storedStatus(id));
    }

    @Test
    void questionsThatAreNotPublishedCannotBeUsedInQuizzes() {
        Long published = draft();
        questions.submitForReview(published);
        questions.startReview(published);
        questions.approve(published);
        questions.publish(published);
        Long inReview = draft();
        questions.submitForReview(inReview);
        long before = quizRows();

        assertThrows(BusinessRuleException.class, () -> quizzes.create("Quiz", List.of(published, draft())));
        assertThrows(BusinessRuleException.class, () -> quizzes.create("Quiz", List.of(published, inReview)));
        assertEquals(before, quizRows(), "A rejected quiz must not leave rows behind");

        Quiz quiz = quizzes.create("Mock exam", List.of(published));
        assertEquals(before + 1, quizRows());
        assertEquals(QuestionStatus.PUBLISHED, quizzes.findById(quiz.getId()).getQuestions().get(0).getStatus());
    }

    @Test
    void archivedQuestionsCannotBeAddedToNewQuizzes() {
        Long id = draft();
        questions.submitForReview(id);
        questions.startReview(id);
        questions.approve(id);
        questions.publish(id);
        Quiz existing = quizzes.create("Before archiving", List.of(id));
        questions.archive(id);

        assertThrows(BusinessRuleException.class, () -> quizzes.create("After archiving", List.of(id)));
        // The quiz created before keeps its question, now archived.
        assertEquals(QuestionStatus.ARCHIVED, quizzes.findById(existing.getId()).getQuestions().get(0).getStatus());
    }
}
