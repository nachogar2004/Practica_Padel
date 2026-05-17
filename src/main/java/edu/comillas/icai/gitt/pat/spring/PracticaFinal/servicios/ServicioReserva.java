package edu.comillas.icai.gitt.pat.spring.PracticaFinal.servicios;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import edu.comillas.icai.gitt.pat.spring.PracticaFinal.ModeloPista;
import edu.comillas.icai.gitt.pat.spring.PracticaFinal.ModeloReserva;
import edu.comillas.icai.gitt.pat.spring.PracticaFinal.ModeloRol;
import edu.comillas.icai.gitt.pat.spring.PracticaFinal.ModeloUsuario;
import edu.comillas.icai.gitt.pat.spring.PracticaFinal.repository.RepositorioPista;
import edu.comillas.icai.gitt.pat.spring.PracticaFinal.repository.RepositorioReserva;
import edu.comillas.icai.gitt.pat.spring.PracticaFinal.repository.RepositorioUsuario;

/**
 * Servicio de negocio que gestiona las operaciones sobre reservas.
 *
 * <p>Es la capa intermedia entre los controladores REST y los repositorios JPA.
 * Contiene toda la lógica de negocio: validaciones, comprobación de solapes,
 * autorización por rol, etc. Los controladores delegan en este servicio.</p>
 *
 * <h3>Decisiones de diseño</h3>
 * <ul>
 *   <li><b>{@code @Service}</b>: marca esta clase como un componente de la capa de
 *       servicio, lo que permite a Spring inyectarla automáticamente.</li>
 *   <li><b>{@code @Transactional}</b>: garantiza que las operaciones de escritura
 *       (crear, cancelar, actualizar) sean atómicas. Si ocurre un error a mitad
 *       de la operación, la BD queda en el estado anterior (rollback automático).</li>
 *   <li><b>ADMIN no puede reservar</b>: las cuentas de administrador están pensadas
 *       sólo para gestión; se rechaza cualquier intento de reserva de un ADMIN.</li>
 *   <li><b>Método privado {@code obtenerReservaYValidarPropiedad}</b>: centraliza la
 *       lógica de "buscar reserva + verificar que el usuario tiene permiso sobre ella".
 *       Es reutilizado por GET, PATCH y DELETE para evitar repetición de código.</li>
 * </ul>
 */
@Service
public class ServicioReserva {

    /** Logger para registrar eventos importantes (nuevas reservas, errores, conflictos). */
    private static final Logger logger = LoggerFactory.getLogger(ServicioReserva.class);

    private final RepositorioReserva repositorioReserva;
    private final RepositorioPista repositorioPista;
    private final RepositorioUsuario repositorioUsuario;

    /**
     * Inyección de dependencias por constructor (estilo recomendado por Spring).
     * Spring detecta automáticamente los repositorios y los inyecta al crear el servicio.
     */
    public ServicioReserva(RepositorioReserva repositorioReserva,
                           RepositorioPista repositorioPista,
                           RepositorioUsuario repositorioUsuario) {
        this.repositorioReserva = repositorioReserva;
        this.repositorioPista = repositorioPista;
        this.repositorioUsuario = repositorioUsuario;
    }

    /**
     * Crea una nueva reserva en el sistema.
     *
     * <p>Flujo completo:
     * <ol>
     *   <li>Verifica que el usuario existe y tiene rol USER (los ADMIN no pueden reservar).</li>
     *   <li>Valida que la fecha/hora no sea en el pasado.</li>
     *   <li>Busca la pista en BD y comprueba que esté activa.</li>
     *   <li>Comprueba que no haya solapamiento con otra reserva activa.</li>
     *   <li>Asigna usuario y pista a la reserva y la guarda.</li>
     * </ol>
     * </p>
     *
     * @param userId id del usuario que hace la reserva (tomado de la sesión)
     * @param req    datos de la reserva (fecha, hora, pista, duración)
     * @return la reserva guardada con todos los campos calculados
     */
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

