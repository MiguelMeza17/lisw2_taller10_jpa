package co.edu.unicauca.questionbank.infrastructure.jpa.record;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

/**
 * Relational model of a multiple choice question (table {@code multiple_choice_questions}).
 * <p>
 * {@code @OneToMany} with {@link AnswerOptionRecord}: the options own the foreign key, so this side uses
 * {@code mappedBy}. Options are saved and deleted together with the question ({@code cascade = ALL},
 * {@code orphanRemoval = true}).
 */
@Entity
@Table(name = "multiple_choice_questions")
@PrimaryKeyJoinColumn(name = "question_id", foreignKey = @ForeignKey(name = "fk_mcq_question"))
public class MultipleChoiceQuestionRecord extends QuestionRecord {

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<AnswerOptionRecord> options = new ArrayList<>();

    /** Constructor required by JPA. */
    public MultipleChoiceQuestionRecord() {
    }

    /**
     * Adds an option keeping both sides of the relationship in sync.
     *
     * @param option option to add
     */
    public void addOption(AnswerOptionRecord option) {
        option.setQuestion(this);
        options.add(option);
    }

    /**
     * Removes every option. Thanks to {@code orphanRemoval} the rows are deleted on flush.
     * The back reference is not set to {@code null} on purpose: Hibernate flushes updates before
     * deletes, and a {@code null} foreign key would violate the NOT NULL constraint before the
     * orphan rows are removed.
     * <p>
     * {@code removeIf} is used instead of {@code clear()} because it iterates the collection: a lazy
     * collection is initialized first, so Hibernate can detect every removed element as an orphan.
     * A plain {@code clear()} on an uninitialized collection is only queued and the old rows would stay.
     */
    public void clearOptions() {
        options.removeIf(option -> true);
    }

    public List<AnswerOptionRecord> getOptions() {
        return options;
    }
}
