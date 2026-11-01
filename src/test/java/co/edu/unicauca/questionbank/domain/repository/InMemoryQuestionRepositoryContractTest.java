package co.edu.unicauca.questionbank.domain.repository;

import co.edu.unicauca.questionbank.support.InMemoryRepositories;

/**
 * Runs the {@link QuestionRepositoryContract} against the in-memory implementation, without Spring
 * and without a database.
 */
class InMemoryQuestionRepositoryContractTest extends QuestionRepositoryContract {

    private final QuestionRepository questions = new InMemoryRepositories.Questions();
    private final UserRepository users = new InMemoryRepositories.Users();
    private final TopicRepository topics = new InMemoryRepositories.Topics();
    private final CompetencyRepository competencies = new InMemoryRepositories.Competencies();

    @Override
    protected QuestionRepository questions() {
        return questions;
    }

    @Override
    protected UserRepository users() {
        return users;
    }

    @Override
    protected TopicRepository topics() {
        return topics;
    }

    @Override
    protected CompetencyRepository competencies() {
        return competencies;
    }
}
