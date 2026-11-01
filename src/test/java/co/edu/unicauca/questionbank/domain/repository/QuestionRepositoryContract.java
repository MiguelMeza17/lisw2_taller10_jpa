package co.edu.unicauca.questionbank.domain.repository;

import co.edu.unicauca.questionbank.domain.model.AnswerOption;
import co.edu.unicauca.questionbank.domain.model.CognitiveLevel;
import co.edu.unicauca.questionbank.domain.model.Competency;
import co.edu.unicauca.questionbank.domain.model.MultipleChoiceQuestion;
import co.edu.unicauca.questionbank.domain.model.Question;
import co.edu.unicauca.questionbank.domain.model.QuestionMetadata;
import co.edu.unicauca.questionbank.domain.model.QuestionStatus;
import co.edu.unicauca.questionbank.domain.model.Role;
import co.edu.unicauca.questionbank.domain.model.Topic;
import co.edu.unicauca.questionbank.domain.model.TrueFalseQuestion;
import co.edu.unicauca.questionbank.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Behaviour that every implementation of {@link QuestionRepository} must have, written only against
 * the domain contracts. It has no Spring nor JPA imports: each implementation (in memory, JPA...)
 * reuses these tests by extending this class and supplying its repositories.
 */
public abstract class QuestionRepositoryContract {

    /** @return implementation under test */
    protected abstract QuestionRepository questions();

    /** @return user repository that works with {@link #questions()} */
    protected abstract UserRepository users();

    /** @return topic repository that works with {@link #questions()} */
    protected abstract TopicRepository topics();

    /** @return competency repository that works with {@link #questions()} */
    protected abstract CompetencyRepository competencies();

    /**
     * Hook called after writing, so implementations backed by a database can clear their cache and
     * make the next read really come from storage.
     */
    protected void afterWrite() {
    }

    protected User author;
    protected Topic topic;
    protected Competency design;
    protected Competency analysis;

    @BeforeEach
    void createReferenceData() {
        author = users().save(new User("Ana Author", "ana@unicauca.edu.co", Role.AUTHOR));
        topic = topics().save(new Topic("Software architecture"));
        design = competencies().save(new Competency("DESIGN", "Solution design"));
        analysis = competencies().save(new Competency("ANALYSIS", "Problem analysis"));
    }

    protected MultipleChoiceQuestion completeMultipleChoice(String statement) {
        MultipleChoiceQuestion q = new MultipleChoiceQuestion(statement, author);
        q.addOption(new AnswerOption("Repository", true));
        q.addOption(new AnswerOption("Singleton", false));
        q.addOption(new AnswerOption("Observer", false));
        q.addOption(new AnswerOption("Builder", false));
        q.addOption(new AnswerOption("Prototype", false));
        q.classify(topic, Set.of(design, analysis));
        q.updateMetadata(new QuestionMetadata(null, CognitiveLevel.APPLY, 3, "Reviewed by the committee"));
        return q;
    }

    protected Question saveAndReload(Question question) {
        Question saved = questions().save(question);
        afterWrite();
        return questions().findById(saved.getId()).orElseThrow();
    }

    protected Question publish(Question question) {
        question.submitForReview();
        question.startReview();
        question.approve();
        question.publish();
        return question;
    }

    @Test
    void saveAssignsAnIdentifierAndFindByIdReturnsTheSameData() {
        Question loaded = saveAndReload(completeMultipleChoice("Which pattern decouples persistence?"));

        assertNotNull(loaded.getId());
        MultipleChoiceQuestion mc = assertInstanceOf(MultipleChoiceQuestion.class, loaded);
        assertEquals("Which pattern decouples persistence?", mc.getStatement());
        assertEquals(QuestionStatus.DRAFT, mc.getStatus());
        assertEquals(author.getId(), mc.getAuthor().getId());
        assertEquals(topic.getId(), mc.getTopic().getId());
        assertEquals(Set.of(design, analysis), mc.getCompetencies());
        assertEquals(List.of("Repository", "Singleton", "Observer", "Builder", "Prototype"),
                mc.getOptions().stream().map(AnswerOption::getText).toList());
        assertTrue(mc.getOptions().get(0).isCorrect());
        assertEquals(CognitiveLevel.APPLY, mc.getMetadata().getCognitiveLevel());
        assertEquals(3, mc.getMetadata().getEstimatedTimeMinutes());
    }

    @Test
    void keepsTheConcreteTypeOfEachQuestion() {
        Question tf = saveAndReload(new TrueFalseQuestion("JPA is a specification", author, true));
        Question mc = saveAndReload(completeMultipleChoice("Which pattern decouples persistence?"));

        assertTrue(assertInstanceOf(TrueFalseQuestion.class, tf).getCorrectAnswer());
        assertInstanceOf(MultipleChoiceQuestion.class, mc);
        assertEquals(2, questions().findAll().size());
    }

    @Test
    void savingAnExistingQuestionUpdatesItInsteadOfCreatingAnother() {
        Question saved = questions().save(completeMultipleChoice("Statement"));
        saved.submitForReview();

        Question updated = saveAndReload(saved);

        assertEquals(saved.getId(), updated.getId());
        assertEquals(QuestionStatus.PENDING_REVIEW, updated.getStatus());
        assertEquals(1, questions().findAll().size());
    }

    @Test
    void replacingTheOptionsOfADraftKeepsOnlyTheNewOnes() {
        MultipleChoiceQuestion saved = (MultipleChoiceQuestion) questions().save(completeMultipleChoice("Statement"));
        saved.clearOptions();
        saved.addOption(new AnswerOption("Only option", true));

        MultipleChoiceQuestion reloaded = (MultipleChoiceQuestion) saveAndReload(saved);

        assertEquals(List.of("Only option"), reloaded.getOptions().stream().map(AnswerOption::getText).toList());
    }

    @Test
    void findsByAuthorStatusAndPublishedCompetency() {
        User other = users().save(new User("Otto Other", "otto@unicauca.edu.co", Role.AUTHOR));
        Question published = questions().save(publish(completeMultipleChoice("Published")));
        questions().save(completeMultipleChoice("Draft"));
        questions().save(new TrueFalseQuestion("Other author", other, false));
        afterWrite();

        assertEquals(2, questions().findByAuthor(author.getId()).size());
        assertEquals(1, questions().findByAuthor(other.getId()).size());
        assertEquals(2, questions().findByStatus(QuestionStatus.DRAFT).size());
        assertEquals(List.of(published.getId()),
                questions().findPublishedByCompetency(design.getId()).stream().map(Question::getId).toList());
        Competency unused = competencies().save(new Competency("UNUSED", "Not evaluated"));
        assertTrue(questions().findPublishedByCompetency(unused.getId()).isEmpty());
    }

    @Test
    void findByIdOfAnUnknownQuestionIsEmpty() {
        assertFalse(questions().findById(123_456L).isPresent());
    }
}
