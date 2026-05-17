package edu.comillas.icai.gitt.pat.spring.PracticaFinal.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.Map;

/**
 * Controlador REST que expone un endpoint de comprobación de salud del servicio.
 *
 * <p>Es una práctica común en sistemas en producción tener un endpoint {@code /health}
 * que devuelva el estado del servidor. Así, herramientas de monitorización (Kubernetes,
 * AWS ALB, etc.) pueden verificar que la aplicación está funcionando sin necesidad de
 * autenticación.</p>
 *
 * <p>Endpoint disponible: {@code GET /pistaPadel/health}</p>
 * <p>Respuesta ejemplo: {@code {"status": "UP", "database": "CONNECTED"}}</p>
 */
@RestController
@RequestMapping("/pistaPadel")
public class HealthController {

    /**
     * Devuelve el estado actual de la aplicación y la base de datos.
     * Siempre devuelve HTTP 200 con un cuerpo JSON indicando que el servicio está operativo.
     *
     * @return mapa con claves "status" y "database" y sus valores actuales
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> status = new HashMap<>();
        status.put("status", "UP");
        status.put("database", "CONNECTED");
        return ResponseEntity.ok(status);
    }
}

