package co.edu.unicauca.questionbank.infrastructure.jpa.repository;

import co.edu.unicauca.questionbank.infrastructure.jpa.record.UserRecord;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data interface for {@link UserRecord}.
 */
public interface SpringUserDataRepository extends JpaRepository<UserRecord, Long> {
}
