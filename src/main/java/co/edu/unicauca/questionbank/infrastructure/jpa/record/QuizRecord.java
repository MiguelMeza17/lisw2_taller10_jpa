package co.edu.unicauca.questionbank.infrastructure.jpa.record;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

/**
 * Relational model of a quiz (table {@code quizzes}). Its questions are stored in the join table
 * {@code quiz_question}, keeping the order in the {@code position} column.
 */
@Entity
@Table(name = "quizzes")
public class QuizRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @ManyToMany
    @JoinTable(name = "quiz_question",
            joinColumns = @JoinColumn(name = "quiz_id", foreignKey = @ForeignKey(name = "fk_qq_quiz")),
            inverseJoinColumns = @JoinColumn(name = "question_id",
                    foreignKey = @ForeignKey(name = "fk_qq_question")))
    @OrderColumn(name = "position")
    private List<QuestionRecord> questions = new ArrayList<>();

    /** Constructor required by JPA. */
    public QuizRecord() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<QuestionRecord> getQuestions() {
        return questions;
    }
}
