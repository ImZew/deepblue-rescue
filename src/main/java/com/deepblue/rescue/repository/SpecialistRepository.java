package com.deepblue.rescue.repository;

import com.deepblue.rescue.domain.Specialist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
@Repository
public interface SpecialistRepository extends JpaRepository<Specialist, Long> {
    
    // Búsqueda personalizada 1: Buscar por su código profesional único
    Optional<Specialist> findByProfessionalCode(String professionalCode);
    
    // Búsqueda personalizada 2: Traer una lista solo con los que están activos
    List<Specialist> findByActiveTrue();

    @Query("""
        SELECT DISTINCT s
        FROM Specialist s
        JOIN s.expertiseAreas e
        WHERE s.active = true 
        AND LOWER(e.name) = LOWER(:expertiseName)
        ORDER BY s.lastName ASC, s.firstName ASC
        """)
    List<Specialist> findActiveByExpertise(@Param("expertiseName") String expertiseName);
}