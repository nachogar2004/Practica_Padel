package edu.comillas.icai.gitt.pat.spring.PracticaFinal.exception;

/**
 * Excepción personalizada que representa un recurso no encontrado (HTTP 404).
 *
 * <p>Se lanza cuando se intenta acceder a una entidad que no existe en la base de datos.
 * Al extender {@code RuntimeException} no es necesario declararla en la firma de los métodos
 * (excepción no comprobada / unchecked exception).</p>
 *
 * <p>El {@link GlobalExceptionHandler} la captura automáticamente y devuelve
 * un HTTP 404 con el mensaje de error al cliente.</p>
 */
public class NotFoundException extends RuntimeException {

    /**
     * Crea la excepción con un mensaje descriptivo del recurso no encontrado.
     *
     * @param msg descripción de lo que no se encontró (p.ej. "Usuario no encontrado")
     */
    public NotFoundException(String msg) { super(msg); }
}
