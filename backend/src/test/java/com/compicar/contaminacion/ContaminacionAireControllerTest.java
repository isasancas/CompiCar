package com.compicar.contaminacion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ContaminacionAireControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ContaminacionAireIA contaminacionAireIA;

    @InjectMocks
    private ContaminacionAireController contaminacionAireController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(contaminacionAireController).build();
    }

    @Test
    void obtenerActual_ok_devuelveRespuesta() throws Exception {
        ContaminacionAireIA.ContaminacionRespuesta respuesta = new ContaminacionAireIA.ContaminacionRespuesta(
            "{\"ciudades\":[]}", LocalDateTime.of(2026, 9, 14, 12, 0)
        );

        when(contaminacionAireIA.obtenerContaminacionActual()).thenReturn(respuesta);

        mockMvc.perform(get("/api/contaminacion/actual"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.datos").value("{\"ciudades\":[]}"))
            .andExpect(jsonPath("$.fechaConsulta").exists());

        verify(contaminacionAireIA).obtenerContaminacionActual();
    }
}