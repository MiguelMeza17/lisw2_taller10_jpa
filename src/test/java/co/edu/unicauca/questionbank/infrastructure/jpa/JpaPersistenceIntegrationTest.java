package co.edu.unicauca.questionbank.infrastructure.jpa;

import co.edu.unicauca.questionbank.domain.exception.BusinessRuleException;
import co.edu.unicauca.questionbank.domain.model.AnswerOption;
import co.edu.unicauca.questionbank.domain.model.CognitiveLevel;
import co.edu.unicauca.questionbank.domain.model.Competency;
import co.edu.unicauca.questionbank.domain.model.MultipleChoiceQuestion;
import co.edu.unicauca.questionbank.domain.model.Question;
import co.edu.unicauca.questionbank.domain.model.QuestionMetadata;
import co.edu.unicauca.questionbank.domain.model.Quiz;
import co.edu.unicauca.questionbank.domain.model.Role;
import co.edu.unicauca.questionbank.domain.model.Topic;
import co.edu.unicauca.questionbank.domain.model.TrueFalseQuestion;
import co.edu.unicauca.questionbank.domain.model.User;
import co.edu.unicauca.questionbank.domain.repository.CompetencyRepository;
import co.edu.unicauca.questionbank.domain.repository.QuestionRepository;
import co.edu.unicauca.questionbank.domain.repository.QuizRepository;
import co.edu.unicauca.questionbank.domain.repository.TopicRepository;
import co.edu.unicauca.questionbank.domain.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Integration tests of the JPA infrastructure on H2. Besides using the repositories, they inspect the
 * tables with native SQL to show how the relationships and the JOINED inheritance are stored, and
 * write invalid rows directly to prove that the foreign keys and constraints reject them.
 */
@DataJpaTest
@Import(JpaTestConfig.class)
class JpaPersistenceIntegrationTest {

    @Autowired
    private QuestionRepository questions;
    @Autowired
    private QuizRepository quizzes;
    @Autowired
    private UserRepository users;
    @Autowired
    private TopicRepository topics;
    @Autowired
    private CompetencyRepository competencies;
    @Autowired
    private EntityManager em;

    private User author;
    private Topic topic;
    private Competency design;

    @BeforeEach
    void setUp() {
        author = users.save(new User("Ana Author", "ana@unicauca.edu.co", Role.AUTHOR));
        topic = topics.save(new Topic("Databases"));
        design = competencies.save(new Competency("DESIGN", "Solution design"));
    }

    private MultipleChoiceQuestion complete(String statement) {
        MultipleChoiceQuestion q = new MultipleChoiceQuestion(statement, author);
        for (String text : List.of("Correct", "Wrong A", "Wrong B", "Wrong C", "Wrong D")) {
            q.addOption(new AnswerOption(text, text.equals("Correct")));
        }
        q.classify(topic, Set.of(design));
        return q;
    }

    private Question published(String statement) {
        Question q = questions.save(complete(statement));
        q.submitForReview();
        q.startReview();
        q.approve();
        q.publish();
        return questions.save(q);
    }

    private long count(String sql) {
        em.flush();
        return ((Number) em.createNativeQuery(sql).getSingleResult()).longValue();
    }

    private void clear() {
        em.flush();
        em.clear();
    }

    // ---------------------------------------------------------------- save and retrieve, relationships

    @Test
    void storesEachRelationshipInItsTable() {
        MultipleChoiceQuestion q = complete("Which normal form removes transitive dependencies?");
        q.updateMetadata(new QuestionMetadata(null, CognitiveLevel.ANALYZE, 4, null));
        Long id = questions.save(q).getId();
        clear();

        // @ManyToOne author and topic: foreign keys in "questions"
        assertEquals(author.getId(), ((Number) em.createNativeQuery(
                "SELECT author_id FROM questions WHERE id = " + id).getSingleResult()).longValue());
        assertEquals(topic.getId(), ((Number) em.createNativeQuery(
                "SELECT topic_id FROM questions WHERE id = " + id).getSingleResult()).longValue());
        // @ManyToMany: row in the join table
        assertEquals(1, count("SELECT COUNT(*) FROM question_competency WHERE question_id = " + id));
        // @OneToMany: five options pointing to the question
        assertEquals(5, count("SELECT COUNT(*) FROM answer_options WHERE question_id = " + id));
        // @OneToOne: a single metadata row
        assertEquals(1, count("SELECT COUNT(*) FROM question_metadata WHERE question_id = " + id));

        MultipleChoiceQuestion loaded = (MultipleChoiceQuestion) questions.findById(id).orElseThrow();
        assertEquals("ana@unicauca.edu.co", loaded.getAuthor().getEmail());
        assertEquals("Databases", loaded.getTopic().getName());
        assertEquals("DESIGN", loaded.getCompetencies().iterator().next().getCode());
        assertEquals("Correct", loaded.getOptions().get(0).getText());
        assertEquals(CognitiveLevel.ANALYZE, loaded.getMetadata().getCognitiveLevel());
    }

