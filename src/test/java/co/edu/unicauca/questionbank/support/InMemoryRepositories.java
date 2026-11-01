package co.edu.unicauca.questionbank.support;

import co.edu.unicauca.questionbank.domain.model.Competency;
import co.edu.unicauca.questionbank.domain.model.MultipleChoiceQuestion;
import co.edu.unicauca.questionbank.domain.model.Question;
import co.edu.unicauca.questionbank.domain.model.QuestionStatus;
import co.edu.unicauca.questionbank.domain.model.Quiz;
import co.edu.unicauca.questionbank.domain.model.Topic;
import co.edu.unicauca.questionbank.domain.model.TrueFalseQuestion;
import co.edu.unicauca.questionbank.domain.model.User;
import co.edu.unicauca.questionbank.domain.repository.CompetencyRepository;
import co.edu.unicauca.questionbank.domain.repository.QuestionRepository;
import co.edu.unicauca.questionbank.domain.repository.QuizRepository;
import co.edu.unicauca.questionbank.domain.repository.TopicRepository;
import co.edu.unicauca.questionbank.domain.repository.UserRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory implementations of the domain repository contracts. They let the use cases be tested
 * without Spring, JPA or a database, which shows that the application layer only depends on the
 * abstractions of the domain.
 * <p>
 * Every store copies the objects on {@code save}, assigning an identifier to new ones, just as a real
 * database would. Stored objects are immutable snapshots: callers always get a fresh copy.
 */
public final class InMemoryRepositories {

    private InMemoryRepositories() {
    }

    /** In-memory {@link UserRepository}. */
    public static class Users implements UserRepository {
        private final Map<Long, User> data = new TreeMap<>();
        private final AtomicLong sequence = new AtomicLong();

        @Override
        public User save(User user) {
            Long id = user.getId() != null ? user.getId() : sequence.incrementAndGet();
            boolean emailTaken = data.values().stream()
                    .anyMatch(u -> u.getEmail().equals(user.getEmail()) && !u.getId().equals(id));
            if (emailTaken) {
                throw new IllegalStateException("Duplicated email " + user.getEmail());
            }
            User stored = new User(id, user.getFullName(), user.getEmail(), user.getRole());
            data.put(id, stored);
            return stored;
        }

        @Override
        public Optional<User> findById(Long id) {
            return Optional.ofNullable(data.get(id));
        }
    }

    /** In-memory {@link TopicRepository}. */
    public static class Topics implements TopicRepository {
        private final Map<Long, Topic> data = new TreeMap<>();
        private final AtomicLong sequence = new AtomicLong();

        @Override
        public Topic save(Topic topic) {
            Long id = topic.getId() != null ? topic.getId() : sequence.incrementAndGet();
            Topic stored = new Topic(id, topic.getName());
            data.put(id, stored);
            return stored;
        }

        @Override
        public Optional<Topic> findById(Long id) {
            return Optional.ofNullable(data.get(id));
        }
    }

    /** In-memory {@link CompetencyRepository}. */
    public static class Competencies implements CompetencyRepository {
        private final Map<Long, Competency> data = new TreeMap<>();
        private final AtomicLong sequence = new AtomicLong();

        @Override
        public Competency save(Competency competency) {
            Long id = competency.getId() != null ? competency.getId() : sequence.incrementAndGet();
            Competency stored = new Competency(id, competency.getCode(), competency.getName());
            data.put(id, stored);
            return stored;
        }

        @Override
        public Optional<Competency> findById(Long id) {
            return Optional.ofNullable(data.get(id));
        }

        @Override
        public List<Competency> findAllById(Collection<Long> ids) {
            return ids.stream().distinct().map(data::get).filter(Objects::nonNull).toList();
        }
    }

    /** In-memory {@link QuestionRepository}. */
    public static class Questions implements QuestionRepository {
        private final Map<Long, Question> data = new TreeMap<>();
        private final AtomicLong sequence = new AtomicLong();

        @Override
        public Question save(Question question) {
            if (question.getId() != null && !data.containsKey(question.getId())) {
                throw new IllegalStateException("Question " + question.getId() + " does not exist.");
            }
            Long id = question.getId() != null ? question.getId() : sequence.incrementAndGet();
            Question stored = copy(question, id);
            data.put(id, stored);
            return copy(stored, id);
        }

        @Override
        public Optional<Question> findById(Long id) {
            return Optional.ofNullable(data.get(id)).map(q -> copy(q, id));
        }

        @Override
        public List<Question> findAll() {
            return data.values().stream().map(q -> copy(q, q.getId())).toList();
        }

        @Override
        public List<Question> findByAuthor(Long authorId) {
            return findAll().stream().filter(q -> Objects.equals(q.getAuthor().getId(), authorId)).toList();
        }

        @Override
        public List<Question> findByStatus(QuestionStatus status) {
            return findAll().stream().filter(q -> q.getStatus() == status).toList();
        }

        @Override
        public List<Question> findPublishedByCompetency(Long competencyId) {
            return findByStatus(QuestionStatus.PUBLISHED).stream()
                    .filter(q -> q.getCompetencies().stream().anyMatch(c -> Objects.equals(c.getId(), competencyId)))
                    .toList();
        }

        private static Question copy(Question q, Long id) {
            if (q instanceof MultipleChoiceQuestion mc) {
                return new MultipleChoiceQuestion(id, mc.getStatement(), mc.getAuthor(), mc.getTopic(),
                        mc.getCompetencies(), mc.getMetadata(), mc.getStatus(), new ArrayList<>(mc.getOptions()));
            }
            TrueFalseQuestion tf = (TrueFalseQuestion) q;
            return new TrueFalseQuestion(id, tf.getStatement(), tf.getAuthor(), tf.getTopic(),
                    tf.getCompetencies(), tf.getMetadata(), tf.getStatus(), tf.getCorrectAnswer());
        }
    }

    /** In-memory {@link QuizRepository}. */
    public static class Quizzes implements QuizRepository {
        private final Map<Long, Quiz> data = new TreeMap<>();
        private final AtomicLong sequence = new AtomicLong();

        @Override
        public Quiz save(Quiz quiz) {
            Long id = quiz.getId() != null ? quiz.getId() : sequence.incrementAndGet();
            Quiz stored = new Quiz(id, quiz.getTitle(), new ArrayList<>(quiz.getQuestions()));
            data.put(id, stored);
            return stored;
        }

        @Override
        public Optional<Quiz> findById(Long id) {
            return Optional.ofNullable(data.get(id));
        }

        /**
         * Number of stored quizzes, useful to check that a failed use case stored nothing.
         *
         * @return count of quizzes
         */
        public int count() {
            return data.size();
        }
    }
}
