package co.edu.unicauca.questionbank.infrastructure.jpa;

import co.edu.unicauca.questionbank.domain.repository.CompetencyRepository;
import co.edu.unicauca.questionbank.domain.repository.QuestionRepository;
import co.edu.unicauca.questionbank.domain.repository.QuestionRepositoryContract;
import co.edu.unicauca.questionbank.domain.repository.TopicRepository;
import co.edu.unicauca.questionbank.domain.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs the same {@link QuestionRepositoryContract} against the JPA implementation on H2.
 * <p>
 * The test is not wrapped in a transaction: every repository call commits on its own, so each read
 * opens a new persistence context and really comes from the database. The tables are emptied after
 * every test.
 */
@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import(JpaTestConfig.class)
class JpaQuestionRepositoryContractTest extends QuestionRepositoryContract {

    private static final String[] TABLES = {"quiz_question", "quizzes", "answer_options", "question_metadata",
        "question_competency", "multiple_choice_questions", "true_false_questions", "questions",
        "competencies", "topics", "users"};

    @Autowired
    private QuestionRepository questions;
    @Autowired
    private UserRepository users;
    @Autowired
    private TopicRepository topics;
    @Autowired
    private CompetencyRepository competencies;
    @Autowired
    private JdbcTemplate jdbc;

    @Override
    protected QuestionRepository questions() {
        return questions;
    }

    @Override
    protected UserRepository users() {
        return users;
    }

    @Override
    protected TopicRepository topics() {
        return topics;
    }

    @Override
    protected CompetencyRepository competencies() {
        return competencies;
    }

    @AfterEach
    void emptyTables() {
        jdbc.execute("SET REFERENTIAL_INTEGRITY FALSE");
        for (String table : TABLES) {
            jdbc.execute("TRUNCATE TABLE " + table);
        }
        jdbc.execute("SET REFERENTIAL_INTEGRITY TRUE");
    }
}
