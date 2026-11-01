package co.edu.unicauca.questionbank.infrastructure.jpa;

import co.edu.unicauca.questionbank.infrastructure.jpa.mapper.DomainMapper;
import co.edu.unicauca.questionbank.infrastructure.jpa.repository.CompetencyRepositoryJpa;
import co.edu.unicauca.questionbank.infrastructure.jpa.repository.QuestionRepositoryJpa;
import co.edu.unicauca.questionbank.infrastructure.jpa.repository.QuizRepositoryJpa;
import co.edu.unicauca.questionbank.infrastructure.jpa.repository.TopicRepositoryJpa;
import co.edu.unicauca.questionbank.infrastructure.jpa.repository.UserRepositoryJpa;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

/**
 * Adds to a {@code @DataJpaTest} slice the domain repository implementations and the mapper, which are
 * not Spring Data interfaces and therefore are not loaded by the slice on their own.
 */
@TestConfiguration
@Import({DomainMapper.class, UserRepositoryJpa.class, TopicRepositoryJpa.class, CompetencyRepositoryJpa.class,
        QuestionRepositoryJpa.class, QuizRepositoryJpa.class})
public class JpaTestConfig {
}
