# Parte III — Preguntas de reflexión

## 1. ¿Qué ventajas y desventajas tiene mantener dos modelos, uno de dominio y otro de persistencia?

**Ventajas**

- El dominio (`domain/model`) queda libre de anotaciones y detalles técnicos: las reglas de negocio (ciclo de vida, 4 distractores + 1 respuesta correcta, solo preguntas publicadas en cuestionarios) se leen y se prueban sin base de datos (`QuestionTest` corre sin Spring).
- Cada modelo evoluciona por su cuenta: se puede cambiar una tabla, un índice o una estrategia de herencia sin tocar las clases de negocio, y al revés.
- El dominio puede proteger sus invariantes (constructores que validan, listas inmodificables), algo que choca con lo que JPA exige (constructor vacío, setters, colecciones mutables).

**Desventajas**

- Hay más clases y código repetido: cada entidad existe dos veces (`Question` y `QuestionRecord`) y hace falta un mapeador (`DomainMapper`).
- La conversión tiene un costo y puede introducir errores si se olvida mapear un campo.
- Se pierden algunas facilidades de JPA sobre el objeto de dominio, como el *dirty checking* automático: hay que copiar los cambios al Record explícitamente.

## 2. ¿Qué cambios serían necesarios si el sistema dejara de utilizar JPA?

Solo cambiaría la infraestructura:

- Se reemplazan los paquetes `infrastructure/jpa/record`, `repository` y `mapper` por otra implementación (JDBC, MongoDB, archivos, un servicio remoto…) que implemente las mismas interfaces de `domain/repository`.
- Se ajustan las dependencias de Maven y la configuración.

**No cambian** el dominio ni los servicios de aplicación, porque dependen de `QuestionRepository` y no de Spring Data (inversión de dependencias). Las pruebas que usan repositorios en memoria siguen sirviendo.

## 3. ¿Cuándo conviene una relación muchos a muchos directa y cuándo es mejor crear una entidad asociativa?

- **Directa (`@ManyToMany`)** cuando la relación no tiene datos propios: solo importa *qué* competencias evalúa una pregunta. Es el caso de `question_competency`.
- **Entidad asociativa** cuando la relación tiene atributos o comportamiento propio. Por ejemplo, si cada competencia tuviera un *peso* dentro de la pregunta, o si el orden, el puntaje o la fecha en que una pregunta se agregó a un cuestionario fueran importantes y necesitaran reglas. En ese caso se modela, por ejemplo, `QuizQuestion` con `@ManyToOne` hacia ambos lados.
- En `quiz_question` solo guardamos la posición (`@OrderColumn`). Si apareciera un puntaje por pregunta, convendría convertirla en entidad asociativa.

## 4. ¿Qué problemas pueden aparecer al recuperar asociaciones perezosas (LAZY) fuera de una transacción?

- `LazyInitializationException`: al acceder a una colección o relación LAZY después de cerrar la sesión de Hibernate, no hay conexión para cargarla.
- Si se "soluciona" con `open-in-view` o pasando todo a EAGER, aparecen consultas ocultas en la capa web, el problema N+1 y cargas innecesarias de datos.

**Cómo lo evitamos:** `spring.jpa.open-in-view=false`, y los repositorios `*RepositoryJpa` son `@Transactional`. Convierten el Record a objeto de dominio *dentro* de la transacción, así que fuera de la infraestructura nunca circulan entidades JPA con asociaciones sin cargar.

## 5. ¿Qué estrategia de herencia elegirían si el sistema incorpora muchos tipos de preguntas con atributos y reglas diferentes?

**`JOINED`**, la que usamos:

- Cada tipo guarda sus atributos propios en su tabla (`multiple_choice_questions`, `true_false_questions`) y los comunes en `questions`. Agregar un tipo nuevo es agregar una tabla, sin columnas vacías.
- Permite restricciones `NOT NULL` por subtipo, cosa que `SINGLE_TABLE` no puede hacer porque todas sus columnas específicas deben ser nulas.
- Las consultas polimórficas (todas las preguntas de un autor) siguen siendo sencillas, a diferencia de `TABLE_PER_CLASS`, que requiere `UNION`.
- Su costo son los *joins*, aceptable para este volumen de datos. Si el rendimiento de lectura fuera crítico y los tipos tuvieran pocos atributos distintos, se podría evaluar `SINGLE_TABLE`.

Las **reglas** diferentes de cada tipo no dependen de la estrategia de herencia: viven en el dominio (`validateStructure()` en cada subclase de `Question`).

## 6. ¿Por qué la persistencia de una pregunta y la validación de su ciclo de vida son responsabilidades distintas?

- **Validar el ciclo de vida** es una regla de negocio: qué transiciones son válidas (`QuestionStatus.canTransitionTo`) y qué condiciones debe cumplir una pregunta para enviarse a revisión. Debe cumplirse sin importar dónde o cómo se guarde la pregunta, y por eso vive en el dominio.
- **Persistir** es un detalle técnico: cómo se convierte el estado a columnas y filas.
- Si la validación estuviera en el repositorio o en la base de datos, cambiar de tecnología implicaría reescribir reglas de negocio, y además se podría "guardar" un estado inválido actualizando una columna directamente.
- Separarlas cumple SRP: el servicio pide al dominio la transición (`question.submitForReview()`) y luego al repositorio que la guarde, dentro de una transacción.
