package edu.comillas.icai.gitt.pat.spring.PracticaFinal.controller;

import edu.comillas.icai.gitt.pat.spring.PracticaFinal.ModeloReserva;
import edu.comillas.icai.gitt.pat.spring.PracticaFinal.ModeloRol;
import edu.comillas.icai.gitt.pat.spring.PracticaFinal.ModeloUsuario;
import edu.comillas.icai.gitt.pat.spring.PracticaFinal.dto.PatchReservationRequest;
import edu.comillas.icai.gitt.pat.spring.PracticaFinal.repository.RepositorioUsuario;
import edu.comillas.icai.gitt.pat.spring.PracticaFinal.servicios.ServicioReserva;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/pistaPadel/reservations")
public class ControladorReserva {
    private final ServicioReserva servicioReserva;
    private final RepositorioUsuario repositorioUsuario;
    private static final String USUARIO_SESION = "USUARIO_LOGUEADO";

    public ControladorReserva(ServicioReserva servicioReserva, RepositorioUsuario repositorioUsuario) {
        this.servicioReserva = servicioReserva;
        this.repositorioUsuario = repositorioUsuario;
    }

    private ModeloUsuario obtenerUsuarioDeSesion(HttpSession session) {
        String email = (String) session.getAttribute(USUARIO_SESION);
        if (email == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado");
        ModeloUsuario usuario = repositorioUsuario.findByEmail(email);
        if (usuario == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado");
        return usuario;
    }

    // POST /pistaPadel/reservations
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ModeloReserva crearReserva(HttpSession session, @RequestBody ModeloReserva req) {
        ModeloUsuario usuario = obtenerUsuarioDeSesion(session);
        return servicioReserva.crearReserva(usuario.getIdUsuario(), req);
    }

    // GET /pistaPadel/reservations
    @GetMapping
    public List<ModeloReserva> misReservas(
            HttpSession session,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        ModeloUsuario usuario = obtenerUsuarioDeSesion(session);
        return servicioReserva.listarMisReservas(usuario.getIdUsuario(), from, to);
    }

    // GET /pistaPadel/reservations/{reservationId}
    @GetMapping("/{reservationId}")
    public ModeloReserva obtenerReserva(HttpSession session, @PathVariable Long reservationId) {
        ModeloUsuario usuario = obtenerUsuarioDeSesion(session);
        return servicioReserva.obtenerReserva(usuario.getIdUsuario(), reservationId);
    }

    // PATCH /pistaPadel/reservations/{reservationId}
    @PatchMapping("/{reservationId}")
    public ModeloReserva editarReserva(HttpSession session, @PathVariable Long reservationId, @RequestBody PatchReservationRequest body) {
        ModeloUsuario usuario = obtenerUsuarioDeSesion(session);
        // El servicio debe encargarse de validar que la reserva pertenece al idUsuario
        return servicioReserva.actualizarReserva(usuario.getIdUsuario(), reservationId, body);
    }

    // DELETE /pistaPadel/reservations/{reservationId}
    @DeleteMapping("/{reservationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelarReserva(HttpSession session, @PathVariable Long reservationId) {
        ModeloUsuario usuario = obtenerUsuarioDeSesion(session);
        // El servicio debe validar la propiedad antes de cancelar
        servicioReserva.cancelarReserva(usuario.getIdUsuario(), reservationId);
    }
}