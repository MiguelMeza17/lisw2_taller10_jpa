package co.edu.unicauca.questionbank.domain.model;

import co.edu.unicauca.questionbank.domain.exception.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests of the pure domain model. They do not need Spring nor a database.
 */
class QuestionTest {

    private User author;
    private Topic topic;
    private Competency competency;

    @BeforeEach
    void setUp() {
        author = new User(1L, "Ana Author", "ana@unicauca.edu.co", Role.AUTHOR);
        topic = new Topic(1L, "Software architecture");
        competency = new Competency(1L, "DESIGN", "Solution design");
    }

    private MultipleChoiceQuestion completeMultipleChoice() {
        MultipleChoiceQuestion q = new MultipleChoiceQuestion("Which pattern decouples persistence?", author);
        q.addOption(new AnswerOption("Repository", true));
        q.addOption(new AnswerOption("Singleton", false));
        q.addOption(new AnswerOption("Observer", false));
        q.addOption(new AnswerOption("Builder", false));
        q.addOption(new AnswerOption("Prototype", false));
        q.classify(topic, Set.of(competency));
        return q;
    }

    @Test
    void newQuestionStartsAsDraft() {
        assertEquals(QuestionStatus.DRAFT, new TrueFalseQuestion("Java is typed", author, true).getStatus());
    }

    @Test
    void onlyAuthorsCanWriteQuestions() {
        User student = new User("Sam", "sam@unicauca.edu.co", Role.STUDENT);
        assertThrows(BusinessRuleException.class, () -> new TrueFalseQuestion("Statement", student, true));
    }

    @Test
    void blankStatementIsRejected() {
        assertThrows(BusinessRuleException.class, () -> new MultipleChoiceQuestion("  ", author));
    }

    @Test
    void completeQuestionCanBeSubmittedForReview() {
        MultipleChoiceQuestion q = completeMultipleChoice();
        q.submitForReview();
        assertEquals(QuestionStatus.PENDING_REVIEW, q.getStatus());
    }

    @Test
    void cannotSubmitWithMissingOptions() {
        MultipleChoiceQuestion q = new MultipleChoiceQuestion("Statement", author);
        q.addOption(new AnswerOption("A", true));
        q.classify(topic, Set.of(competency));
        assertThrows(BusinessRuleException.class, q::submitForReview);
        assertEquals(QuestionStatus.DRAFT, q.getStatus());
    }

    @Test
    void cannotSubmitWithoutCorrectAnswer() {
        MultipleChoiceQuestion q = new MultipleChoiceQuestion("Statement", author);
        List.of("A", "B", "C", "D", "E").forEach(t -> q.addOption(new AnswerOption(t, false)));
        q.classify(topic, Set.of(competency));
        assertThrows(BusinessRuleException.class, q::submitForReview);
    }

    @Test
    void cannotSubmitWithoutClassification() {
        TrueFalseQuestion q = new TrueFalseQuestion("Statement", author, false);
        assertThrows(BusinessRuleException.class, q::submitForReview);
    }

    @Test
    void cannotAddMoreThanFiveOptions() {
        MultipleChoiceQuestion q = completeMultipleChoice();
        assertThrows(BusinessRuleException.class, () -> q.addOption(new AnswerOption("Extra", false)));
    }

    @Test
    void cannotAddSecondCorrectAnswer() {
        MultipleChoiceQuestion q = new MultipleChoiceQuestion("Statement", author);
        q.addOption(new AnswerOption("A", true));
        assertThrows(BusinessRuleException.class, () -> q.addOption(new AnswerOption("B", true)));
    }

    @Test
    void cannotRepeatOptionText() {
        MultipleChoiceQuestion q = new MultipleChoiceQuestion("Statement", author);
        q.addOption(new AnswerOption("Same", false));
        assertThrows(BusinessRuleException.class, () -> q.addOption(new AnswerOption("same", false)));
    }

    @Test
    void optionsCannotChangeOutsideDraft() {
        MultipleChoiceQuestion q = completeMultipleChoice();
        q.submitForReview();
        assertThrows(BusinessRuleException.class, q::clearOptions);
    }

    @Test
    void fullLifeCycleUntilArchived() {
        MultipleChoiceQuestion q = completeMultipleChoice();
        q.submitForReview();
        q.startReview();
        q.approve();
        q.publish();
        assertTrue(q.isPublished());
        q.archive();
        assertEquals(QuestionStatus.ARCHIVED, q.getStatus());
    }

    @Test
    void rejectedQuestionGoesBackToDraft() {
        MultipleChoiceQuestion q = completeMultipleChoice();
        q.submitForReview();
        q.startReview();
        q.reject();
        assertEquals(QuestionStatus.DRAFT, q.getStatus());
    }

    @Test
    void cannotPublishWithoutApproval() {
        MultipleChoiceQuestion q = completeMultipleChoice();
        q.submitForReview();
        assertThrows(BusinessRuleException.class, q::publish);
    }

    @Test
    void quizOnlyAcceptsPublishedQuestions() {
        Quiz quiz = new Quiz("Mock exam 1");
        MultipleChoiceQuestion draft = completeMultipleChoice();
        assertThrows(BusinessRuleException.class, () -> quiz.addQuestion(draft));
    }

    @Test
    void quizRejectsRepeatedQuestions() {
        MultipleChoiceQuestion published = new MultipleChoiceQuestion(10L, "Statement", author, topic,
                Set.of(competency), null, QuestionStatus.PUBLISHED, List.of());
        Quiz quiz = new Quiz("Mock exam 1");
        quiz.addQuestion(published);
        assertThrows(BusinessRuleException.class, () -> quiz.addQuestion(published));
    }

    @Test
    void metadataRequiresPositiveTime() {
        assertThrows(BusinessRuleException.class,
                () -> new QuestionMetadata(null, CognitiveLevel.APPLY, 0, null));
    }
}
