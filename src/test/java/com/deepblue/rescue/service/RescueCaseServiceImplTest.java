package com.deepblue.rescue.service;

import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.dto.request.ChangeRescueStatusRequest;
import com.deepblue.rescue.dto.response.RescueCaseResponse;
import com.deepblue.rescue.exception.BusinessRuleException;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.mapper.RescueCaseMapper;
import com.deepblue.rescue.repository.RescueCaseRepository;
import com.deepblue.rescue.service.impl.RescueCaseServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RescueCaseServiceImplTest {

    @Mock
    private RescueCaseRepository repository;

    @Mock
    private RescueCaseMapper mapper;

    @InjectMocks
    private RescueCaseServiceImpl service;

    // TEST 1 (Paso 28): Caso encontrado -> retorna DTO
    @Test
    void shouldFindRescueCaseByCode() {
        RescueCase rescueCase = new RescueCase();
        rescueCase.setCaseCode("RES-001");
        rescueCase.setStatus(RescueStatus.ADMITTED);

        RescueCaseResponse response = new RescueCaseResponse(
                1L,
                "RES-001",
                LocalDate.of(2026, 8, 20),
                "Taganga Bay",
                RescueStatus.ADMITTED,
                "RC-01",
                "AN-001"
        );

        when(repository.findByCaseCode("RES-001"))
                .thenReturn(Optional.of(rescueCase));
        when(mapper.toResponse(rescueCase))
                .thenReturn(response);

        RescueCaseResponse result = service.findByCode("RES-001");

        assertThat(result).isEqualTo(response);
        verify(repository).findByCaseCode("RES-001");
        verify(mapper).toResponse(rescueCase);
    }

    // TEST 2 (Paso 29): Recurso inexistente -> ResourceNotFoundException
    @Test
    void shouldThrowExceptionWhenRescueCaseNotFound() {
        when(repository.findByCaseCode("RES-999"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByCode("RES-999"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(repository).findByCaseCode("RES-999");
        verify(mapper, never()).toResponse(any());
    }

    // TEST 3 (Paso 30): Transición válida (ADMITTED -> UNDER_EVALUATION)
    @Test
    void shouldChangeStatusWhenTransitionIsValid() {
        RescueCase rescueCase = new RescueCase();
        rescueCase.setCaseCode("RES-001");
        rescueCase.setStatus(RescueStatus.ADMITTED);

        ChangeRescueStatusRequest request =
                new ChangeRescueStatusRequest(RescueStatus.UNDER_EVALUATION);

        RescueCaseResponse response = new RescueCaseResponse(
                1L,
                "RES-001",
                LocalDate.of(2026, 8, 20),
                "Taganga Bay",
                RescueStatus.UNDER_EVALUATION,
                "RC-01",
                "AN-001"
        );

        when(repository.findByCaseCode("RES-001"))
                .thenReturn(Optional.of(rescueCase));
        when(repository.save(rescueCase))
                .thenReturn(rescueCase);
        when(mapper.toResponse(rescueCase))
                .thenReturn(response);

        RescueCaseResponse result = service.changeStatus("RES-001", request);

        assertThat(result.status()).isEqualTo(RescueStatus.UNDER_EVALUATION);
        assertThat(rescueCase.getStatus()).isEqualTo(RescueStatus.UNDER_EVALUATION);
        verify(repository).save(rescueCase);
    }

    // TEST 4 (Paso 31): Transición inválida (ADMITTED -> READY_FOR_RELEASE)
    @Test
    void shouldThrowExceptionWhenTransitionIsInvalid() {
        RescueCase rescueCase = new RescueCase();
        rescueCase.setCaseCode("RES-001");
        rescueCase.setStatus(RescueStatus.ADMITTED);

        ChangeRescueStatusRequest request =
                new ChangeRescueStatusRequest(RescueStatus.READY_FOR_RELEASE);

        when(repository.findByCaseCode("RES-001"))
                .thenReturn(Optional.of(rescueCase));

        assertThatThrownBy(() -> service.changeStatus("RES-001", request))
                .isInstanceOf(BusinessRuleException.class);

        verify(repository, never()).save(any());
    }
}