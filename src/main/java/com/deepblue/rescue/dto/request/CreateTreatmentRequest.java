package com.deepblue.rescue.dto.request;

import com.deepblue.rescue.domain.TreatmentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record CreateTreatmentRequest(
        @NotBlank(message = "El código del animal es obligatorio")
        String animalCode,
        
        @NotBlank(message = "El código del especialista es obligatorio")
        String specialistCode,
        
        @NotNull(message = "La fecha del tratamiento es obligatoria")
        LocalDateTime performedAt,
        
        @NotNull(message = "El tipo de tratamiento es obligatorio")
        TreatmentType type,
        
        @NotBlank(message = "La descripción es obligatoria")
        String description
) {
}