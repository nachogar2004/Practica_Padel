package edu.comillas.icai.gitt.pat.spring.PracticaFinal.servicios;

import edu.comillas.icai.gitt.pat.spring.PracticaFinal.ModeloPista;
import edu.comillas.icai.gitt.pat.spring.PracticaFinal.ModeloReserva;
import edu.comillas.icai.gitt.pat.spring.PracticaFinal.ModeloUsuario;
import edu.comillas.icai.gitt.pat.spring.PracticaFinal.ModeloRol;
import edu.comillas.icai.gitt.pat.spring.PracticaFinal.dto.PatchReservationRequest;
import edu.comillas.icai.gitt.pat.spring.PracticaFinal.repository.RepositorioPista;
import edu.comillas.icai.gitt.pat.spring.PracticaFinal.repository.RepositorioReserva;
import edu.comillas.icai.gitt.pat.spring.PracticaFinal.repository.RepositorioUsuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class ServicioReserva {

    private static final Logger logger = LoggerFactory.getLogger(ServicioReserva.class);

    private final RepositorioReserva repositorioReserva;
    private final RepositorioPista repositorioPista;
    private final RepositorioUsuario repositorioUsuario;

    public ServicioReserva(RepositorioReserva repositorioReserva,
                           RepositorioPista repositorioPista,
                           RepositorioUsuario repositorioUsuario) {
        this.repositorioReserva = repositorioReserva;
        this.repositorioPista = repositorioPista;
        this.repositorioUsuario = repositorioUsuario;
    }

    @Transactional
    public ModeloReserva crearReserva(Long userId, ModeloReserva req) {
        // Recuperamos al usuario para verificar su existencia y su ROL
        ModeloUsuario usuario = repositorioUsuario.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));

        // Si es ADMIN, no puede realizar reservas
        if (usuario.getRol() == ModeloRol.ADMIN) {
            logger.warn("Intento de reserva denegado: El usuario ID {} es ADMIN", userId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Los administradores no pueden realizar reservas");
        }

        // Validaciones de fecha y hora
        if (req.getFechaReserva().isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No puedes reservar en una fecha pasada");
        }

        if (req.getFechaReserva().isEqual(LocalDate.now()) && req.getHoraInicio().isBefore(LocalTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La hora de inicio ya ha pasado");
        }

        // Buscamos la pista real en la base de datos para ver si está activa
        ModeloPista pista = repositorioPista.findById(req.getPista().getIdPista())
                .orElseThrow(() -> {
                    logger.error("Error: Intento de reserva en pista inexistente ID: {}", req.getPista().getIdPista());
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Pista no encontrada");
                });

        if (!pista.isActiva()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Esta pista no está activa");
        }

        logger.info("Solicitud de nueva reserva - Usuario ID: {} (Rol: USER)", userId);

        // Validación de solapes
        logger.debug("Validando solapes para Pista: {}, Fecha: {}, Inicio: {}",
                pista.getNombre(), req.getFechaReserva(), req.getHoraInicio());

        LocalTime horaFin = req.getHoraInicio().plusMinutes(req.getDuracionMinutos());

        boolean solape = repositorioReserva.existeSolapeActivo(
                pista.getIdPista(), req.getFechaReserva(), null, req.getHoraInicio(), horaFin);

        if (solape) {
            logger.warn("Conflicto de reserva: Horario ocupado en pista {}", pista.getNombre());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La pista ya está ocupada en ese horario");
        }

        // Configuración final y guardado
        req.setUsuario(usuario);
        req.setPista(pista);
        req.setEstado(ModeloReserva.Estado.ACTIVA);

        ModeloReserva guardada = repositorioReserva.save(req);
        logger.info("Reserva creada con éxito para usuario {}. ID Reserva: {}", usuario.getEmail(), guardada.getIdReserva());
        return guardada;
    }


    public List<ModeloReserva> listarMisReservas(Long userId, LocalDateTime from, LocalDateTime to) {
        logger.info("Consultando historial de reservas para Usuario ID: {}", userId);
        ModeloUsuario usuario = repositorioUsuario.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        List<ModeloReserva> reservas = repositorioReserva.findByUsuarioOrderByFechaReservaAscHoraInicioAsc(usuario);

        // Filtrado por fechas si se proporcionan
        if (from != null) {
            reservas = reservas.stream()
                    .filter(r -> LocalDateTime.of(r.getFechaReserva(), r.getHoraInicio()).isAfter(from) ||
                            LocalDateTime.of(r.getFechaReserva(), r.getHoraInicio()).isEqual(from))
                    .toList();
        }
        if (to != null) {
            reservas = reservas.stream()
                    .filter(r -> LocalDateTime.of(r.getFechaReserva(), r.getHoraInicio()).isBefore(to) ||
                            LocalDateTime.of(r.getFechaReserva(), r.getHoraInicio()).isEqual(to))
                    .toList();
        }
        return reservas;
    }

    @Transactional
    public void cancelarReserva(Long userId, Long reservationId) {
        ModeloReserva reserva = obtenerReservaYValidarPropiedad(userId, reservationId);
        reserva.setEstado(ModeloReserva.Estado.CANCELADA);
        repositorioReserva.save(reserva);
        logger.info("Reserva ID: {} CANCELADA por usuario ID: {}", reservationId, userId);
    }

    public ModeloReserva obtenerReserva(Long userId, Long reservationId) {
        return obtenerReservaYValidarPropiedad(userId, reservationId);
    }

    @Transactional
    public ModeloReserva actualizarReserva(Long userId, Long reservationId, PatchReservationRequest body) {
        logger.info("Intento de actualización de reserva ID: {} por usuario ID: {}", reservationId, userId);

        //Validar existencia y propiedad
        ModeloReserva reserva = obtenerReservaYValidarPropiedad(userId, reservationId);

        //Calcular los nuevos valores propuestos
        LocalDate nuevaFecha = body.getDate() != null ? body.getDate() : reserva.getFechaReserva();
        LocalTime nuevoInicio = body.getStartTime() != null ? body.getStartTime() : reserva.getHoraInicio();
        Integer nuevaDuracion = body.getDurationMinutes() != null ? body.getDurationMinutes() : reserva.getDuracionMinutos();

        ModeloPista nuevaPista = reserva.getPista();

        // Validar que la nueva fecha/hora no sea en el pasado
        if (nuevaFecha.isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No puedes reprogramar a una fecha pasada");
        }
        if (nuevaFecha.isEqual(LocalDate.now()) && nuevoInicio.isBefore(LocalTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La nueva hora de inicio ya ha pasado");
        }

        LocalTime nuevoFin = nuevoInicio.plusMinutes(nuevaDuracion);

        // Validar solapes con el nuevo horario usando las variables temporales
        logger.debug("Validando disponibilidad para la actualización de la reserva {}", reservationId);

        // Pasamos reservationId para que el repositorio ignore esta misma reserva al buscar choques
        boolean haySolape = repositorioReserva.existeSolapeActivo(
                reserva.getPista().getIdPista(),
                nuevaFecha,
                reservationId,
                nuevoInicio,
                nuevoFin
        );

        if (haySolape) {
            logger.warn("No se puede actualizar: conflicto de horario con otra reserva");
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El nuevo horario no está disponible");
        }

        // 4. Si pasamos todas las validaciones, AHORA SÍ aplicamos los cambios a la entidad
        reserva.setFechaReserva(nuevaFecha);
        reserva.setHoraInicio(nuevoInicio);
        reserva.setDuracionMinutos(nuevaDuracion);

        // Al hacer save, saltará el @PreUpdate que ajustará la horaFin definitiva en BD
        ModeloReserva actualizada = repositorioReserva.save(reserva);
        logger.info("Reserva ID: {} actualizada con éxito", actualizada.getIdReserva());
        return actualizada;
    }


    private ModeloReserva obtenerReservaYValidarPropiedad(Long userId, Long reservationId) {
        logger.debug("Verificando permisos para User: {}, Reserva: {}", userId, reservationId);

        ModeloUsuario solicitante = repositorioUsuario.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        ModeloReserva reserva = repositorioReserva.findById(reservationId)
                .orElseThrow(() -> {
                    logger.error("Error: Reserva {} no encontrada", reservationId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Reserva no encontrada");
                });

        boolean esAdmin = solicitante.getRol() == ModeloRol.ADMIN;
        boolean esDueno = reserva.getUsuario().getIdUsuario().equals(solicitante.getIdUsuario());

        if (!esAdmin && !esDueno) {
            logger.warn("Acceso denegado: Usuario {} no tiene permiso sobre reserva {}", userId, reservationId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso sobre esta reserva");
        }
        return reserva;
    }
}