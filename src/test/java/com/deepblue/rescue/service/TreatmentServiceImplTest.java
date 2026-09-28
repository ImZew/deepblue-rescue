package com.deepblue.rescue.service;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.domain.Specialist;
import com.deepblue.rescue.domain.Treatment;
import com.deepblue.rescue.domain.TreatmentType;
import com.deepblue.rescue.dto.request.CreateTreatmentRequest;
import com.deepblue.rescue.dto.response.TreatmentResponse;
import com.deepblue.rescue.exception.BusinessRuleException;
import com.deepblue.rescue.mapper.TreatmentMapper;
import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.repository.SpecialistRepository;
import com.deepblue.rescue.repository.TreatmentRepository;
import com.deepblue.rescue.service.impl.TreatmentServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TreatmentServiceImplTest {

    @Mock
    private AnimalRepository animalRepository;

    @Mock
    private SpecialistRepository specialistRepository;

    @Mock
    private TreatmentRepository treatmentRepository;

    @Mock
    private TreatmentMapper mapper;

    @InjectMocks
    private TreatmentServiceImpl service;

    // TEST 5 (Pasos 32 y 51): Registro de tratamiento válido
    @Test
    void shouldRegisterTreatmentSuccessfully() {
        RescueCase rescueCase = new RescueCase();
        rescueCase.setCaseCode("RES-2026-100");
        rescueCase.setStatus(RescueStatus.IN_REHABILITATION);
        rescueCase.setRescueDate(LocalDate.of(2026, 8, 20));

        Animal animal = new Animal();
        animal.setAnimalCode("AN-2026-100");
        animal.setCommonName("Green Sea Turtle");
        animal.setRescueCase(rescueCase);

        Specialist specialist = new Specialist();
        specialist.setProfessionalCode("SPEC-001");
        specialist.setActive(true);

        LocalDateTime performedAt = LocalDateTime.of(2026, 8, 21, 9, 0);

        CreateTreatmentRequest request = new CreateTreatmentRequest(
                "AN-2026-100",
                "SPEC-001",
                performedAt,
                TreatmentType.WOUND_CARE,
                "Cleaning of left front flipper injury."
        );

        Treatment savedTreatment = new Treatment(
                animal,
                specialist,
                performedAt,
                TreatmentType.WOUND_CARE,
                "Cleaning of left front flipper injury."
        );

        TreatmentResponse expectedResponse = new TreatmentResponse(
                1L,
                "AN-2026-100",
                "SPEC-001",
                performedAt,
                TreatmentType.WOUND_CARE,
                "Cleaning of left front flipper injury."
        );

        when(animalRepository.findByAnimalCode("AN-2026-100"))
                .thenReturn(Optional.of(animal));
        when(specialistRepository.findByProfessionalCode("SPEC-001"))
                .thenReturn(Optional.of(specialist));
        when(treatmentRepository.save(any(Treatment.class)))
                .thenReturn(savedTreatment);
        when(mapper.toResponse(savedTreatment))
                .thenReturn(expectedResponse);

        TreatmentResponse result = service.register(request);

        assertThat(result).isEqualTo(expectedResponse);
        verify(treatmentRepository).save(any(Treatment.class));
    }

    // TEST 6 (Pasos 33 y 54): Especialista inactivo -> BusinessRuleException
    @Test
    void shouldThrowExceptionWhenSpecialistIsInactive() {
        RescueCase rescueCase = new RescueCase();
        rescueCase.setStatus(RescueStatus.IN_REHABILITATION);
        rescueCase.setRescueDate(LocalDate.of(2026, 8, 20));

        Animal animal = new Animal();
        animal.setAnimalCode("AN-001");
        animal.setRescueCase(rescueCase);

        Specialist specialist = new Specialist();
        specialist.setProfessionalCode("SPEC-001");
        specialist.setActive(false);

        CreateTreatmentRequest request = new CreateTreatmentRequest(
                "AN-001",
                "SPEC-001",
                LocalDateTime.of(2026, 8, 21, 9, 0),
                TreatmentType.WOUND_CARE,
                "Cleaning injury"
        );

        when(animalRepository.findByAnimalCode("AN-001"))
                .thenReturn(Optional.of(animal));
        when(specialistRepository.findByProfessionalCode("SPEC-001"))
                .thenReturn(Optional.of(specialist));

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(BusinessRuleException.class);

        verify(treatmentRepository, never()).save(any());
    }

    // TEST 7 (Pasos 34 y 53): Animal en caso RELEASED -> BusinessRuleException
    @Test
    void shouldThrowExceptionWhenAnimalAlreadyReleased() {
        RescueCase rescueCase = new RescueCase();
        rescueCase.setStatus(RescueStatus.RELEASED);
        rescueCase.setRescueDate(LocalDate.of(2026, 8, 20));

        Animal animal = new Animal();
        animal.setAnimalCode("AN-001");
        animal.setRescueCase(rescueCase);

        Specialist specialist = new Specialist();
        specialist.setProfessionalCode("SPEC-001");
        specialist.setActive(true);

        CreateTreatmentRequest request = new CreateTreatmentRequest(
                "AN-001",
                "SPEC-001",
                LocalDateTime.of(2026, 8, 21, 9, 0),
                TreatmentType.OBSERVATION,
                "Post-release observation"
        );

        when(animalRepository.findByAnimalCode("AN-001"))
                .thenReturn(Optional.of(animal));
        when(specialistRepository.findByProfessionalCode("SPEC-001"))
                .thenReturn(Optional.of(specialist));

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(BusinessRuleException.class);

        verify(treatmentRepository, never()).save(any());
    }

    // TEST ADICIONAL (Paso 52): Fecha de tratamiento anterior a fecha de rescate
    @Test
    void shouldThrowExceptionWhenTreatmentDateIsBeforeRescueDate() {
        RescueCase rescueCase = new RescueCase();
        rescueCase.setStatus(RescueStatus.IN_REHABILITATION);
        rescueCase.setRescueDate(LocalDate.of(2026, 8, 20));

        Animal animal = new Animal();
        animal.setAnimalCode("AN-2026-100");
        animal.setRescueCase(rescueCase);

        Specialist specialist = new Specialist();
        specialist.setProfessionalCode("SPEC-001");
        specialist.setActive(true);

        CreateTreatmentRequest request = new CreateTreatmentRequest(
                "AN-2026-100",
                "SPEC-001",
                LocalDateTime.of(2026, 8, 15, 10, 0),
                TreatmentType.WOUND_CARE,
                "Invalid date treatment"
        );

        when(animalRepository.findByAnimalCode("AN-2026-100"))
                .thenReturn(Optional.of(animal));
        when(specialistRepository.findByProfessionalCode("SPEC-001"))
                .thenReturn(Optional.of(specialist));

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(BusinessRuleException.class);

        verify(treatmentRepository, never()).save(any());
    }
}