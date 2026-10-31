package co.edu.unicauca.questionbank.domain.model;

import co.edu.unicauca.questionbank.domain.exception.BusinessRuleException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Quiz or mock exam built from published questions.
 */
public class Quiz {

    private final Long id;
    private final String title;
    private final List<Question> questions = new ArrayList<>();

    /**
     * Creates an empty quiz that has not been persisted yet.
     *
     * @param title title of the quiz
     */
    public Quiz(String title) {
        this(null, title, List.of());
    }

    /**
     * Rebuilds a quiz loaded from storage. Questions are not validated again because they may have
     * been archived after the quiz was created.
     *
     * @param id        identifier
     * @param title     title of the quiz
     * @param questions questions of the quiz
     */
    public Quiz(Long id, String title, List<Question> questions) {
        if (title == null || title.isBlank()) {
            throw new BusinessRuleException("The quiz title is required.");
        }
        this.id = id;
        this.title = title.trim();
        if (questions != null) {
            this.questions.addAll(questions);
        }
    }

    /**
     * Adds a question to the quiz. Only published and persisted questions are accepted and a
     * question cannot be repeated.
     *
     * @param question question to add
     */
    public void addQuestion(Question question) {
        if (question == null) {
            throw new BusinessRuleException("The question is required.");
        }
        if (!question.isPublished()) {
            throw new BusinessRuleException("Only published questions can be used in a quiz. Question "
                    + question.getId() + " is " + question.getStatus() + ".");
        }
        if (question.getId() == null) {
            throw new BusinessRuleException("Only stored questions can be added to a quiz.");
        }
        boolean repeated = questions.stream().anyMatch(q -> Objects.equals(q.getId(), question.getId()));
        if (repeated) {
            throw new BusinessRuleException("Question " + question.getId() + " is already in the quiz.");
        }
        questions.add(question);
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public List<Question> getQuestions() {
        return Collections.unmodifiableList(questions);
    }
}
