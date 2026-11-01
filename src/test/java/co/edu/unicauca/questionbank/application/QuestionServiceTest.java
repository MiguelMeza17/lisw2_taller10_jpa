package co.edu.unicauca.questionbank.application;

import co.edu.unicauca.questionbank.application.command.CreateQuestionCommand;
import co.edu.unicauca.questionbank.application.command.CreateQuestionCommand.Option;
import co.edu.unicauca.questionbank.application.command.QuestionCriteria;
import co.edu.unicauca.questionbank.application.command.QuestionType;
import co.edu.unicauca.questionbank.application.exception.ResourceNotFoundException;
import co.edu.unicauca.questionbank.domain.exception.BusinessRuleException;
import co.edu.unicauca.questionbank.domain.model.CognitiveLevel;
import co.edu.unicauca.questionbank.domain.model.Competency;
import co.edu.unicauca.questionbank.domain.model.MultipleChoiceQuestion;
import co.edu.unicauca.questionbank.domain.model.Question;
import co.edu.unicauca.questionbank.domain.model.QuestionStatus;
import co.edu.unicauca.questionbank.domain.model.Role;
import co.edu.unicauca.questionbank.domain.model.Topic;
import co.edu.unicauca.questionbank.domain.model.TrueFalseQuestion;
import co.edu.unicauca.questionbank.domain.model.User;
import co.edu.unicauca.questionbank.support.InMemoryRepositories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests of {@link QuestionService} with in-memory repositories: no Spring, no JPA, no database.
 */
class QuestionServiceTest {

    private QuestionService service;
    private CatalogService catalog;
    private User author;
    private User student;
    private Topic topic;
    private Competency design;

    @BeforeEach
    void setUp() {
        var users = new InMemoryRepositories.Users();
        var topics = new InMemoryRepositories.Topics();
        var competencies = new InMemoryRepositories.Competencies();
        service = new QuestionService(new InMemoryRepositories.Questions(), users, topics, competencies);
        catalog = new CatalogService(users, topics, competencies);

        author = catalog.registerUser("Ana Author", "ana@unicauca.edu.co", Role.AUTHOR);
        student = catalog.registerUser("Sam Student", "sam@unicauca.edu.co", Role.STUDENT);
        topic = catalog.createTopic("Software architecture");
        design = catalog.createCompetency("DESIGN", "Solution design");
    }

    private static List<Option> fiveOptions() {
        return List.of(new Option("Repository", true), new Option("Singleton", false),
                new Option("Observer", false), new Option("Builder", false), new Option("Prototype", false));
    }

    private CreateQuestionCommand multipleChoice(Long topicId, List<Long> competencyIds, List<Option> options) {
        return new CreateQuestionCommand("Which pattern decouples persistence?", author.getId(),
                QuestionType.MULTIPLE_CHOICE, topicId, competencyIds, options, null, null);
    }

    // ---------------------------------------------------------------- create

    @Test
    void createsAClassifiedMultipleChoiceDraft() {
        Question q = service.create(multipleChoice(topic.getId(), List.of(design.getId()), fiveOptions()));

        assertNotNull(q.getId());
        assertEquals(QuestionStatus.DRAFT, q.getStatus());
        assertEquals(topic.getId(), q.getTopic().getId());
        assertEquals(5, assertInstanceOf(MultipleChoiceQuestion.class, q).getOptions().size());
    }

    @Test
    void createsATrueFalseQuestionWithMetadata() {
        var command = new CreateQuestionCommand("JPA is a specification", author.getId(), QuestionType.TRUE_FALSE,
                null, null, null, true, new CreateQuestionCommand.Metadata(CognitiveLevel.REMEMBER, 2, "Easy"));

        TrueFalseQuestion q = assertInstanceOf(TrueFalseQuestion.class, service.create(command));

        assertTrue(q.getCorrectAnswer());
        assertEquals(CognitiveLevel.REMEMBER, q.getMetadata().getCognitiveLevel());
    }

    @Test
    void rejectsQuestionsWrittenByNonAuthors() {
        var command = new CreateQuestionCommand("Statement", student.getId(), QuestionType.TRUE_FALSE,
                null, null, null, false, null);
        assertThrows(BusinessRuleException.class, () -> service.create(command));
    }

    @Test
    void rejectsUnknownAuthorTopicOrCompetency() {
        var unknownAuthor = new CreateQuestionCommand("Statement", 99L, QuestionType.TRUE_FALSE,
                null, null, null, true, null);
        assertThrows(ResourceNotFoundException.class, () -> service.create(unknownAuthor));
        assertThrows(ResourceNotFoundException.class,
                () -> service.create(multipleChoice(99L, List.of(design.getId()), fiveOptions())));
        assertThrows(ResourceNotFoundException.class,
                () -> service.create(multipleChoice(topic.getId(), List.of(99L), fiveOptions())));
    }

