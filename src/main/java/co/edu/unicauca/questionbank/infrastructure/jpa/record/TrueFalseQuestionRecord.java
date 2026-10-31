package co.edu.unicauca.questionbank.infrastructure.jpa.record;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

/**
 * Relational model of a true/false question (table {@code true_false_questions}).
 */
@Entity
@Table(name = "true_false_questions")
@PrimaryKeyJoinColumn(name = "question_id", foreignKey = @ForeignKey(name = "fk_tfq_question"))
public class TrueFalseQuestionRecord extends QuestionRecord {

    @Column(name = "correct_answer", nullable = false)
    private boolean correctAnswer;

    /** Constructor required by JPA. */
    public TrueFalseQuestionRecord() {
    }

    public boolean isCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(boolean correctAnswer) {
        this.correctAnswer = correctAnswer;
    }
}
