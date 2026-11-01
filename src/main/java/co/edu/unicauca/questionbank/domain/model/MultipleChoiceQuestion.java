package co.edu.unicauca.questionbank.domain.model;

import co.edu.unicauca.questionbank.domain.exception.BusinessRuleException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Single answer multiple choice question. Following the academic rules of the bank, it must have
 * four distractors and exactly one correct answer (five options in total) before being reviewed.
 */
public class MultipleChoiceQuestion extends Question {

    /** Number of options required: four distractors plus the correct answer. */
    public static final int REQUIRED_OPTIONS = 5;

    private final List<AnswerOption> options = new ArrayList<>();

    /**
     * Creates a new draft multiple choice question without options.
     *
     * @param statement text of the question
     * @param author    author of the question
     */
    public MultipleChoiceQuestion(String statement, User author) {
        super(statement, author);
    }

    /**
     * Rebuilds a multiple choice question loaded from storage.
     *
     * @param id           identifier
     * @param statement    text of the question
     * @param author       author of the question
     * @param topic        topic, may be {@code null}
     * @param competencies evaluated competencies
     * @param metadata     metadata, may be {@code null}
     * @param status       life cycle state
     * @param options      answer options in display order
     */
    public MultipleChoiceQuestion(Long id, String statement, User author, Topic topic,
                                  Collection<Competency> competencies, QuestionMetadata metadata,
                                  QuestionStatus status, List<AnswerOption> options) {
        super(id, statement, author, topic, competencies, metadata, status);
        if (options != null) {
            if (options.size() > REQUIRED_OPTIONS) {
                throw new BusinessRuleException("A question cannot have more than " + REQUIRED_OPTIONS + " options.");
            }
            this.options.addAll(options);
        }
    }

    /**
     * Adds an answer option. Only allowed while the question is a draft.
     *
     * @param option option to add
     */
    public void addOption(AnswerOption option) {
        requireDraft("modify the options");
        if (option == null) {
            throw new BusinessRuleException("The answer option is required.");
        }
        if (options.size() >= REQUIRED_OPTIONS) {
            throw new BusinessRuleException("A question cannot have more than " + REQUIRED_OPTIONS + " options.");
        }
        boolean duplicated = options.stream().anyMatch(o -> o.getText().equalsIgnoreCase(option.getText()));
        if (duplicated) {
            throw new BusinessRuleException("Options of the same question must be different.");
        }
        if (option.isCorrect() && hasCorrectOption()) {
            throw new BusinessRuleException("The question already has a correct answer.");
        }
        options.add(option);
    }

    /**
     * Removes all the options. Only allowed while the question is a draft.
     */
    public void clearOptions() {
        requireDraft("modify the options");
        options.clear();
    }

    private boolean hasCorrectOption() {
        return options.stream().anyMatch(AnswerOption::isCorrect);
    }

    @Override
    protected void validateStructure() {
        if (options.size() != REQUIRED_OPTIONS) {
            throw new BusinessRuleException("A multiple choice question must have exactly " + REQUIRED_OPTIONS
                    + " options (4 distractors and 1 correct answer).");
        }
        long correct = options.stream().filter(AnswerOption::isCorrect).count();
        if (correct != 1) {
            throw new BusinessRuleException("A multiple choice question must have exactly one correct answer.");
        }
    }

    /**
     * Returns the options in display order.
     *
     * @return read only list of options
     */
    public List<AnswerOption> getOptions() {
        return Collections.unmodifiableList(options);
    }
}
