package com.deepblue.rescue.repository;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.RescueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnimalRepository extends JpaRepository<Animal, Long> {

    // Paso 34 (Consulta A): Buscar animal por animalCode
    Optional<Animal> findByAnimalCode(String animalCode);

    // Paso 34 (Consulta B): Buscar por nombre común conteniendo texto (ignora mayúsculas/minúsculas)
    List<Animal> findByCommonNameContainingIgnoreCase(String commonName);

    // Paso 35: Navegar una relación (Animal -> RescueCase -> status)
    List<Animal> findByRescueCaseStatus(RescueStatus status);

    // Paso 36: Navegar varias relaciones (Animal -> RescueCase -> RescueCenter -> code)
    List<Animal> findByRescueCaseRescueCenterCode(String centerCode);
}