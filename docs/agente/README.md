# Agente de documentación

> **Propósito a futuro:** esta carpeta está reservada para un **agente** que se encargará de responder y explicar dudas sobre el proyecto: por qué se tomó cada decisión de diseño, cómo funciona cada capa y cómo se relacionan las clases.
>
> Mientras ese agente no exista, aquí se guardan las explicaciones de cada cambio integrado. Los Pull Requests quedan con una descripción corta, y el detalle técnico vive en este documento para que el agente (o cualquier integrante del equipo) lo consulte.

## Cómo usar este documento

- Cada sección corresponde a un Pull Request y al issue que resuelve.
- Cuando se integre un PR nuevo, agregar su sección al final con el mismo formato.

---

## PR #18: Configurar proyecto Spring Boot y estructura de paquetes (issue #1)

- Proyecto Maven con Java 17, Spring Boot 3.3, Spring Data JPA, H2 y JUnit 5.
- Paquetes `api`, `application`, `domain/model`, `domain/repository` e `infrastructure/jpa/{record,repository,mapper}`, cada uno con su `package-info.java`.
- H2 en memoria con consola en `/h2-console`.
- Prueba de arranque del contexto.

## PR #19: Análisis del dominio y diagrama UML de clases (issue #6)

- README con actores, ciclo de vida de la pregunta (diagrama de estados) y alcance elegido.
- Diagrama de clases del dominio en Mermaid: se renderiza en GitHub y se puede exportar a PNG para el informe.

## PR #20: Modelo de dominio puro del Banco de Preguntas (issue #7)

Clases en `domain/model` **sin anotaciones ni imports de `jakarta.persistence`**:

- `Question` (abstracta), con `MultipleChoiceQuestion` (4 distractores + 1 correcta) y `TrueFalseQuestion`.
- `AnswerOption` y `QuestionMetadata` como objetos de valor.
- `Quiz`, que solo acepta preguntas publicadas.
- `User`, `Role`, `Topic`, `Competency` y `QuestionStatus` con sus transiciones.
- `BusinessRuleException` en `domain/exception`.

Pruebas: `QuestionTest`, 17 pruebas unitarias sin Spring.

## PR #21: Records JPA con las relaciones OneToOne, ManyToOne y ManyToMany (issue #8)

- `@OneToOne` Question ↔ QuestionMetadata: el **propietario es la metadata**. La FK `question_id` es obligatoria y **única**, así una pregunta no puede tener dos registros de metadatos.
- `@ManyToOne` Question → User (`author_id`, obligatoria) y Question → Topic (`topic_id`, opcional hasta clasificar). Las FK impiden asignar un autor o un tema inexistente.
- `@ManyToMany` Question ↔ Competency con la tabla intermedia `question_competency`; Quiz ↔ Question con `quiz_question` ordenada por `position`.
- Llaves foráneas con nombre explícito (`fk_question_author`, `fk_qc_competency`…).

## PR #22: Herencia JOINED para los tipos de pregunta y @OneToMany de opciones (issue #9)

- `QuestionRecord` con `@Inheritance(strategy = InheritanceType.JOINED)`.
- `MultipleChoiceQuestionRecord` (`multiple_choice_questions`) y `TrueFalseQuestionRecord` (`true_false_questions`), cuya PK es a la vez FK a `questions`.
- `@OneToMany` de opciones con `cascade = ALL` y `orphanRemoval`: al eliminar la pregunta se eliminan sus opciones, y la FK obligatoria impide opciones sin pregunta.
- `clearOptions()` usa `removeIf` y no anula la referencia inversa. Así Hibernate detecta las opciones eliminadas como huérfanas aunque la colección sea LAZY, y no intenta poner `question_id` en NULL antes de borrarlas (el detalle está en el JavaDoc).
- README: tabla de relaciones y diagrama Entidad-Relación en Mermaid.

Verificación: Hibernate genera las 11 tablas en H2.
