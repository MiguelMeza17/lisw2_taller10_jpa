package co.edu.unicauca.questionbank.infrastructure.jpa.record;

import co.edu.unicauca.questionbank.domain.model.QuestionStatus;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Relational model of the data shared by every question (table {@code questions}).
 * <ul>
 *     <li>{@code @ManyToOne} with {@link UserRecord}: the author, mandatory foreign key {@code author_id}.</li>
 *     <li>{@code @ManyToOne} with {@link TopicRecord}: optional foreign key {@code topic_id}.</li>
 *     <li>{@code @ManyToMany} with {@link CompetencyRecord}: join table {@code question_competency}.</li>
 *     <li>{@code @OneToOne} with {@link QuestionMetadataRecord}: inverse side, the metadata owns the key.</li>
 * </ul>
 */
@Entity
@Table(name = "questions")
public abstract class QuestionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 2000)
    private String statement;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuestionStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_question_author"))
    private UserRecord author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", foreignKey = @ForeignKey(name = "fk_question_topic"))
    private TopicRecord topic;

    @ManyToMany
    @JoinTable(name = "question_competency",
            joinColumns = @JoinColumn(name = "question_id",
                    foreignKey = @ForeignKey(name = "fk_qc_question")),
            inverseJoinColumns = @JoinColumn(name = "competency_id",
                    foreignKey = @ForeignKey(name = "fk_qc_competency")))
    private Set<CompetencyRecord> competencies = new LinkedHashSet<>();

    @OneToOne(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private QuestionMetadataRecord metadata;

    /** Constructor required by JPA. */
    protected QuestionRecord() {
    }

    /**
     * Sets the metadata keeping both sides of the relationship in sync.
     *
     * @param newMetadata metadata record, or {@code null} to remove it
     */
    public void setMetadata(QuestionMetadataRecord newMetadata) {
        if (newMetadata != null) {
            newMetadata.setQuestion(this);
        }
        this.metadata = newMetadata;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStatement() {
        return statement;
    }

    public void setStatement(String statement) {
        this.statement = statement;
    }

    public QuestionStatus getStatus() {
        return status;
    }

    public void setStatus(QuestionStatus status) {
        this.status = status;
    }

    public UserRecord getAuthor() {
        return author;
    }

    public void setAuthor(UserRecord author) {
        this.author = author;
    }

    public TopicRecord getTopic() {
        return topic;
    }

    public void setTopic(TopicRecord topic) {
        this.topic = topic;
    }

    public Set<CompetencyRecord> getCompetencies() {
        return competencies;
    }

    public QuestionMetadataRecord getMetadata() {
        return metadata;
    }
}
