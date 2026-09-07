package com.deepblue.rescue;

import com.deepblue.rescue.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import com.deepblue.rescue.domain.*;
import java.time.LocalDate;
import org.springframework.dao.DataIntegrityViolationException;
import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@SpringBootTest
@Transactional
class PersistenceIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("deepblue_test")
            .withUsername("deepblue")
            .withPassword("deepblue");

    // Paso 46: Inyectar repositorios y JdbcTemplate
    @Autowired private RescueCenterRepository rescueCenterRepository;
    @Autowired private RescueCaseRepository rescueCaseRepository;
    @Autowired private AnimalRepository animalRepository;
    @Autowired private SpecialistRepository specialistRepository;
    @Autowired private ExpertiseRepository expertiseRepository;
    @Autowired private TreatmentRepository treatmentRepository;
    
    @Autowired private JdbcTemplate jdbcTemplate;

    // Paso 47: Test de Flyway
    @Test
    void schemaShouldBeCreatedByFlyway() {
        Integer v1Count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE version = '1'", Integer.class);
        Integer v2Count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE version = '2'", Integer.class);

        assertTrue(v1Count != null && v1Count == 1, "Migración V1 falló");
        assertTrue(v2Count != null && v2Count == 1, "Migración V2 falló");
    }

    // Paso 48: Test de métodos heredados
    @Test
    void shouldPerformBasicCrudOperationsOnRescueCenter() {
        // 1. Crear la entidad con los datos del laboratorio
        RescueCenter center = new RescueCenter();
        center.setCode("DB-CAR");
        center.setName("DeepBlue Caribbean Center");
        center.setCity("Santa Marta");

        // 2. Comprobar save()
        rescueCenterRepository.save(center);
        
        // 3. Comprobar count()
        long totalCenters = rescueCenterRepository.count();
        assertTrue(totalCenters > 0, "Debería existir al menos un centro en la base de datos");

        // 4. Comprobar existsById() y findById() usando el ID generado automáticamente
        boolean exists = rescueCenterRepository.existsById(center.getId());
        assertTrue(exists, "El centro guardado debería existir en la base de datos");
    }

    // Paso 49: Test relación 1:N (Un centro tiene muchos casos)
    @Test
    void shouldPersistOneToManyRelationshipBetweenCenterAndCases() {
        RescueCenter center = new RescueCenter();
        center.setCode("DB-PAC");
        center.setName("DeepBlue Pacific");
        center.setCity("Buenaventura");
        rescueCenterRepository.save(center);

        RescueCase case1 = new RescueCase();
        case1.setCaseCode("RES-001");
        case1.setRescueDate(LocalDate.now());
        case1.setStatus(RescueStatus.ADMITTED);
        case1.setRescueCenter(center);

        RescueCase case2 = new RescueCase();
        case2.setCaseCode("RES-002");
        case2.setRescueDate(LocalDate.now());
        case2.setStatus(RescueStatus.ADMITTED);
        case2.setRescueCenter(center);

        rescueCaseRepository.save(case1);
        rescueCaseRepository.save(case2);

        assertTrue(case1.getRescueCenter().getCode().equals("DB-PAC"), "El caso 1 debe pertenecer al centro pacífico");
        assertTrue(case2.getRescueCenter().getCode().equals("DB-PAC"), "El caso 2 debe pertenecer al centro pacífico");
    }

    // Paso 50: Test relación 1:1 (RescueCase 1:1 Animal)
    @Test
    void shouldPersistOneToOneRelationshipBetweenCaseAndAnimal() {
        // Necesitamos un centro primero para no violar la llave foránea (FK) del caso
        RescueCenter center = new RescueCenter();
        center.setCode("DB-ATL");
        center.setName("Atlantic Center");
        center.setCity("Barranquilla");
        rescueCenterRepository.save(center);

        RescueCase rescueCase = new RescueCase();
        rescueCase.setCaseCode("RES-2026-001");
        rescueCase.setRescueDate(LocalDate.now());
        rescueCase.setStatus(RescueStatus.ADMITTED);
        rescueCase.setRescueCenter(center);

        Animal animal = new Animal();
        animal.setAnimalCode("AN-2026-001");
        animal.setCommonName("Green Sea Turtle");
        animal.setScientificName("Chelonia mydas");
        animal.setSex(AnimalSex.FEMALE); 

        // Sincronizamos ambos lados de la relación
        rescueCase.assignAnimal(animal);

        // Al guardar el caso, la cascada (CascadeType.ALL) guardará automáticamente al animal
        rescueCaseRepository.save(rescueCase);

        assertTrue(rescueCase.getAnimal() != null, "El caso debe tener un animal asociado");
        assertTrue(animal.getRescueCase() != null, "El animal debe tener un caso asociado");
    }

    // Paso 51: Test Animal 1:1 MedicalRecord
    @Test
    void shouldPersistMedicalRecordWithAnimalUsingCascade() {
        // Preparar datos base para no violar constraints
        RescueCenter center = new RescueCenter();
        center.setCode("DB-MED");
        center.setName("Medical Center");
        center.setCity("Santa Marta");
        rescueCenterRepository.save(center);

        RescueCase rescueCase = new RescueCase();
        rescueCase.setCaseCode("RES-2026-002");
        rescueCase.setRescueDate(LocalDate.now());
        rescueCase.setStatus(RescueStatus.ADMITTED);
        rescueCase.setRescueCenter(center);

        // Crear el animal AN-2026-002
        Animal animal = new Animal();
        animal.setAnimalCode("AN-2026-002");
        animal.setCommonName("Sea Turtle");
        animal.setSex(AnimalSex.UNKNOWN);
        rescueCase.assignAnimal(animal);
        
        // Crear el expediente médico con los datos exigidos
        MedicalRecord record = new MedicalRecord();
        record.setInitialWeight(new java.math.BigDecimal("28.40"));
        record.setInitialCondition("STABLE");
        record.setInjuries("Left front flipper injury");
        
        // Sincronizar la relación
        animal.assignMedicalRecord(record);

        // La cascada guardará: RescueCase -> Animal -> MedicalRecord
        rescueCaseRepository.save(rescueCase);

        // Validar que ambos obtuvieron ID
        assertTrue(animal.getId() != null, "El animal debe obtener un ID");
        assertTrue(record.getId() != null, "El expediente debe obtener un ID");
    }

    // Paso 52: Test N:M
    @Test
    void shouldPersistManyToManyRelationshipBetweenSpecialistAndExpertise() {
        // Recuperar registros insertados por el script V2 de Flyway
        Expertise trauma = expertiseRepository.findByNameIgnoreCase("Trauma").orElseThrow();
        Expertise rehab = expertiseRepository.findByNameIgnoreCase("Rehabilitation").orElseThrow();

        // Crear a Elena Vargas
        Specialist elena = new Specialist();
        elena.setProfessionalCode("SPEC-001");
        elena.setFirstName("Elena");
        elena.setLastName("Vargas");
        elena.setEmail("elena@deepblue.org");
        elena.setActive(true);

        // Asociar ambas especialidades
        elena.addExpertise(trauma);
        elena.addExpertise(rehab);

        // Guardar especialista y relación en la tabla intermedia
        specialistRepository.save(elena);

        // Comprobar indirectamente consultando la entidad
        Specialist savedElena = specialistRepository.findByProfessionalCode("SPEC-001").orElseThrow();
        assertTrue(savedElena.getExpertiseAreas().size() == 2, "Elena debe tener 2 especialidades");
    }

    // Paso 53: Test Query Method simple
    @Test
    void shouldFindCasesByStatus() {
        RescueCenter center = new RescueCenter();
        center.setCode("DB-TEST-STATUS");
        center.setName("Status Center");
        center.setCity("Santa Marta");
        rescueCenterRepository.save(center);

        // Crear 3 casos con diferentes estados
        RescueCase c1 = new RescueCase();
        c1.setCaseCode("RES-001");
        c1.setRescueDate(LocalDate.now());
        c1.setStatus(RescueStatus.IN_REHABILITATION);
        c1.setRescueCenter(center);

        RescueCase c2 = new RescueCase();
        c2.setCaseCode("RES-002");
        c2.setRescueDate(LocalDate.now());
        c2.setStatus(RescueStatus.READY_FOR_RELEASE);
        c2.setRescueCenter(center);

        RescueCase c3 = new RescueCase();
        c3.setCaseCode("RES-003");
        c3.setRescueDate(LocalDate.now());
        c3.setStatus(RescueStatus.IN_REHABILITATION);
        c3.setRescueCenter(center);

        rescueCaseRepository.saveAll(java.util.List.of(c1, c2, c3));

        // Ejecutar consulta por status
        java.util.List<RescueCase> inRehabCases = rescueCaseRepository.findByStatusOrderByRescueDateAsc(RescueStatus.IN_REHABILITATION);
        
        assertTrue(inRehabCases.size() == 2, "Deberían existir exactamente 2 casos en rehabilitación");
    }

    // Paso 54: Test Query Method navegando relaciones
    @Test
    void shouldFindAnimalsByRescueCenterCode() {
        // Centro 1 (DB-CAR)
        RescueCenter centerCar = new RescueCenter();
        centerCar.setCode("DB-CAR-2");
        centerCar.setName("Caribbean Center 2");
        centerCar.setCity("Santa Marta");
        rescueCenterRepository.save(centerCar);

        RescueCase caseCar = new RescueCase();
        caseCar.setCaseCode("RES-CAR-01");
        caseCar.setRescueDate(LocalDate.now());
        caseCar.setStatus(RescueStatus.ADMITTED);
        caseCar.setRescueCenter(centerCar);

        Animal animalCar = new Animal();
        animalCar.setAnimalCode("AN-CAR-01");
        caseCar.assignAnimal(animalCar);
        rescueCaseRepository.save(caseCar);

        // Centro 2 (DB-PAC)
        RescueCenter centerPac = new RescueCenter();
        centerPac.setCode("DB-PAC-2");
        centerPac.setName("Pacific Center 2");
        centerPac.setCity("Buenaventura");
        rescueCenterRepository.save(centerPac);

        RescueCase casePac = new RescueCase();
        casePac.setCaseCode("RES-PAC-01");
        casePac.setRescueDate(LocalDate.now());
        casePac.setStatus(RescueStatus.ADMITTED);
        casePac.setRescueCenter(centerPac);

        Animal animalPac = new Animal();
        animalPac.setAnimalCode("AN-PAC-01");
        casePac.assignAnimal(animalPac);
        rescueCaseRepository.save(casePac);

        // Ejecutar consulta
        java.util.List<Animal> carAnimals = animalRepository.findByRescueCaseRescueCenterCode("DB-CAR-2");

        assertTrue(carAnimals.size() == 1, "Debería retornar solo 1 animal");
        assertTrue(carAnimals.get(0).getAnimalCode().equals("AN-CAR-01"), "El animal retornado debe pertenecer al centro DB-CAR-2");
    }

    // Paso 55: Test JPQL de especialistas
    @Test
    void shouldFindSpecialistsByExpertise() {
        // Recuperar áreas de experiencia creadas por Flyway
        Expertise trauma = expertiseRepository.findByNameIgnoreCase("Trauma").orElseThrow();
        Expertise rehab = expertiseRepository.findByNameIgnoreCase("Rehabilitation").orElseThrow();
        Expertise mammals = expertiseRepository.findByNameIgnoreCase("Marine Mammals").orElseThrow();
        Expertise birds = expertiseRepository.findByNameIgnoreCase("Marine Birds").orElseThrow();

        // Crear a los 3 especialistas exigidos por el laboratorio
        Specialist elena = new Specialist();
        elena.setProfessionalCode("SPEC-002");
        elena.setFirstName("Elena");
        elena.setLastName("Vargas");
        elena.setEmail("elena.v@deepblue.org");
        elena.setActive(true);
        elena.addExpertise(trauma);
        elena.addExpertise(rehab);
        specialistRepository.save(elena);

        Specialist mateo = new Specialist();
        mateo.setProfessionalCode("SPEC-003");
        mateo.setFirstName("Mateo");
        mateo.setLastName("Gomez");
        mateo.setEmail("mateo@deepblue.org");
        mateo.setActive(true);
        mateo.addExpertise(mammals);
        mateo.addExpertise(rehab);
        specialistRepository.save(mateo);

        Specialist sofia = new Specialist();
        sofia.setProfessionalCode("SPEC-004");
        sofia.setFirstName("Sofia");
        sofia.setLastName("Rojas");
        sofia.setEmail("sofia@deepblue.org");
        sofia.setActive(true);
        sofia.addExpertise(birds);
        sofia.addExpertise(trauma);
        specialistRepository.save(sofia);

        // Ejecutar consulta JPQL
        java.util.List<Specialist> traumaSpecialists = specialistRepository.findActiveByExpertise("Trauma");
        
        assertTrue(traumaSpecialists.size() == 2, "Deberían retornar 2 especialistas (Elena y Sofia)");
        assertTrue(traumaSpecialists.stream().anyMatch(s -> s.getFirstName().equals("Elena")), "Elena debe estar en la lista");
        assertTrue(traumaSpecialists.stream().anyMatch(s -> s.getFirstName().equals("Sofia")), "Sofia debe estar en la lista");
    }

    // Pasos 56 y 57: Crear tratamientos y probar Query Method cronológico
    @Test
    void shouldFindTreatmentsChronologically() {
        // Preparar base
        RescueCenter center = new RescueCenter();
        center.setCode("DB-TRT");
        center.setName("Treatment Center");
        center.setCity("Santa Marta");
        rescueCenterRepository.save(center);

        RescueCase rescueCase = new RescueCase();
        rescueCase.setCaseCode("RES-TRT");
        rescueCase.setRescueDate(java.time.LocalDate.now());
        rescueCase.setStatus(RescueStatus.IN_REHABILITATION);
        rescueCase.setRescueCenter(center);

        Animal animal = new Animal();
        animal.setAnimalCode("AN-TRT");
        rescueCase.assignAnimal(animal);
        rescueCaseRepository.save(rescueCase);

        Specialist elena = new Specialist();
        elena.setProfessionalCode("SPEC-TRT-1");
        elena.setFirstName("Elena");
        elena.setEmail("elenatrt@deepblue.org");
        elena.setActive(true);
        specialistRepository.save(elena);

        Specialist mateo = new Specialist();
        mateo.setProfessionalCode("SPEC-TRT-2");
        mateo.setFirstName("Mateo");
        mateo.setEmail("mateotrt@deepblue.org");
        mateo.setActive(true);
        specialistRepository.save(mateo);

        // Crear 3 tratamientos en desorden cronológico
        Treatment t1 = new Treatment();
        t1.setType(TreatmentType.WOUND_CARE);
        t1.setSpecialist(elena);
        t1.setAnimal(animal);
        t1.setPerformedAt(java.time.LocalDateTime.now().minusDays(2)); // Hace 2 días
        t1.setDescription("Treatment 1");
        
        Treatment t2 = new Treatment();
        t2.setType(TreatmentType.HYDRATION);
        t2.setSpecialist(elena);
        t2.setAnimal(animal);
        t2.setPerformedAt(java.time.LocalDateTime.now().minusDays(1)); // Hace 1 día
        t2.setDescription("Treatment 2");

        Treatment t3 = new Treatment();
        t3.setType(TreatmentType.OBSERVATION);
        t3.setSpecialist(mateo);
        t3.setAnimal(animal);
        t3.setPerformedAt(java.time.LocalDateTime.now()); // Hoy
        t3.setDescription("Treatment 3");

        // Los guardamos en orden aleatorio intencionalmente
        treatmentRepository.saveAll(java.util.List.of(t3, t1, t2)); 

        // Consultar ordenados
        java.util.List<Treatment> treatments = treatmentRepository.findByAnimalIdOrderByPerformedAtAsc(animal.getId());

        assertTrue(treatments.size() == 3, "Deben existir 3 tratamientos");
        assertTrue(treatments.get(0).getDescription().equals("Treatment 1"), "El tratamiento de hace 2 días debe ser el primero");
        assertTrue(treatments.get(2).getDescription().equals("Treatment 3"), "El tratamiento de hoy debe ser el último");
    }

    // Paso 58: Test JPQL por intervalo
    @Test
    void shouldFindTreatmentsBetweenDates() {
        // 1. Preparar dependencias mínimas
        RescueCenter c = new RescueCenter(); c.setCode("DB-INT"); c.setName("C"); c.setCity("C"); rescueCenterRepository.save(c);
        RescueCase rc = new RescueCase(); rc.setCaseCode("RES-INT"); rc.setRescueDate(java.time.LocalDate.now()); rc.setStatus(RescueStatus.ADMITTED); rc.setRescueCenter(c);
        Animal a = new Animal(); a.setAnimalCode("AN-INT"); rc.assignAnimal(a); rescueCaseRepository.save(rc);
        Specialist s = new Specialist(); s.setProfessionalCode("SPEC-INT"); s.setFirstName("S"); s.setEmail("s@d.com"); s.setActive(true); specialistRepository.save(s);

        // 2. Crear tratamientos en las fechas exactas requeridas por el laboratorio
        Treatment t1 = new Treatment(); t1.setType(TreatmentType.WOUND_CARE); t1.setSpecialist(s); t1.setAnimal(a);
        t1.setPerformedAt(java.time.LocalDateTime.of(2026, 8, 1, 10, 0)); t1.setDescription("Agosto 1");
        
        Treatment t2 = new Treatment(); t2.setType(TreatmentType.WOUND_CARE); t2.setSpecialist(s); t2.setAnimal(a);
        t2.setPerformedAt(java.time.LocalDateTime.of(2026, 8, 10, 10, 0)); t2.setDescription("Agosto 10");

        Treatment t3 = new Treatment(); t3.setType(TreatmentType.WOUND_CARE); t3.setSpecialist(s); t3.setAnimal(a);
        t3.setPerformedAt(java.time.LocalDateTime.of(2026, 8, 20, 10, 0)); t3.setDescription("Agosto 20");

        treatmentRepository.saveAll(java.util.List.of(t1, t2, t3));

        // 3. Consultar entre el 5 y el 15 de agosto
        java.time.LocalDateTime start = java.time.LocalDateTime.of(2026, 8, 5, 0, 0);
        java.time.LocalDateTime end = java.time.LocalDateTime.of(2026, 8, 15, 23, 59);
        
        java.util.List<Treatment> results = treatmentRepository.findTreatmentsBetweenDates(start, end);

        assertTrue(results.size() == 1, "Debe retornar un solo tratamiento");
        assertTrue(results.get(0).getDescription().equals("Agosto 10"), "Debe ser el tratamiento del 10 de agosto");
    }

    // Paso 59: Probar UNIQUE constraint
    @Test
    void shouldThrowExceptionWhenAnimalCodeIsDuplicated() {
        RescueCenter center = new RescueCenter(); center.setCode("DB-UNI"); center.setName("U"); center.setCity("U"); rescueCenterRepository.save(center);
        
        RescueCase c1 = new RescueCase(); c1.setCaseCode("RES-U1"); c1.setRescueDate(java.time.LocalDate.now()); c1.setStatus(RescueStatus.ADMITTED); c1.setRescueCenter(center);
        Animal a1 = new Animal(); a1.setAnimalCode("AN-100"); c1.assignAnimal(a1); 
        rescueCaseRepository.saveAndFlush(c1);

        RescueCase c2 = new RescueCase(); c2.setCaseCode("RES-U2"); c2.setRescueDate(java.time.LocalDate.now()); c2.setStatus(RescueStatus.ADMITTED); c2.setRescueCenter(center);
        Animal a2 = new Animal(); a2.setAnimalCode("AN-100"); c2.assignAnimal(a2);

        assertThrows(DataIntegrityViolationException.class, () -> {
            rescueCaseRepository.saveAndFlush(c2);
        }, "Debe lanzar excepción por duplicar AN-100");
    }

    // Paso 60: Probar FK constraint
    @Test
    void shouldThrowExceptionWhenForeignKeyIsViolated() {
        RescueCase rescueCase = new RescueCase();
        rescueCase.setCaseCode("RES-FK");
        rescueCase.setRescueDate(java.time.LocalDate.now());
        rescueCase.setStatus(RescueStatus.ADMITTED);
        
        // Asignamos un centro ficticio que no existe en la base de datos
        RescueCenter fakeCenter = new RescueCenter();
        fakeCenter.setId(9999L);
        rescueCase.setRescueCenter(fakeCenter);

        assertThrows(DataIntegrityViolationException.class, () -> {
            rescueCaseRepository.saveAndFlush(rescueCase);
        }, "Debe lanzar excepción por violar la llave foránea del centro de rescate");
    }

    // PARTE XII: Reto Integrador
    @Test
    void shouldExecuteFullRescueScenario() {
        // 1. Crear el Centro y el Caso
        RescueCenter center = new RescueCenter();
        center.setCode("DB-FINAL");
        center.setName("DeepBlue Final Center");
        center.setCity("Santa Marta");
        rescueCenterRepository.save(center);

        RescueCase rescueCase = new RescueCase();
        rescueCase.setCaseCode("RES-FINAL-01");
        rescueCase.setRescueDate(java.time.LocalDate.now());
        rescueCase.setStatus(RescueStatus.IN_REHABILITATION);
        rescueCase.setRescueCenter(center);

        // 2. Crear el Animal con el nuevo GPS
        Animal turtle = new Animal();
        turtle.setAnimalCode("AN-FINAL-01");
        turtle.setCommonName("Loggerhead Sea Turtle");
        turtle.setScientificName("Caretta caretta");
        turtle.setSex(AnimalSex.FEMALE);
        turtle.setTrackingDeviceCode("GPS-LST-001");
        rescueCase.assignAnimal(turtle);

        // 3. Crear el Expediente Médico
        MedicalRecord record = new MedicalRecord();
        record.setInitialWeight(new java.math.BigDecimal("45.50"));
        record.setInitialCondition("CRITICAL");
        record.setInjuries("Propeller strike on carapace");
        turtle.assignMedicalRecord(record);

        // Guardar la cascada principal (Caso -> Animal -> Record)
        rescueCaseRepository.save(rescueCase);

        // 4. Asignar un Especialista y añadir el Tratamiento
        Expertise trauma = expertiseRepository.findByNameIgnoreCase("Trauma").orElseThrow();
        Specialist specialist = new Specialist();
        specialist.setProfessionalCode("SPEC-FINAL");
        specialist.setFirstName("Laura");
        specialist.setLastName("Martinez");
        specialist.setEmail("laura@deepblue.org");
        specialist.setActive(true);
        specialist.addExpertise(trauma);
        specialistRepository.save(specialist);

        Treatment t1 = new Treatment();
        t1.setType(TreatmentType.WOUND_CARE);
        t1.setSpecialist(specialist);
        t1.setAnimal(turtle);
        t1.setPerformedAt(java.time.LocalDateTime.now());
        t1.setDescription("Carapace cleaning and epoxy repair");
        treatmentRepository.save(t1);

        // 5. Validar la integridad de toda la arquitectura
        assertTrue(animalRepository.findByAnimalCode("AN-FINAL-01").isPresent(), "El animal debe existir");
        assertTrue(rescueCaseRepository.findByStatusOrderByRescueDateAsc(RescueStatus.IN_REHABILITATION).size() > 0, "Debe haber casos en rehabilitación");
        assertTrue(treatmentRepository.findByAnimalIdOrderByPerformedAtAsc(turtle.getId()).size() == 1, "Debe existir 1 tratamiento registrado");
    }
}