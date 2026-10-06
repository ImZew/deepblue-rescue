package com.deepblue.rescue.controller;

import com.deepblue.rescue.dto.request.CreateTreatmentRequest;
import com.deepblue.rescue.dto.response.TreatmentResponse;
import com.deepblue.rescue.exception.GlobalExceptionHandler;
import com.deepblue.rescue.service.TreatmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TreatmentController.class)
@Import(GlobalExceptionHandler.class)
class TreatmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TreatmentService service;

    @Test
    void shouldCreateTreatment() throws Exception {
        mockMvc.perform(post("/api/treatments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "animalCode": "AN-001",
                            "specialistCode": "SPEC-001",
                            "performedAt": "2026-08-21T09:00:00",
                            "type": "WOUND_CARE",
                            "description": "Cleaning and treatment of flipper injury."
                        }
                        """))
                .andExpect(status().isCreated());

        verify(service).register(any(CreateTreatmentRequest.class));
    }

    @Test
    void shouldReturn400WhenTreatmentRequestIsInvalid() throws Exception {
        mockMvc.perform(post("/api/treatments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "animalCode": "",
                            "specialistCode": ""
                        }
                        """))
                .andExpect(status().isBadRequest());

        verify(service, never()).register(any());
    }
}