package co.edu.unicauca.questionbank.domain.model;

/**
 * Roles of the people that interact with the question bank.
 */
public enum Role {
    /** Manages users, roles and system parameters. */
    ADMIN,
    /** Creates and edits questions. */
    AUTHOR,
    /** Evaluates the quality and relevance of questions. */
    REVIEWER,
    /** Organizes quizzes and mock exams. */
    TEACHER,
    /** Answers questions and checks results. */
    STUDENT
}
