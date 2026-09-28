package com.deepblue.rescue.service;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.mapper.AnimalMapper;
import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.service.impl.AnimalServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnimalServiceImplTest {

    @Mock
    private AnimalRepository repository;

    @Mock
    private AnimalMapper mapper;

    @InjectMocks
    private AnimalServiceImpl service;

    @Test
    void shouldReturnTrueWhenAnimalIsInRehabilitation() {
        RescueCase rescueCase = new RescueCase();
        rescueCase.setStatus(RescueStatus.IN_REHABILITATION);

        Animal animal = new Animal();
        animal.setAnimalCode("AN-001");
        animal.setRescueCase(rescueCase);

        when(repository.findByAnimalCode("AN-001"))
                .thenReturn(Optional.of(animal));

        boolean result = service.canReceiveTreatment("AN-001");

        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnFalseWhenAnimalIsReleased() {
        RescueCase rescueCase = new RescueCase();
        rescueCase.setStatus(RescueStatus.RELEASED);

        Animal animal = new Animal();
        animal.setAnimalCode("AN-001");
        animal.setRescueCase(rescueCase);

        when(repository.findByAnimalCode("AN-001"))
                .thenReturn(Optional.of(animal));

        boolean result = service.canReceiveTreatment("AN-001");

        assertThat(result).isFalse();
    }
}