package com.deepblue.rescue.repository;

import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate; // Import vital para que reconozca la fecha

@Repository
public interface RescueCaseRepository extends JpaRepository<RescueCase, Long> {

    // Paso 32 (Consulta A): Buscar un caso por su código
    Optional<RescueCase> findByCaseCode(String caseCode);

    // Paso 32 (Consulta B): Buscar por estado y ordenar por fecha de rescate ascendente
    List<RescueCase> findByStatusOrderByRescueDateAsc(RescueStatus status);

    // Paso 32 (Consulta C) y 33: Buscar navegando la relación hacia RescueCenter
    List<RescueCase> findByRescueCenterCode(String code);

    // Paso 37: Casos de rescate posteriores a una fecha, del más reciente al más antiguo
    List<RescueCase> findByRescueDateAfterOrderByRescueDateDesc(LocalDate date);
}