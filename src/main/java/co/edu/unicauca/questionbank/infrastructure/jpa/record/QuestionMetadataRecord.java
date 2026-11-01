package co.edu.unicauca.questionbank.infrastructure.jpa.record;

import co.edu.unicauca.questionbank.domain.model.CognitiveLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;

/**
 * Relational model of the metadata of a question (table {@code question_metadata}).
 * <p>
 * This side owns the one-to-one relationship: it stores the foreign key {@code question_id}, which is
 * mandatory and unique, so a question can never have more than one metadata row.
 */
@Entity
@Table(name = "question_metadata")
public class QuestionMetadataRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false, unique = true,
            foreignKey = @ForeignKey(name = "fk_metadata_question"))
    private QuestionRecord question;

    @Column(name = "last_review_date")
    private LocalDate lastReviewDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "cognitive_level", nullable = false, length = 20)
    private CognitiveLevel cognitiveLevel;

    @Column(name = "estimated_time_minutes", nullable = false)
    private int estimatedTimeMinutes;

    @Column(name = "editorial_notes", length = 1000)
    private String editorialNotes;

    /** Constructor required by JPA. */
    public QuestionMetadataRecord() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public QuestionRecord getQuestion() {
        return question;
    }

    public void setQuestion(QuestionRecord question) {
        this.question = question;
    }

    public LocalDate getLastReviewDate() {
        return lastReviewDate;
    }

    public void setLastReviewDate(LocalDate lastReviewDate) {
        this.lastReviewDate = lastReviewDate;
    }

    public CognitiveLevel getCognitiveLevel() {
        return cognitiveLevel;
    }

    public void setCognitiveLevel(CognitiveLevel cognitiveLevel) {
        this.cognitiveLevel = cognitiveLevel;
    }

    public int getEstimatedTimeMinutes() {
        return estimatedTimeMinutes;
    }

    public void setEstimatedTimeMinutes(int estimatedTimeMinutes) {
        this.estimatedTimeMinutes = estimatedTimeMinutes;
    }

    public String getEditorialNotes() {
        return editorialNotes;
    }

    public void setEditorialNotes(String editorialNotes) {
        this.editorialNotes = editorialNotes;
    }
}