        if (!Boolean.TRUE.equals(pista.getActiva())) {
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


    /**
     * Lista las reservas de un usuario con filtros opcionales por rango de fechas/horas.
     *
     * @param userId id del usuario
     * @param from   filtra reservas desde esta fecha-hora (inclusive), o {@code null} para sin límite
     * @param to     filtra reservas hasta esta fecha-hora (inclusive), o {@code null} para sin límite
     * @return lista de reservas ordenadas cronológicamente
     */
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

    /**
     * Cancela una reserva cambiando su estado a CANCELADA (soft-delete).
     * Solo el dueño de la reserva o un ADMIN pueden cancelarla.
     *
     * @param userId        id del usuario que solicita la cancelación
     * @param reservationId id de la reserva a cancelar
     */
    @Transactional
    public void cancelarReserva(Long userId, Long reservationId) {
        ModeloReserva reserva = obtenerReservaYValidarPropiedad(userId, reservationId);
        reserva.setEstado(ModeloReserva.Estado.CANCELADA);
        repositorioReserva.save(reserva);
        logger.info("Reserva ID: {} CANCELADA por usuario ID: {}", reservationId, userId);
    }

    /**
     * Obtiene una reserva concreta validando que el usuario tenga permiso para verla.
     *
     * @param userId        id del usuario que solicita la reserva
     * @param reservationId id de la reserva
     * @return la reserva si el usuario es dueño o ADMIN
     */
    public ModeloReserva obtenerReserva(Long userId, Long reservationId) {
        return obtenerReservaYValidarPropiedad(userId, reservationId);
    }

    /**
     * Actualiza parcialmente una reserva existente.
     * Solo modifica los campos que vienen en {@code body} (los {@code null} se ignoran).
     * Aplica las mismas validaciones que {@code crearReserva}: fechas futuras y sin solapamiento.
     *
     * @param userId        id del usuario que solicita la actualización
     * @param reservationId id de la reserva a modificar
     * @param body          campos a actualizar (los nulos no se cambian)
     * @return la reserva con los cambios aplicados
     */
    @Transactional
    public ModeloReserva actualizarReserva(Long userId, Long reservationId, ModeloReserva body) {
        logger.info("Intento de actualización de reserva ID: {} por usuario ID: {}", reservationId, userId);

        //Validar existencia y propiedad
        ModeloReserva reserva = obtenerReservaYValidarPropiedad(userId, reservationId);

        //Calcular los nuevos valores propuestos
        LocalDate nuevaFecha = body.getFechaReserva() != null ? body.getFechaReserva() : reserva.getFechaReserva();
        LocalTime nuevoInicio = body.getHoraInicio() != null ? body.getHoraInicio() : reserva.getHoraInicio();
        Integer nuevaDuracion = body.getDuracionMinutos() != null ? body.getDuracionMinutos() : reserva.getDuracionMinutos();

        //ModeloPista nuevaPista = reserva.getPista();

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

        // Si pasamos todas las validaciones, AHORA SÍ aplicamos los cambios a la entidad
        reserva.setFechaReserva(nuevaFecha);
        reserva.setHoraInicio(nuevoInicio);
        reserva.setDuracionMinutos(nuevaDuracion);

        // Al hacer save, saltará el @PreUpdate que ajustará la horaFin definitiva en BD
        ModeloReserva actualizada = repositorioReserva.save(reserva);
        logger.info("Reserva ID: {} actualizada con éxito", actualizada.getIdReserva());
        return actualizada;
    }


    /**
     * Método privado reutilizable que busca una reserva por id y verifica que
     * el usuario solicitante tiene permiso sobre ella (es el dueño o es ADMIN).
     *
     * <p>Se usa en GET, PATCH y DELETE para evitar duplicar la lógica de autorización.</p>
     *
     * @param userId        id del usuario que solicita la operación
     * @param reservationId id de la reserva que se quiere acceder
     * @return la reserva si el acceso está autorizado
     * @throws ResponseStatusException 404 si no existe, 403 si no tiene permiso
     */
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