package com.compicar.valoracion;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.compicar.valoracion.dto.ValoracionDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@ExtendWith(MockitoExtension.class)
class ValoracionControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private ValoracionService valoracionService;

    @InjectMocks
    private ValoracionController valoracionController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(valoracionController).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void crearValoracion_ok() throws Exception {
        ValoracionDTO peticion = new ValoracionDTO();
        peticion.setPuntuacion(5);
        peticion.setAutorId(1L);
        peticion.setValoradoId(2L);
        peticion.setViajeId(10L);

        ValoracionDTO respuesta = new ValoracionDTO();
        respuesta.setId(100L);
        respuesta.setPuntuacion(5);

        when(valoracionService.crearValoracion(any(ValoracionDTO.class))).thenReturn(respuesta);

        mockMvc.perform(post("/api/valoraciones")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(peticion)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100L))
                .andExpect(jsonPath("$.puntuacion").value(5));
    }

    @Test
    void actualizarValoracion_ok() throws Exception {
        ValoracionDTO peticion = new ValoracionDTO();
        peticion.setPuntuacion(4);
        peticion.setComentario("Buen viaje");
        peticion.setAutorId(1L);
        peticion.setValoradoId(2L);
        peticion.setViajeId(10L);

        ValoracionDTO respuesta = new ValoracionDTO();
        respuesta.setId(100L);
        respuesta.setPuntuacion(4);
        respuesta.setComentario("Buen viaje");

        when(valoracionService.actualizarValoracion(eq(100L), any(ValoracionDTO.class))).thenReturn(respuesta);

        mockMvc.perform(put("/api/valoraciones/100")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(peticion)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100L))
                .andExpect(jsonPath("$.puntuacion").value(4));
    }

    @Test
    void eliminarValoracion_ok() throws Exception {
        doNothing().when(valoracionService).eliminarValoracion(100L);

        mockMvc.perform(delete("/api/valoraciones/100"))
                .andExpect(status().isNoContent());
    }

    @Test
    void obtenerValoracionPorId_existe_devuelve200() throws Exception {
        ValoracionDTO respuesta = new ValoracionDTO();
        respuesta.setId(100L);

        when(valoracionService.encontrarPorId(100L)).thenReturn(Optional.of(respuesta));

        mockMvc.perform(get("/api/valoraciones/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100L));
    }

    @Test
    void obtenerValoracionPorId_noExiste_devuelve404() throws Exception {
        when(valoracionService.encontrarPorId(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/valoraciones/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void obtenerValoracionesPorAutor_ok() throws Exception {
        ValoracionDTO v1 = new ValoracionDTO();
        v1.setId(1L);
        
        when(valoracionService.encontrarPorAutor(1L)).thenReturn(List.of(v1));

        mockMvc.perform(get("/api/valoraciones/autor/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L));
    }

    @Test
    void obtenerValoracionesPorValorado_ok() throws Exception {
        ValoracionDTO v1 = new ValoracionDTO();
        v1.setId(2L);
        
        when(valoracionService.encontrarPorValorado(2L)).thenReturn(List.of(v1));

        mockMvc.perform(get("/api/valoraciones/valorado/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2L));
    }

    @Test
    void calcularReputacion_ok() throws Exception {
        when(valoracionService.calcularReputacion(2L)).thenReturn(4.8);

        mockMvc.perform(get("/api/valoraciones/reputacion/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(4.8));
    }
}