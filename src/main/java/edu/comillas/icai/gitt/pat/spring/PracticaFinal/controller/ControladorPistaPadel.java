package edu.comillas.icai.gitt.pat.spring.PracticaFinal.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import edu.comillas.icai.gitt.pat.spring.PracticaFinal.ModeloPista;
import edu.comillas.icai.gitt.pat.spring.PracticaFinal.ModeloRol;
import edu.comillas.icai.gitt.pat.spring.PracticaFinal.ModeloUsuario;
import edu.comillas.icai.gitt.pat.spring.PracticaFinal.repository.RepositorioPista;
import edu.comillas.icai.gitt.pat.spring.PracticaFinal.repository.RepositorioUsuario;
import jakarta.servlet.http.HttpSession;

/**
 * Controlador REST para la gestión de pistas de pádel.
 *
 * <p>Los endpoints de lectura ({@code GET}) son públicos (no requieren autenticación).
 * Los endpoints de escritura ({@code POST}, {@code PATCH}, {@code DELETE}) solo pueden
 * ser ejecutados por usuarios con rol ADMIN, verificado mediante el método privado
 * {@link #validarAdmin}.</p>
 *
 * <h3>Endpoints</h3>
 * <ul>
 *   <li>{@code GET /pistaPadel/courts[?active=true|false]} — listar pistas (con filtro opcional)</li>
 *   <li>{@code GET /pistaPadel/courts/{courtId}} — obtener una pista por id</li>
 *   <li>{@code POST /pistaPadel/courts} — crear nueva pista (solo ADMIN)</li>
 *   <li>{@code PATCH /pistaPadel/courts/{courtId}} — actualizar datos de pista (solo ADMIN)</li>
 *   <li>{@code DELETE /pistaPadel/courts/{courtId}} — desactivar pista (solo ADMIN)</li>
 * </ul>
 */
@RestController
@RequestMapping("/pistaPadel/courts")
public class ControladorPistaPadel {

    private final RepositorioPista repositorioPista;
    private final RepositorioUsuario repositorioUsuario;

    /** Clave usada en la sesión HTTP para almacenar el email del usuario autenticado. */
    private static final String USUARIO_SESION = "USUARIO_LOGUEADO";

    /** Inyección de dependencias por constructor. */
    public ControladorPistaPadel(RepositorioPista repositorioPista, RepositorioUsuario repositorioUsuario) {
        this.repositorioPista = repositorioPista;
        this.repositorioUsuario = repositorioUsuario;
    }

    /**
     * Método privado que verifica que el usuario de la sesión tiene rol ADMIN.
     * Si no hay sesión activa → HTTP 401. Si no es ADMIN → HTTP 403.
     *
     * @param session sesión HTTP de la petición actual
     */
    private void validarAdmin(HttpSession session) {
        String email = (String) session.getAttribute(USUARIO_SESION);
        if (email == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Debes iniciar sesión");
        }

        ModeloUsuario usuario = repositorioUsuario.findByEmail(email);
        if (usuario == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado");
        }

        if (usuario.getRol() != ModeloRol.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permisos de administrador");
        }
    }

    /**
     * Devuelve la lista de pistas. Si se pasa {@code active}, filtra por ese estado.
     *
     * @param active (opcional) {@code true} para solo activas, {@code false} para solo inactivas
     * @return lista de pistas
     */
    @GetMapping
    public List<ModeloPista> listarPistas(@RequestParam(required = false) Boolean active) {
        if (active != null) {
            return repositorioPista.findByActiva(active);
        }
        return (List<ModeloPista>) repositorioPista.findAll();
    }

    /**
     * Devuelve una pista concreta por su id.
     *
     * @param courtId id de la pista
     * @return la pista encontrada, o HTTP 404 si no existe
     */
    @GetMapping("/{courtId}")
    public ModeloPista obtenerPistaPorId(@PathVariable Long courtId) {
        return repositorioPista.findById(courtId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pista no encontrada"));
    }

    /**
     * Crea una nueva pista de pádel. Solo permitido a usuarios ADMIN.
     * Devuelve HTTP 201 Created con la pista guardada.
     *
     * @param pista   datos de la nueva pista
     * @param session sesión HTTP (debe pertenecer a un ADMIN)
     * @return la pista creada con id asignado
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ModeloPista crearPista(@RequestBody ModeloPista pista, HttpSession session) {
        validarAdmin(session); // Comprobación de seguridad

        pista.setFechaAlta(LocalDateTime.now());
        return repositorioPista.save(pista);
    }

    /**
     * Actualiza parcialmente los datos de una pista. Solo permitido a usuarios ADMIN.
     * Los campos nulos en el cuerpo JSON no se modifican.
     *
     * @param courtId id de la pista a actualizar
     * @param body    campos a modificar
     * @param session sesión HTTP (debe pertenecer a un ADMIN)
     * @return la pista con los cambios aplicados
     */
    @PatchMapping("/{courtId}")
    public ModeloPista actualizarPista(
            @PathVariable Long courtId,
            @RequestBody ModeloPista body,
            HttpSession session) {

        validarAdmin(session); // Comprobación de seguridad

        ModeloPista pista = repositorioPista.findById(courtId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pista no encontrada"));

        // Actualizar nombre
        if (body.getNombre() != null) {
            if (body.getNombre().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre no puede estar vacío");
            }
            pista.setNombre(body.getNombre());
        }

        // Actualizar ubicación
        if (body.getUbicacion() != null) {
            pista.setUbicacion(body.getUbicacion());
        }

        // Actualizar precio (debe ser >= 0)
        if (body.getPrecioHora() != null) {
            if (body.getPrecioHora().compareTo(BigDecimal.ZERO) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El precio no puede ser negativo");
            }
            pista.setPrecioHora(body.getPrecioHora());
        }

        // Actualizar estado activa (usa Boolean wrapper para distinguir null de false)
        if (body.getActiva() != null) {
            pista.setActiva(body.getActiva());
        }

        return repositorioPista.save(pista);
    }

    /**
     * Desactiva una pista (soft-delete). Solo permitido a usuarios ADMIN.
     * No borra el registro de la BD, sino que pone {@code activa = false}.
     * Devuelve HTTP 204 No Content.
     *
     * @param courtId id de la pista a desactivar
     * @param session sesión HTTP (debe pertenecer a un ADMIN)
     */
    @DeleteMapping("/{courtId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarPista(@PathVariable Long courtId, HttpSession session) {

        validarAdmin(session); // Comprobación de seguridad

        ModeloPista pista = repositorioPista.findById(courtId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pista no encontrada"));

        // Soft-delete: en lugar de borrar, desactivamos la pista para conservar el historial
        pista.setActiva(false);
        repositorioPista.save(pista);
    }
}
