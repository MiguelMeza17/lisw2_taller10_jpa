# Taller 10 — Persistencia de datos con JPA y arquitectura limpia

Banco de Preguntas Saber Pro · Laboratorio de Ingeniería de Software II · Universidad del Cauca · 2026.2

## Tecnologías

- Java 17+
- Spring Boot 3, Spring Data JPA
- H2 (base de datos en memoria)
- Maven, JUnit 5

## Estructura de paquetes

```
co.edu.unicauca.questionbank
├── api/                      # Controladores REST
├── application/              # Casos de uso (servicios)
├── domain/
│   ├── model/                # Clases de dominio puras (sin JPA)
│   └── repository/           # Contratos de repositorio
└── infrastructure/
    └── jpa/
        ├── record/           # Modelos relacionales JPA
        ├── repository/       # Implementaciones con Spring Data JPA
        └── mapper/           # Conversión dominio <-> record
```

## Análisis del dominio

**Actores:** Administrador (usuarios, roles y parámetros), Autor (crea y modifica preguntas), Revisor (evalúa calidad y pertinencia), Docente (organiza cuestionarios) y Estudiante (responde y consulta resultados).

**Ciclo de vida de una pregunta:**

```mermaid
stateDiagram-v2
    [*] --> DRAFT
    DRAFT --> PENDING_REVIEW : submitForReview()
    PENDING_REVIEW --> IN_REVIEW : startReview()
    PENDING_REVIEW --> DRAFT : reject()
    IN_REVIEW --> APPROVED : approve()
    IN_REVIEW --> DRAFT : reject()
    APPROVED --> PUBLISHED : publish()
    PUBLISHED --> ARCHIVED : archive()
```

Solo las preguntas **publicadas** pueden usarse en cuestionarios.

**Alcance elegido:** `User`, `Role`, `Topic`, `Competency`, `Question` (abstracta) con `MultipleChoiceQuestion` y `TrueFalseQuestion`, `AnswerOption`, `QuestionMetadata` y `Quiz`. Con estas clases se cubren los cuatro tipos de relación y la herencia que pide el taller. `Review`, `Attempt` y `Result` quedan fuera del alcance de este taller.

### Diagrama de clases del dominio

```mermaid
classDiagram
    class User {
        -Long id
        -String fullName
        -String email
        -Role role
        +canAuthorQuestions() boolean
    }
    class Role {
        <<enumeration>>
        ADMIN
        AUTHOR
        REVIEWER
        TEACHER
        STUDENT
    }
    class Topic {
        -Long id
        -String name
    }
    class Competency {
        -Long id
        -String code
        -String name
    }
    class Question {
        <<abstract>>
        -Long id
        -String statement
        -QuestionStatus status
        +classify(Topic, Collection~Competency~)
        +submitForReview()
        +startReview()
        +approve()
        +reject()
        +publish()
        +archive()
        #validateStructure()*
    }
    class MultipleChoiceQuestion {
        +REQUIRED_OPTIONS = 5
        +addOption(AnswerOption)
        +clearOptions()
    }
    class TrueFalseQuestion {
        -boolean correctAnswer
        +changeCorrectAnswer(boolean)
    }
    class AnswerOption {
        -String text
        -boolean correct
    }
    class QuestionMetadata {
        -LocalDate lastReviewDate
        -CognitiveLevel cognitiveLevel
        -int estimatedTimeMinutes
        -String editorialNotes
    }
    class QuestionStatus {
        <<enumeration>>
        DRAFT
        PENDING_REVIEW
        IN_REVIEW
        APPROVED
        PUBLISHED
        ARCHIVED
    }
    class Quiz {
        -Long id
        -String title
        +addQuestion(Question)
    }

    User --> Role
    Question <|-- MultipleChoiceQuestion
    Question <|-- TrueFalseQuestion
    Question "*" --> "1" User : author
    Question "*" --> "0..1" Topic : topic
    Question "*" --> "*" Competency : evaluates
    Question "1" --> "0..1" QuestionMetadata : metadata
    Question --> QuestionStatus
    MultipleChoiceQuestion "1" *-- "0..5" AnswerOption : options
    Quiz "*" --> "*" Question : questions
```

## Ejecución

```bash
mvn spring-boot:run
```

Consola H2: <http://localhost:8080/h2-console> (JDBC URL `jdbc:h2:mem:questionbank`, usuario `sa`, sin contraseña).

## Pruebas

```bash
mvn test
```
