package co.edu.unicauca.questionbank.domain.model;

import co.edu.unicauca.questionbank.domain.exception.BusinessRuleException;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Base class of every question of the bank. It owns the life cycle rules that are common to all
 * question types; each subtype adds its own structural validation.
 */
public abstract class Question {

    private final Long id;
    private String statement;
    private final User author;
    private Topic topic;
    private final Set<Competency> competencies = new LinkedHashSet<>();
    private QuestionMetadata metadata;
    private QuestionStatus status;

    /**
     * Creates a new question in {@link QuestionStatus#DRAFT} state.
     *
     * @param statement text of the question
     * @param author    user that writes the question, must have the author role
     */
    protected Question(String statement, User author) {
        this(null, statement, author, null, Set.of(), null, QuestionStatus.DRAFT);
    }

    /**
     * Rebuilds a question with all of its state, used when it is loaded from storage.
     *
     * @param id           identifier, may be {@code null}
     * @param statement    text of the question
     * @param author       author of the question
     * @param topic        topic, may be {@code null} while the question is not classified
     * @param competencies evaluated competencies
     * @param metadata     complementary metadata, may be {@code null}
     * @param status       current life cycle state
     */
    protected Question(Long id, String statement, User author, Topic topic,
                       Collection<Competency> competencies, QuestionMetadata metadata,
                       QuestionStatus status) {
        if (author == null) {
            throw new BusinessRuleException("Every question must have an author.");
        }
        if (!author.canAuthorQuestions()) {
            throw new BusinessRuleException("Only users with the AUTHOR role can write questions.");
        }
        if (status == null) {
            throw new BusinessRuleException("The question status is required.");
        }
        this.id = id;
        this.statement = requireStatement(statement);
        this.author = author;
        this.topic = topic;
        if (competencies != null) {
            this.competencies.addAll(competencies);
        }
        this.metadata = metadata;
        this.status = status;
    }

    private static String requireStatement(String statement) {
        if (statement == null || statement.isBlank()) {
            throw new BusinessRuleException("The question statement is required.");
        }
        return statement.trim();
    }

    /**
     * Replaces the statement. Only allowed while the question is a draft.
     *
     * @param newStatement new text of the question
     */
    public void changeStatement(String newStatement) {
        requireDraft("change the statement");
        this.statement = requireStatement(newStatement);
    }

    /**
     * Classifies the question with a topic and one or more competencies.
     * Only allowed while the question is a draft.
     *
     * @param newTopic        topic of the question
     * @param newCompetencies competencies evaluated by the question
     */
    public void classify(Topic newTopic, Collection<Competency> newCompetencies) {
        requireDraft("classify the question");
        if (newTopic == null) {
            throw new BusinessRuleException("A topic is required to classify the question.");
        }
        if (newCompetencies == null || newCompetencies.isEmpty()) {
            throw new BusinessRuleException("At least one competency is required to classify the question.");
        }
        this.topic = newTopic;
        this.competencies.clear();
        this.competencies.addAll(newCompetencies);
    }

    /**
     * Sets or replaces the metadata. Archived questions cannot be modified.
     *
     * @param newMetadata complementary metadata
     */
    public void updateMetadata(QuestionMetadata newMetadata) {
        if (status == QuestionStatus.ARCHIVED) {
            throw new BusinessRuleException("Archived questions cannot be modified.");
        }
        this.metadata = newMetadata;
    }

    /**
     * Sends the draft to review after checking the minimum conditions:
     * classified with a topic and competencies, and a valid structure for its type.
     */
    public void submitForReview() {
        requireDraft("submit the question for review");
        if (topic == null || competencies.isEmpty()) {
            throw new BusinessRuleException("The question must be classified with a topic and competencies before review.");
        }
        validateStructure();
        moveTo(QuestionStatus.PENDING_REVIEW);
    }

    /** Marks the question as being reviewed. */
    public void startReview() {
        moveTo(QuestionStatus.IN_REVIEW);
    }

    /** Approves a question that is under review. */
    public void approve() {
        moveTo(QuestionStatus.APPROVED);
    }

    /** Rejects the question and sends it back to draft so the author can fix it. */
    public void reject() {
        moveTo(QuestionStatus.DRAFT);
    }

    /** Publishes an approved question so it can be used in quizzes. */
    public void publish() {
        moveTo(QuestionStatus.PUBLISHED);
    }

    /** Archives a published question. */
    public void archive() {
        moveTo(QuestionStatus.ARCHIVED);
    }

    /**
     * Indicates whether the question can be included in a quiz.
     *
     * @return {@code true} only for published questions
     */
    public boolean isPublished() {
        return status == QuestionStatus.PUBLISHED;
    }

    /**
     * Checks the structure required by the concrete question type.
     *
     * @throws BusinessRuleException if the structure is not valid
     */
    protected abstract void validateStructure();

    /**
     * Fails if the question is not a draft.
     *
     * @param action description of the attempted action
     */
    protected void requireDraft(String action) {
        if (status != QuestionStatus.DRAFT) {
            throw new BusinessRuleException("Cannot " + action + " when the question is " + status + ".");
        }
    }

    private void moveTo(QuestionStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new BusinessRuleException("Invalid transition from " + status + " to " + target + ".");
        }
        this.status = target;
    }

    public Long getId() {
        return id;
    }

    public String getStatement() {
        return statement;
    }

    public User getAuthor() {
        return author;
    }

    public Topic getTopic() {
        return topic;
    }

    public Set<Competency> getCompetencies() {
        return Collections.unmodifiableSet(competencies);
    }

    public QuestionMetadata getMetadata() {
        return metadata;
    }

    public QuestionStatus getStatus() {
        return status;
    }
}