    @Test
    void rejectsInvalidOptionsAndMissingData() {
        List<Option> twoCorrect = List.of(new Option("A", true), new Option("B", true));
        assertThrows(BusinessRuleException.class, () -> service.create(multipleChoice(null, null, twoCorrect)));

        var trueFalseWithoutAnswer = new CreateQuestionCommand("Statement", author.getId(), QuestionType.TRUE_FALSE,
                null, null, null, null, null);
        assertThrows(BusinessRuleException.class, () -> service.create(trueFalseWithoutAnswer));

        var withoutType = new CreateQuestionCommand("Statement", author.getId(), null, null, null, null, true, null);
        assertThrows(BusinessRuleException.class, () -> service.create(withoutType));

        assertThrows(BusinessRuleException.class,
                () -> service.create(multipleChoice(topic.getId(), List.of(), fiveOptions())),
                "A topic without competencies is not a valid classification");
    }

    // ---------------------------------------------------------------- search

    @Test
    void findsByIdAndFailsForUnknownIds() {
        Question created = service.create(multipleChoice(null, null, fiveOptions()));

        assertEquals(created.getStatement(), service.findById(created.getId()).getStatement());
        assertThrows(ResourceNotFoundException.class, () -> service.findById(404L));
    }

    @Test
    void searchesWithCriteria() {
        Question classified = service.create(multipleChoice(topic.getId(), List.of(design.getId()), fiveOptions()));
        service.create(multipleChoice(null, null, fiveOptions()));
        service.submitForReview(classified.getId());

        assertEquals(2, service.search(QuestionCriteria.any()).size());
        assertEquals(2, service.search(new QuestionCriteria(author.getId(), null, null, null)).size());
        assertEquals(1, service.search(new QuestionCriteria(null, QuestionStatus.DRAFT, null, null)).size());
        assertEquals(List.of(classified.getId()),
                service.search(new QuestionCriteria(author.getId(), QuestionStatus.PENDING_REVIEW,
                        topic.getId(), design.getId())).stream().map(Question::getId).toList());
        assertTrue(service.search(new QuestionCriteria(null, QuestionStatus.PUBLISHED, null, design.getId()))
                .isEmpty());
    }

    // ---------------------------------------------------------------- classify

    @Test
    void classifiesADraft() {
        Question q = service.create(multipleChoice(null, null, fiveOptions()));
        Competency analysis = catalog.createCompetency("ANALYSIS", "Problem analysis");

        Question classified = service.classify(q.getId(), topic.getId(), List.of(design.getId(), analysis.getId()));

        assertEquals(topic.getId(), classified.getTopic().getId());
        assertEquals(2, classified.getCompetencies().size());
    }

    @Test
    void cannotClassifyAQuestionThatLeftDraft() {
        Question q = service.create(multipleChoice(topic.getId(), List.of(design.getId()), fiveOptions()));
        service.submitForReview(q.getId());

        assertThrows(BusinessRuleException.class,
                () -> service.classify(q.getId(), topic.getId(), List.of(design.getId())));
    }

    // ---------------------------------------------------------------- submit for review and life cycle

    @Test
    void submitsAValidDraftForReview() {
        Question q = service.create(multipleChoice(topic.getId(), List.of(design.getId()), fiveOptions()));

        assertEquals(QuestionStatus.PENDING_REVIEW, service.submitForReview(q.getId()).getStatus());
        assertEquals(QuestionStatus.PENDING_REVIEW, service.findById(q.getId()).getStatus());
    }

    @Test
    void doesNotSubmitUnclassifiedOrIncompleteQuestions() {
        Question unclassified = service.create(multipleChoice(null, null, fiveOptions()));
        Question incomplete = service.create(multipleChoice(topic.getId(), List.of(design.getId()),
                fiveOptions().subList(0, 3)));

        assertThrows(BusinessRuleException.class, () -> service.submitForReview(unclassified.getId()));
        assertThrows(BusinessRuleException.class, () -> service.submitForReview(incomplete.getId()));
        assertEquals(QuestionStatus.DRAFT, service.findById(incomplete.getId()).getStatus());
    }

    @Test
    void walksTheWholeLifeCycle() {
        Long id = service.create(multipleChoice(topic.getId(), List.of(design.getId()), fiveOptions())).getId();

        service.submitForReview(id);
        service.startReview(id);
        service.approve(id);
        assertEquals(QuestionStatus.PUBLISHED, service.publish(id).getStatus());
        assertEquals(QuestionStatus.ARCHIVED, service.archive(id).getStatus());
        assertThrows(BusinessRuleException.class, () -> service.publish(id));
    }

    @Test
    void rejectingSendsTheQuestionBackToDraft() {
        Long id = service.create(multipleChoice(topic.getId(), List.of(design.getId()), fiveOptions())).getId();
        service.submitForReview(id);

        assertEquals(QuestionStatus.DRAFT, service.reject(id).getStatus());
    }
}
