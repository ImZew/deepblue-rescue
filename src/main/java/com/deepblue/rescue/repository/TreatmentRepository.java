package com.deepblue.rescue.repository;

import com.deepblue.rescue.domain.Treatment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TreatmentRepository extends JpaRepository<Treatment, Long> {

    // Paso 41: Query Method (Tratamientos de un animal ordenados cronológicamente)
    List<Treatment> findByAnimalIdOrderByPerformedAtAsc(Long animalId);

    // Paso 42: JPQL (Tratamientos realizados entre dos fechas)
    @Query("""
        SELECT t FROM Treatment t 
        WHERE t.performedAt BETWEEN :start AND :end 
        ORDER BY t.performedAt ASC
        """)
    List<Treatment> findTreatmentsBetweenDates(
        @Param("start") LocalDateTime start, 
        @Param("end") LocalDateTime end
    );

    // Paso 43: JPQL navegando tres entidades (Tratamientos pertenecientes a un centro)
    @Query("""
        SELECT t FROM Treatment t 
        JOIN t.animal a 
        JOIN a.rescueCase rc 
        JOIN rc.rescueCenter center 
        WHERE center.code = :centerCode
        """)
    List<Treatment> findByRescueCenterCode(@Param("centerCode") String centerCode);

    // Paso 44: JPQL con N:M (Tratamientos según la experiencia del especialista)
    @Query("""
        SELECT DISTINCT t FROM Treatment t 
        JOIN t.specialist s 
        JOIN s.expertiseAreas e 
        WHERE e.name = :expertiseName
        """)
    List<Treatment> findBySpecialistExpertise(@Param("expertiseName") String expertiseName);

    List<Treatment> findByAnimalAnimalCodeOrderByPerformedAtAsc(String animalCode);
}