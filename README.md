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

## Modelo relacional (JPA)

| Relación | Clases | Lado propietario / clave foránea |
|---|---|---|
| `@OneToOne` | `QuestionRecord` ↔ `QuestionMetadataRecord` | `question_metadata.question_id` (obligatoria y **única**: una pregunta no puede tener dos registros de metadatos) |
| `@OneToMany` / `@ManyToOne` | `MultipleChoiceQuestionRecord` → `AnswerOptionRecord` | `answer_options.question_id` (obligatoria; `cascade = ALL` y `orphanRemoval`: las opciones se eliminan con la pregunta) |
| `@ManyToOne` | `QuestionRecord` → `UserRecord` (autor) | `questions.author_id` (obligatoria) |
| `@ManyToOne` | `QuestionRecord` → `TopicRecord` | `questions.topic_id` (opcional hasta clasificar) |
| `@ManyToMany` | `QuestionRecord` ↔ `CompetencyRecord` | tabla intermedia `question_competency` |
| `@ManyToMany` | `QuizRecord` → `QuestionRecord` | tabla intermedia `quiz_question` (con `position`) |
| Herencia `JOINED` | `QuestionRecord` ← `MultipleChoiceQuestionRecord`, `TrueFalseQuestionRecord` | PK de cada subtabla = FK a `questions.id` |

```mermaid
erDiagram
    users ||--o{ questions : "author_id"
    topics |o--o{ questions : "topic_id"
    questions ||--o| question_metadata : "question_id (unique)"
    questions ||--o| multiple_choice_questions : "question_id (PK/FK)"
    questions ||--o| true_false_questions : "question_id (PK/FK)"
    multiple_choice_questions ||--o{ answer_options : "question_id"
    questions ||--o{ question_competency : "question_id"
    competencies ||--o{ question_competency : "competency_id"
    quizzes ||--o{ quiz_question : "quiz_id"
    questions ||--o{ quiz_question : "question_id"

    users {
        BIGINT id PK
        VARCHAR full_name
        VARCHAR email UK
        VARCHAR role
    }
    topics {
        BIGINT id PK
        VARCHAR name UK
    }
    competencies {
        BIGINT id PK
        VARCHAR code UK
        VARCHAR name
    }
    questions {
        BIGINT id PK
        VARCHAR statement
        VARCHAR status
        BIGINT author_id FK
        BIGINT topic_id FK
    }
    question_metadata {
        BIGINT id PK
        BIGINT question_id FK
        DATE last_review_date
        VARCHAR cognitive_level
        INT estimated_time_minutes
        VARCHAR editorial_notes
    }
    multiple_choice_questions {
        BIGINT question_id PK
    }
    true_false_questions {
        BIGINT question_id PK
        BOOLEAN correct_answer
    }
    answer_options {
        BIGINT id PK
        VARCHAR text
        BOOLEAN correct
        INT position
        BIGINT question_id FK
    }
    question_competency {
        BIGINT question_id FK
        BIGINT competency_id FK
    }
    quizzes {
        BIGINT id PK
        VARCHAR title
    }
    quiz_question {
        BIGINT quiz_id FK
        BIGINT question_id FK
        INT position
    }
```

## API REST

Los controladores de `api` solo reciben la solicitud, la convierten en una llamada a un caso de uso de `application` y devuelven DTOs (nunca objetos de dominio ni Records JPA).

| Método | Ruta | Caso de uso |
|---|---|---|
| `POST` | `/api/users` | Registrar usuario |
| `GET` | `/api/users/{id}` | Consultar usuario |
| `POST` | `/api/topics` | Crear tema |
| `POST` | `/api/competencies` | Crear competencia |
| `POST` | `/api/questions` | Crear pregunta (enunciado, autor, tipo, opciones, clasificación y metadatos opcionales) |
| `GET` | `/api/questions/{id}` | Consultar pregunta |
| `GET` | `/api/questions?authorId=&status=&topicId=&competencyId=` | Buscar preguntas por criterio |
| `PUT` | `/api/questions/{id}/classification` | Clasificar (tema y competencias) |
| `POST` | `/api/questions/{id}/submit` | Enviar a revisión (DRAFT → PENDING_REVIEW) |
| `POST` | `/api/questions/{id}/start-review` · `/approve` · `/reject` · `/publish` · `/archive` | Resto del ciclo de vida |
| `POST` | `/api/quizzes` | Crear cuestionario (solo preguntas publicadas) |
| `GET` | `/api/quizzes/{id}` | Consultar cuestionario |

Errores (`ProblemDetail`): `400` solicitud mal formada, `404` recurso inexistente, `409` violación de restricción (correo o código duplicado) y `422` regla de negocio incumplida.

```bash
curl -X POST localhost:8080/api/users -H "Content-Type: application/json" \
  -d '{"fullName":"Ana Autora","email":"ana@unicauca.edu.co","role":"AUTHOR"}'
curl -X POST localhost:8080/api/topics -H "Content-Type: application/json" -d '{"name":"Arquitectura de software"}'
curl -X POST localhost:8080/api/competencies -H "Content-Type: application/json" -d '{"code":"DESIGN","name":"Diseño de soluciones"}'
curl -X POST localhost:8080/api/questions -H "Content-Type: application/json" -d '{
  "statement":"¿Qué patrón desacopla la persistencia?","authorId":1,"type":"MULTIPLE_CHOICE",
  "topicId":1,"competencyIds":[1],
  "options":[{"text":"Repository","correct":true},{"text":"Singleton","correct":false},
             {"text":"Observer","correct":false},{"text":"Builder","correct":false},{"text":"Prototype","correct":false}]}'
curl -X POST localhost:8080/api/questions/1/submit
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
