# DeepBlue Rescue

**Plataforma de rescate y rehabilitación de fauna marina**
Este proyecto implementa la capa de persistencia backend para gestionar centros de rescate, expedientes médicos, especialistas y tratamientos de animales marinos. Desarrollado con Java 21, Spring Boot 4, Spring Data JPA, Hibernate, PostgreSQL y Testcontainers.

## Modelo de Datos y Relaciones
El sistema está compuesto por 7 entidades principales con las siguientes asociaciones:
* **RescueCenter 1 ───────── N RescueCase:** Un centro gestiona múltiples casos de rescate.
* **RescueCase 1 ───────── 1 Animal:** Cada caso involucra a un único animal (relación bidireccional).
* **Animal 1 ───────── 1 MedicalRecord:** Cada animal posee un expediente médico exclusivo.
* **Specialist N ───────── M Expertise:** Los especialistas tienen múltiples áreas de experiencia (tabla intermedia `specialist_expertise`).
* **Animal 1 ───────── N Treatment:** Un animal puede recibir múltiples tratamientos.
* **Specialist 1 ───────── N Treatment:** Un especialista puede realizar múltiples tratamientos.

## Instrucciones para Ejecutar
Para ejecutar la aplicación principal, es necesario contar con Java 21 y una base de datos PostgreSQL local configurada en el archivo `application.yml`.
1. Abrir una terminal en la raíz del proyecto.
2. Ejecutar el comando: `.\mvnw spring-boot:run`

## Instrucciones para Ejecutar Tests
Las pruebas de integración utilizan contenedores efímeros y no requieren configurar una base de datos manual.
1. Asegurarse de tener Docker Desktop abierto y con el motor en ejecución.
2. Ejecutar el siguiente comando para limpiar compilaciones previas y lanzar las pruebas: `.\mvnw clean test`

## Arquitectura y Herramientas

### Explicación de Flyway
Flyway actúa como el controlador de versiones del esquema de base de datos relacional. En lugar de delegar la creación de tablas a Hibernate, Flyway ejecuta scripts SQL secuenciales (`V1`, `V2`, `V3`) para construir el esquema, crear restricciones y sembrar datos iniciales. Hibernate solo se utiliza en modo `ddl-auto: validate` para confirmar que el modelo de Java coincide con las tablas de Flyway.

### Explicación de Testcontainers
Testcontainers es una librería de Java que se integra con JUnit para levantar contenedores Docker reales (PostgreSQL) exclusivamente durante la ejecución de las pruebas. Esto asegura que las consultas JPQL se validen contra un motor idéntico al de producción. El contenedor se destruye automáticamente al terminar la prueba.

## Diccionario de Consultas

### Listado de Query Methods Implementados
Métodos derivados por convención de nombres en las interfaces JpaRepository:
* `findByCode(String code)`
* `findByCaseCode(String caseCode)`
* `findByStatusOrderByRescueDateAsc(RescueStatus status)`
* `findByRescueCenterCode(String centerCode)`
* `findByAnimalCode(String animalCode)`
* `findByCommonNameContainingIgnoreCase(String name)`
* `findByRescueCaseStatus(RescueStatus status)`
* `findByRescueCaseRescueCenterCode(String centerCode)`
* `findByNameIgnoreCase(String name)`
* `findByAnimalIdOrderByPerformedAtAsc(Long animalId)`

### Listado de Consultas JPQL Implementadas (@Query)
* **Especialistas activos por experiencia:** `SELECT DISTINCT s FROM Specialist s JOIN s.expertiseAreas e WHERE s.active = true AND LOWER(e.name) = LOWER(:expertise)`
* **Tratamientos en intervalo temporal:** `SELECT t FROM Treatment t WHERE t.performedAt BETWEEN :start AND :end ORDER BY t.performedAt ASC`
* **Tratamientos por centro de rescate:** `SELECT t FROM Treatment t JOIN t.animal a JOIN a.rescueCase rc JOIN rc.rescueCenter center WHERE center.code = :centerCode`
* **Tratamientos por experiencia médica:** `SELECT t FROM Treatment t JOIN t.specialist s JOIN s.expertiseAreas e WHERE LOWER(e.name) = LOWER(:expertise)`
* **Reto Integrador Final:** `SELECT DISTINCT a FROM Animal a JOIN a.rescueCase rc JOIN a.treatments t JOIN t.specialist s JOIN s.expertiseAreas e WHERE rc.status = :status AND LOWER(e.name) = LOWER(:expertise)`