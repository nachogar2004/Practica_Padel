package edu.comillas.icai.gitt.pat.spring.PracticaFinal.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import edu.comillas.icai.gitt.pat.spring.PracticaFinal.ModeloPista;
import edu.comillas.icai.gitt.pat.spring.PracticaFinal.ModeloReserva;
import edu.comillas.icai.gitt.pat.spring.PracticaFinal.ModeloUsuario;

/**
 * Repositorio JPA para la entidad {@link ModeloReserva}.
 *
 * <p>Además de los métodos CRUD heredados, incluye búsquedas por nombre y
 * dos consultas JPQL personalizadas ({@code @Query}) para casos que la convención
 * de nombres no puede expresar directamente.</p>
 *
 * <h3>Consultas JPQL con parámetros opcionales</h3>
 * <p>La query {@code adminFindAllWithFilters} usa el patrón
 * {@code :param IS NULL OR campo = :param} para que un parámetro {@code null}
 * equivalga a "sin filtro" — muy útil para paneles de administración.</p>
 *
 * <h3>Detección de solapes</h3>
 * <p>{@code existeSolapeActivo} usa la condición estándar de solapamiento de intervalos:
 * {@code A.inicio < B.fin && B.inicio < A.fin}. Excluye la propia reserva
 * cuando se hace un UPDATE (parámetro {@code :reservaId IS NULL OR id <> :reservaId}).</p>
 */
@Repository
public interface RepositorioReserva extends JpaRepository<ModeloReserva, Long> {

    // --- MÉTODOS DE BÚSqueda AUTOMÁTICOS (Spring Data genera el SQL por el nombre) ---

    /**
     * Reservas activas de una pista en un día, ordenadas por hora de inicio.
     * Usado por el servicio de disponibilidad para calcular qué franjas están ocupadas.
     */
    List<ModeloReserva> findByPistaAndFechaReservaAndEstadoOrderByHoraInicioAsc(
            ModeloPista pista, LocalDate fechaReserva, ModeloReserva.Estado estado);

    /**
     * Reservas de un día concreto con un estado determinado.
     * Usado por las tareas programadas para enviar recordatorios del día siguiente.
     */
    List<ModeloReserva> findByFechaReservaAndEstado(LocalDate fecha, ModeloReserva.Estado estado);

    /**
     * Reservas de un usuario con un estado concreto, ordenadas cronológicamente.
     * Usado para listar las reservas activas de un usuario.
     */
    List<ModeloReserva> findByUsuarioAndEstadoOrderByFechaReservaAscHoraInicioAsc(
            ModeloUsuario usuario, ModeloReserva.Estado estado);

    /** Todas las reservas de un usuario (independientemente del estado), ordenadas por fecha/hora. */
    List<ModeloReserva> findByUsuarioOrderByFechaReservaAscHoraInicioAsc(ModeloUsuario usuario);

    /** Reservas de un usuario a partir de una fecha (inclusive), para filtrado por fecha desde. */
    List<ModeloReserva> findByUsuarioAndFechaReservaGreaterThanEqualOrderByFechaReservaAscHoraInicioAsc(
            ModeloUsuario usuario, LocalDate from);

    /** Reservas de un usuario hasta una fecha (inclusive), para filtrado por fecha hasta. */
    List<ModeloReserva> findByUsuarioAndFechaReservaLessThanEqualOrderByFechaReservaAscHoraInicioAsc(
            ModeloUsuario usuario, LocalDate to);

    /** Reservas de un usuario entre dos fechas (ambas inclusive), para filtrado por rango de fechas. */
    List<ModeloReserva> findByUsuarioAndFechaReservaBetweenOrderByFechaReservaAscHoraInicioAsc(
            ModeloUsuario usuario, LocalDate from, LocalDate to);

    // --- CONSULTAS PERSONALIZADAS (@Query JPQL) ---

    /**
     * Devuelve todas las reservas del sistema con filtros opcionales de fecha, pista y usuario.
     * Usado por el panel de administración.
     * El patrón {@code :param IS NULL OR campo = :param} hace que si el parámetro
     * es {@code null}, ese filtro se ignora (equivale a "sin restricción").
     *
     * @param date     filtra por fecha (o {@code null} para todas las fechas)
     * @param courtId  filtra por id de pista (o {@code null} para todas)
     * @param userId   filtra por id de usuario (o {@code null} para todos)
     */
    @Query("""
        SELECT r
        FROM ModeloReserva r
        WHERE (:date IS NULL OR r.fechaReserva = :date)
          AND (:courtId IS NULL OR r.pista.idPista = :courtId)
          AND (:userId IS NULL OR r.usuario.idUsuario = :userId)
        ORDER BY r.fechaReserva, r.horaInicio
        """)
    List<ModeloReserva> adminFindAllWithFilters(
            @Param("date") LocalDate date,
            @Param("courtId") Long courtId,
            @Param("userId") Long userId);

    /**
     * Detecta si existe alguna reserva activa que solape con el intervalo propuesto.
     *
     * <p><b>Fórmula de solape</b>: dos intervalos [A,B) y [C,D) se solapan si
     * {@code A < D && C < B}. Aquí: {@code horaInicio < nuevoFin && horaFin > nuevoInicio}.</p>
     *
     * <p>El parámetro {@code :reservaId} permite excluir la propia reserva al hacer un
     * PATCH: si se pasa {@code null}, no excluye ninguna (creación nueva).</p>
     *
     * @param pistaId    id de la pista que se quiere reservar
     * @param fecha      día de la reserva
     * @param reservaId  id de la reserva que se está editando (o {@code null} si es nueva)
     * @param nuevoInicio hora de inicio del nuevo intervalo propuesto
     * @param nuevoFin   hora de fin del nuevo intervalo propuesto
     * @return {@code true} si existe solapamiento con otra reserva activa
     */
    @Query("""
        SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END
        FROM ModeloReserva r
        WHERE r.estado = edu.comillas.icai.gitt.pat.spring.PracticaFinal.ModeloReserva$Estado.ACTIVA
          AND r.pista.idPista = :pistaId
          AND r.fechaReserva = :fecha
          AND (:reservaId IS NULL OR r.idReserva <> :reservaId)
          AND r.horaInicio < :nuevoFin
          AND r.horaFin > :nuevoInicio
        """)
    boolean existeSolapeActivo(
            @Param("pistaId") Long pistaId,
            @Param("fecha") LocalDate fecha,
            @Param("reservaId") Long reservaId,
            @Param("nuevoInicio") LocalTime nuevoInicio,
            @Param("nuevoFin") LocalTime nuevoFin);
}

