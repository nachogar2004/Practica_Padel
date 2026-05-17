package edu.comillas.icai.gitt.pat.spring.PracticaFinal.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/**
 * Manejador global de excepciones para todos los controladores REST.
 *
 * <p>{@code @RestControllerAdvice} convierte esta clase en un interceptor de excepciones
 * que se aplica a todos los {@code @RestController} del proyecto. Así se centraliza
 * el manejo de errores en un solo lugar, en lugar de poner try-catch en cada endpoint.</p>
 *
 * <h3>Jerarquía de manejadores</h3>
 * <ol>
 *   <li>{@link ResponseStatusException}: excepción estándar de Spring con código HTTP
 *       incorporado (p.ej. 404, 403, 409). Se devuelve el mismo código sin cuerpo.</li>
 *   <li>{@link NotFoundException}: excepción personalizada para recursos no encontrados.
 *       Devuelve 404 con el mensaje descriptivo en el cuerpo.</li>
 *   <li>{@code Exception}: cualquier otro error inesperado. Devuelve 500 con un mensaje
 *       genérico para no filtrar información interna (trazas de pila, nombres de clases).</li>
 * </ol>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Captura los errores de validación de campos ({@code @Valid}).
     * Devuelve HTTP 400 con el mensaje del primer campo inválido.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<String> handleValidationException(MethodArgumentNotValidException ex) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .findFirst()
                .orElse("Petición inválida");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mensaje);
    }

    /**
     * Captura las excepciones estándar de Spring ({@link ResponseStatusException}).
     * Devuelve el mismo código HTTP que lleva la excepción, sin cuerpo de respuesta.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Void> handleResponseStatusException(ResponseStatusException ex) {
        return new ResponseEntity<>(ex.getStatusCode());
    }

    /**
     * Captura la excepción personalizada {@link NotFoundException}.
     * Devuelve HTTP 404 y el mensaje de error en el cuerpo de la respuesta.
     */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<String> handleNotFoundException(NotFoundException ex) {
        return ResponseEntity.status(404).body(ex.getMessage());
    }

    /**
     * Captura cualquier otra excepción no contemplada específicamente.
     * Devuelve HTTP 500 con un mensaje genérico para no exponer detalles internos
     * (traza de pila, nombres de clases, etc.) que podrían ser un riesgo de seguridad.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleAllExceptions(Exception ex) {
        return ResponseEntity.status(500).body("Error interno del servidor");
    }
}

