package com.compicar.contaminacion;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.compicar.viaje.CalculoPrecioIA;

@Component
public class ContaminacionAireIA {

  private static final long MAXIMA_ANTIGUEDAD_DIAS = 7;
    private static final Logger logger = LoggerFactory.getLogger(ContaminacionAireIA.class);

    private static final String PROMPT_CONTAMINACION = """
        Necesito la contaminacion del aire mas reciente disponible para las cinco grandes ciudades de Espana:
        Madrid, Barcelona, Valencia, Sevilla y Zaragoza.

        Consulta fuentes actuales y fiables si tienes acceso a ellas. Si no puedes verificar los datos en tiempo real,
        indicalo claramente en el campo fuente y no inventes valores. Usa como momento de referencia: %s.

        Responde exclusivamente con un objeto JSON valido, sin Markdown ni texto adicional, con esta estructura:
        {
          "momentoConsulta": "fecha y hora ISO-8601",
          "fuente": "fuente de los datos o indicacion de que no se pudo verificar en tiempo real",
          "ciudades": [
            {
              "ciudad": "Madrid",
              "indiceCalidadAire": "valor o no disponible",
              "categoria": "buena, moderada, mala o no disponible",
              "contaminantes": {
                "pm25": "valor y unidad o no disponible",
                "pm10": "valor y unidad o no disponible",
                "no2": "valor y unidad o no disponible",
                "o3": "valor y unidad o no disponible"
              }
            }
          ]
        }

        Incluye exactamente una entrada por ciudad y conserva las unidades originales de cada fuente.
        """;

    private final CalculoPrecioIA calculoPrecioIA;
    private final ObjectMapper objectMapper;
    private final Path archivoCache;
    private volatile ContaminacionRespuesta ultimaRespuesta;

    public ContaminacionAireIA(
        CalculoPrecioIA calculoPrecioIA,
        @Value("${contaminacion.aire.cache-file:data/contaminacion-aire.json}") String archivoCache) {
        this.calculoPrecioIA = calculoPrecioIA;
      this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
      this.archivoCache = Path.of(archivoCache);
      cargarCache();
    }

    public synchronized ContaminacionRespuesta obtenerContaminacionActual() {
      if (ultimaRespuesta != null && !necesitaActualizar(ultimaRespuesta)) {
        return ultimaRespuesta;
      }

      try {
        obtenerContaminacionSemanal();
      } catch (RuntimeException exception) {
        if (ultimaRespuesta != null) {
          logger.warn("Gemini no disponible; se devuelve la ultima contaminacion guardada", exception);
          return ultimaRespuesta;
        }
        throw exception;
      }

      return ultimaRespuesta;
    }

    private String obtenerContaminacionSemanal() {
      String prompt = PROMPT_CONTAMINACION.formatted(LocalDateTime.now());
      String respuesta = calculoPrecioIA.pedirEstimacionJson(prompt);
      ContaminacionRespuesta nuevaRespuesta = new ContaminacionRespuesta(respuesta, LocalDateTime.now());
      guardarCache(nuevaRespuesta);
      ultimaRespuesta = nuevaRespuesta;
      return respuesta;
    }

    public ContaminacionRespuesta obtenerUltimaRespuesta() {
      return ultimaRespuesta;
    }

    private boolean necesitaActualizar(ContaminacionRespuesta respuesta) {
      return respuesta.fechaConsulta().plusDays(MAXIMA_ANTIGUEDAD_DIAS).isBefore(LocalDateTime.now());
    }

    private void cargarCache() {
      if (!Files.exists(archivoCache)) {
        return;
      }

      try {
        ultimaRespuesta = objectMapper.readValue(archivoCache.toFile(), ContaminacionRespuesta.class);
      } catch (IOException | RuntimeException exception) {
        logger.warn("No se pudo cargar la cache de contaminacion; se solicitara un dato nuevo", exception);
      }
    }

    private void guardarCache(ContaminacionRespuesta respuesta) {
      try {
        Path parent = archivoCache.getParent();
        if (parent != null) {
          Files.createDirectories(parent);
        }

        Path archivoTemporal = archivoCache.resolveSibling(archivoCache.getFileName() + ".tmp");
        objectMapper.writeValue(archivoTemporal.toFile(), respuesta);
        Files.move(archivoTemporal, archivoCache,
            StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
      } catch (IOException exception) {
        logger.warn("No se pudo guardar la cache de contaminacion; se conserva en memoria", exception);
      }
    }

    public record ContaminacionRespuesta(String datos, LocalDateTime fechaConsulta) {
    }
}