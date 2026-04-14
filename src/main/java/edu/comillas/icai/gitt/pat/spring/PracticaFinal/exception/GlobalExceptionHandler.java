package edu.comillas.icai.gitt.pat.spring.PracticaFinal.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Captura las excepciones estándar de Spring (ResponseStatusException).
     * Mantiene la lógica de tu ManejadorErroresGlobales: devuelve el status code original.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Void> handleResponseStatusException(ResponseStatusException ex) {
        return new ResponseEntity<>(ex.getStatusCode());
    }

    /**
     * Captura tu excepción personalizada NotFoundException.
     * Mantiene la lógica de tu GlobalExceptionHandler anterior: devuelve 404 y el mensaje.
     */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<String> handleNotFoundException(NotFoundException ex) {
        return ResponseEntity.status(404).body(ex.getMessage());
    }

    /**
     * Opcional: Captura errores genéricos para evitar que se filtre información sensible
     * y devolver un 500 limpio.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleAllExceptions(Exception ex) {
        return ResponseEntity.status(500).body("Error interno del servidor");
    }
}

