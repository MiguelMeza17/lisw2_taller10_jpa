package co.edu.unicauca.questionbank;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the Saber Pro question bank application.
 */
@SpringBootApplication
public class QuestionBankApplication {

    /**
     * Starts the Spring Boot application.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(QuestionBankApplication.class, args);
    }
}
