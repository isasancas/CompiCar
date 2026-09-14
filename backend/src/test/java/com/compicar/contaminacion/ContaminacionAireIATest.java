package com.compicar.contaminacion;

import com.compicar.viaje.CalculoPrecioIA;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContaminacionAireIATest {

    @Mock
    private CalculoPrecioIA calculoPrecioIA;

    @TempDir
    Path tempDir;

    private String cacheFilePath;
    private ContaminacionAireIA contaminacionAireIA;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private static final String JSON_VALIDO = """
        {
          "momentoConsulta": "2026-09-14T12:00:00",
          "fuente": "AEMET",
          "ciudades": [
            {
              "ciudad": "Madrid",
              "indiceCalidadAire": "Buena",
              "categoria": "buena",
              "contaminantes": {
                "pm25": "12 ug/m3",
                "pm10": "20 ug/m3"
              }
            }
          ]
        }
        """;

    private static final String JSON_MARKDOWN = "```json\n" + JSON_VALIDO + "\n```";

    private static final String JSON_SIN_DATOS = """
        {
          "momentoConsulta": "2026-09-14T12:00:00",
          "fuente": "No disponible",
          "ciudades": [
            {
              "ciudad": "Madrid",
              "indiceCalidadAire": "no disponible",
              "categoria": "no disponible",
              "contaminantes": {
                "pm25": "no disponible"
              }
            }
          ]
        }
        """;

    @BeforeEach
    void setUp() {
        cacheFilePath = tempDir.resolve("contaminacion-cache.json").toString();
        contaminacionAireIA = new ContaminacionAireIA(calculoPrecioIA, cacheFilePath);
    }

    @Test
    void obtenerContaminacionActual_sinCache_consultaIAYGuarda() {
        when(calculoPrecioIA.pedirEstimacionJsonConBusqueda(anyString())).thenReturn(JSON_VALIDO);

        ContaminacionAireIA.ContaminacionRespuesta result = contaminacionAireIA.obtenerContaminacionActual();

        assertNotNull(result);
        assertEquals(JSON_VALIDO, result.datos());
        assertNotNull(result.fechaConsulta());
        verify(calculoPrecioIA).pedirEstimacionJsonConBusqueda(anyString());
    }

    @Test
    void obtenerContaminacionActual_respuestaMarkdown_normalizaExitosamente() {
        when(calculoPrecioIA.pedirEstimacionJsonConBusqueda(anyString())).thenReturn(JSON_MARKDOWN);

        ContaminacionAireIA.ContaminacionRespuesta result = contaminacionAireIA.obtenerContaminacionActual();

        assertNotNull(result);
        assertEquals(JSON_MARKDOWN, result.datos());
        verify(calculoPrecioIA).pedirEstimacionJsonConBusqueda(anyString());
    }

    @Test
    void obtenerContaminacionActual_cacheValido_devuelveSinConsultarIA() {
        when(calculoPrecioIA.pedirEstimacionJsonConBusqueda(anyString())).thenReturn(JSON_VALIDO);
        contaminacionAireIA.obtenerContaminacionActual();
        reset(calculoPrecioIA);

        ContaminacionAireIA.ContaminacionRespuesta result = contaminacionAireIA.obtenerContaminacionActual();

        assertNotNull(result);
        assertEquals(JSON_VALIDO, result.datos());
        verifyNoInteractions(calculoPrecioIA);
    }

    @Test
    void obtenerContaminacionActual_respuestaSinDatosUtiles_lanzaExcepcion() {
        when(calculoPrecioIA.pedirEstimacionJsonConBusqueda(anyString())).thenReturn(JSON_SIN_DATOS);

        assertThrows(IllegalStateException.class, () -> contaminacionAireIA.obtenerContaminacionActual());
        verify(calculoPrecioIA).pedirEstimacionJsonConBusqueda(anyString());
    }

    @Test
    void obtenerContaminacionActual_errorIAConCacheExistente_retornaCachePrevio() {
        when(calculoPrecioIA.pedirEstimacionJsonConBusqueda(anyString())).thenReturn(JSON_VALIDO);
        contaminacionAireIA.obtenerContaminacionActual();

        // Forzar expiración de cache para provocar re-consulta
        ContaminacionAireIA.ContaminacionRespuesta expirada = new ContaminacionAireIA.ContaminacionRespuesta(
            JSON_VALIDO, LocalDateTime.now().minusDays(10)
        );
        ReflectionTestUtils.setField(contaminacionAireIA, "ultimaRespuesta", expirada);

        when(calculoPrecioIA.pedirEstimacionJsonConBusqueda(anyString())).thenThrow(new RuntimeException("API indisponible"));

        ContaminacionAireIA.ContaminacionRespuesta result = contaminacionAireIA.obtenerContaminacionActual();

        assertNotNull(result);
        assertEquals(expirada, result);
        verify(calculoPrecioIA, times(2)).pedirEstimacionJsonConBusqueda(anyString());
    }

    @Test
    void obtenerContaminacionActual_errorIASinCache_lanzaExcepcion() {
        when(calculoPrecioIA.pedirEstimacionJsonConBusqueda(anyString())).thenThrow(new RuntimeException("API indisponible"));

        assertThrows(RuntimeException.class, () -> contaminacionAireIA.obtenerContaminacionActual());
    }

    @Test
    void cargarCache_alInicializarConArchivoExistente_cargaRespuesta() throws Exception {
        ContaminacionAireIA.ContaminacionRespuesta guardada = new ContaminacionAireIA.ContaminacionRespuesta(
            JSON_VALIDO, LocalDateTime.now()
        );
        objectMapper.writeValue(new File(cacheFilePath), guardada);

        ContaminacionAireIA iaConCache = new ContaminacionAireIA(calculoPrecioIA, cacheFilePath);
        ContaminacionAireIA.ContaminacionRespuesta result = iaConCache.obtenerUltimaRespuesta();

        assertNotNull(result);
        assertEquals(JSON_VALIDO, result.datos());
    }
}
