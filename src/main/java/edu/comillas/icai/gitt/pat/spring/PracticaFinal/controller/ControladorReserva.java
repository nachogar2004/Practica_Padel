package edu.comillas.icai.gitt.pat.spring.PracticaFinal.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
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

import edu.comillas.icai.gitt.pat.spring.PracticaFinal.ModeloReserva;
import edu.comillas.icai.gitt.pat.spring.PracticaFinal.ModeloUsuario;
import edu.comillas.icai.gitt.pat.spring.PracticaFinal.repository.RepositorioUsuario;
import edu.comillas.icai.gitt.pat.spring.PracticaFinal.servicios.ServicioReserva;
import jakarta.servlet.http.HttpSession;

/**
 * Controlador REST que gestiona las reservas de pistas de pádel de los usuarios.
 *
 * <p>Todos los endpoints requieren autenticación: el usuario debe haber iniciado sesión.
 * El método privado {@link #obtenerUsuarioDeSesion} verifica la sesión HTTP en cada llamada.</p>
 *
 * <p>La lógica de negocio (validaciones, comprobación de solapes, autorización por
 * propiedad) se delega al {@link ServicioReserva}. El controlador solo se encarga
 * de la capa HTTP: leer parámetros, obtener el usuario de sesión y devolver la respuesta.</p>
 *
 * <h3>Endpoints</h3>
 * <ul>
 *   <li>{@code POST /pistaPadel/reservations} — crear reserva (devuelve 201)</li>
 *   <li>{@code GET /pistaPadel/reservations} — listar mis reservas (con filtros opcionales)</li>
 *   <li>{@code GET /pistaPadel/reservations/{id}} — obtener una reserva concreta</li>
 *   <li>{@code PATCH /pistaPadel/reservations/{id}} — editar fecha/hora/duración</li>
 *   <li>{@code DELETE /pistaPadel/reservations/{id}} — cancelar reserva (devuelve 204)</li>
 * </ul>
 */
@RestController
@RequestMapping("/pistaPadel/reservations")
public class ControladorReserva {
    private final ServicioReserva servicioReserva;
    private final RepositorioUsuario repositorioUsuario;
    /** Clave usada en la sesión HTTP para almacenar el email del usuario autenticado. */
    private static final String USUARIO_SESION = "USUARIO_LOGUEADO";

    /** Inyección de dependencias por constructor. */
    public ControladorReserva(ServicioReserva servicioReserva, RepositorioUsuario repositorioUsuario) {
        this.servicioReserva = servicioReserva;
        this.repositorioUsuario = repositorioUsuario;
    }

    /**
     * Método privado que lee el email almacenado en la sesión HTTP y recupera el
     * {@link ModeloUsuario} correspondiente de la base de datos.
     *
     * <p>Si no hay sesión activa (el usuario no ha hecho login) o el usuario no existe,
     * lanza {@code 401 Unauthorized}.</p>
     *
     * @param session sesión HTTP de la petición actual
     * @return el usuario autenticado
     */
    private ModeloUsuario obtenerUsuarioDeSesion(HttpSession session) {
        String email = (String) session.getAttribute(USUARIO_SESION);
        if (email == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado");
        ModeloUsuario usuario = repositorioUsuario.findByEmail(email);
        if (usuario == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado");
        return usuario;
    }

    /**
     * Crea una nueva reserva para el usuario autenticado.
     * Devuelve HTTP 201 Created con la reserva guardada.
     *
     * @param session sesión HTTP
     * @param req     datos de la reserva (fecha, hora, pista, duración)
     * @return la reserva creada con id y campos calculados
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ModeloReserva crearReserva(HttpSession session, @RequestBody ModeloReserva req) {
        ModeloUsuario usuario = obtenerUsuarioDeSesion(session);
        return servicioReserva.crearReserva(usuario.getIdUsuario(), req);
    }

    /**
     * Lista las reservas del usuario autenticado con filtros opcionales por rango de fecha-hora.
     *
     * @param session sesión HTTP
     * @param from    (opcional) filtra reservas desde esta fecha-hora
     * @param to      (opcional) filtra reservas hasta esta fecha-hora
     * @return lista de reservas ordenadas cronológicamente
     */
    @GetMapping
    public List<ModeloReserva> misReservas(
            HttpSession session,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        ModeloUsuario usuario = obtenerUsuarioDeSesion(session);
        return servicioReserva.listarMisReservas(usuario.getIdUsuario(), from, to);
    }

    /**
     * Devuelve los detalles de una reserva concreta.
     * Solo puede verla el dueño de la reserva o un ADMIN.
     *
     * @param session       sesión HTTP
     * @param reservationId id de la reserva a consultar
     * @return la reserva encontrada
     */
    @GetMapping("/{reservationId}")
    public ModeloReserva obtenerReserva(HttpSession session, @PathVariable Long reservationId) {
        ModeloUsuario usuario = obtenerUsuarioDeSesion(session);
        return servicioReserva.obtenerReserva(usuario.getIdUsuario(), reservationId);
    }

    /**
     * Edita parcialmente una reserva: fecha, hora de inicio y/o duración.
     * Los campos nulos en el cuerpo JSON no se modifican.
     *
     * @param session       sesión HTTP
     * @param reservationId id de la reserva a editar
     * @param body          campos a actualizar
     * @return la reserva con los cambios aplicados
     */
    @PatchMapping("/{reservationId}")
    public ModeloReserva editarReserva(HttpSession session, @PathVariable Long reservationId, @RequestBody ModeloReserva body) {
        ModeloUsuario usuario = obtenerUsuarioDeSesion(session);
        return servicioReserva.actualizarReserva(usuario.getIdUsuario(), reservationId, body);
    }

    /**
     * Cancela una reserva (soft-delete: cambia su estado a CANCELADA).
     * Devuelve HTTP 204 No Content.
     *
     * @param session       sesión HTTP
     * @param reservationId id de la reserva a cancelar
     */
    @DeleteMapping("/{reservationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelarReserva(HttpSession session, @PathVariable Long reservationId) {
        ModeloUsuario usuario = obtenerUsuarioDeSesion(session);
        servicioReserva.cancelarReserva(usuario.getIdUsuario(), reservationId);
    }
}