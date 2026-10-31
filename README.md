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

## Ejecución

```bash
mvn spring-boot:run
```

Consola H2: <http://localhost:8080/h2-console> (JDBC URL `jdbc:h2:mem:questionbank`, usuario `sa`, sin contraseña).

## Pruebas

```bash
mvn test
```