    @Test
    void storesAndRetrievesQuizzesWithTheirQuestionsInOrder() {
        Question first = published("First");
        Question second = published("Second");
        Quiz quiz = new Quiz("Mock exam");
        quiz.addQuestion(second);
        quiz.addQuestion(first);
        Long quizId = quizzes.save(quiz).getId();
        clear();

        Quiz loaded = quizzes.findById(quizId).orElseThrow();
        assertEquals(List.of(second.getId(), first.getId()), loaded.getQuestions().stream().map(Question::getId).toList());
        assertEquals(2, count("SELECT COUNT(*) FROM quiz_question WHERE quiz_id = " + quizId));
    }

    @Test
    void removingTheMetadataDeletesItsRow() {
        MultipleChoiceQuestion q = complete("Statement");
        q.updateMetadata(new QuestionMetadata(null, CognitiveLevel.APPLY, 2, null));
        Question saved = questions.save(q);
        saved.updateMetadata(null);
        questions.save(saved);

        assertEquals(0, count("SELECT COUNT(*) FROM question_metadata WHERE question_id = " + saved.getId()));
    }

    // ---------------------------------------------------------------- JOINED inheritance

    @Test
    void joinedInheritanceSplitsTheRowsAndRebuildsTheSubtypes() {
        Long mcId = questions.save(complete("Multiple choice")).getId();
        Long tfId = questions.save(new TrueFalseQuestion("JPA is a specification", author, true)).getId();
        clear();

        assertEquals(2, count("SELECT COUNT(*) FROM questions"));
        assertEquals(1, count("SELECT COUNT(*) FROM multiple_choice_questions WHERE question_id = " + mcId));
        assertEquals(1, count("SELECT COUNT(*) FROM true_false_questions WHERE question_id = " + tfId));
        assertEquals(0, count("SELECT COUNT(*) FROM true_false_questions WHERE question_id = " + mcId));

        assertInstanceOf(MultipleChoiceQuestion.class, questions.findById(mcId).orElseThrow());
        TrueFalseQuestion tf = assertInstanceOf(TrueFalseQuestion.class, questions.findById(tfId).orElseThrow());
        assertEquals(true, tf.getCorrectAnswer());
    }

    // ---------------------------------------------------------------- foreign keys and constraints

    @Test
    void foreignKeyRejectsAnOptionOfAnUnknownQuestion() {
        assertThrows(PersistenceException.class, () -> em.createNativeQuery(
                "INSERT INTO answer_options (text, correct, position, question_id) VALUES ('x', false, 0, 9999)")
                .executeUpdate());
    }

    @Test
    void foreignKeyRejectsAQuestionOfAnUnknownAuthor() {
        assertThrows(PersistenceException.class, () -> em.createNativeQuery(
                "INSERT INTO questions (statement, status, author_id) VALUES ('x', 'DRAFT', 9999)")
                .executeUpdate());
    }

    @Test
    void foreignKeyPreventsDeletingAnAuthorThatHasQuestions() {
        questions.save(new TrueFalseQuestion("Statement", author, true));
        em.flush();

        assertThrows(PersistenceException.class, () -> em.createNativeQuery(
                "DELETE FROM users WHERE id = " + author.getId()).executeUpdate());
    }

    @Test
    void foreignKeyRejectsAQuizWithAnUnknownQuestion() {
        Long quizId = quizzes.save(new Quiz("Empty quiz")).getId();
        em.flush();

        assertThrows(PersistenceException.class, () -> em.createNativeQuery(
                "INSERT INTO quiz_question (quiz_id, question_id, position) VALUES (" + quizId + ", 9999, 0)")
                .executeUpdate());
    }

    @Test
    void uniqueConstraintAllowsOnlyOneMetadataRowPerQuestion() {
        MultipleChoiceQuestion q = complete("Statement");
        q.updateMetadata(new QuestionMetadata(null, CognitiveLevel.APPLY, 2, null));
        Long id = questions.save(q).getId();
        em.flush();

        assertThrows(PersistenceException.class, () -> em.createNativeQuery(
                "INSERT INTO question_metadata (question_id, cognitive_level, estimated_time_minutes) "
                        + "VALUES (" + id + ", 'APPLY', 2)").executeUpdate());
    }

    @Test
    void uniqueConstraintRejectsADuplicatedEmail() {
        assertThrows(DataIntegrityViolationException.class, () -> {
            users.save(new User("Another Ana", "ana@unicauca.edu.co", Role.REVIEWER));
            em.flush();
        });
    }

    @Test
    void repositoryRejectsReferencesThatDoNotExist() {
        User ghost = new User(9999L, "Ghost", "ghost@unicauca.edu.co", Role.AUTHOR);
        assertThrows(BusinessRuleException.class,
                () -> questions.save(new TrueFalseQuestion("Statement", ghost, true)));

        MultipleChoiceQuestion q = complete("Statement");
        q.classify(new Topic(9999L, "Ghost topic"), Set.of(design));
        assertThrows(BusinessRuleException.class, () -> questions.save(q));

        MultipleChoiceQuestion q2 = complete("Statement");
        q2.classify(topic, Set.of(new Competency(9999L, "GHOST", "Ghost competency")));
        assertThrows(BusinessRuleException.class, () -> questions.save(q2));
    }
}
