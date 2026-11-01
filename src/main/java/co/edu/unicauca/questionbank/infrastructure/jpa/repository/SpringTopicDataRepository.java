package co.edu.unicauca.questionbank.infrastructure.jpa.repository;

import co.edu.unicauca.questionbank.infrastructure.jpa.record.TopicRecord;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data interface for {@link TopicRecord}.
 */
public interface SpringTopicDataRepository extends JpaRepository<TopicRecord, Long> {
}
