package edu.comillas.icai.gitt.pat.spring.PracticaFinal.repository;

import edu.comillas.icai.gitt.pat.spring.PracticaFinal.ModeloPista;
import edu.comillas.icai.gitt.pat.spring.PracticaFinal.ModeloReserva;
import edu.comillas.icai.gitt.pat.spring.PracticaFinal.ModeloUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface RepositorioReserva extends JpaRepository<ModeloReserva, Long> {

    // --- MÉTODOS DE BÚSQUEDA AUTOMÁTICOS ---

    // Reservas de una pista en un día (solo activas) para el cálculo de disponibilidad
    List<ModeloReserva> findByPistaAndFechaReservaAndEstadoOrderByHoraInicioAsc(
            ModeloPista pista, LocalDate fechaReserva, ModeloReserva.Estado estado);

    // Para las TareasProgramadas
    List<ModeloReserva> findByFechaReservaAndEstado(LocalDate fecha, ModeloReserva.Estado estado);

    // Reservas de un usuario (activas o todas)
    List<ModeloReserva> findByUsuarioAndEstadoOrderByFechaReservaAscHoraInicioAsc(
            ModeloUsuario usuario, ModeloReserva.Estado estado);

    List<ModeloReserva> findByUsuarioOrderByFechaReservaAscHoraInicioAsc(ModeloUsuario usuario);

    // Consultas con filtros de fecha para "Mis Reservas"
    List<ModeloReserva> findByUsuarioAndFechaReservaGreaterThanEqualOrderByFechaReservaAscHoraInicioAsc(
            ModeloUsuario usuario, LocalDate from);

    List<ModeloReserva> findByUsuarioAndFechaReservaLessThanEqualOrderByFechaReservaAscHoraInicioAsc(
            ModeloUsuario usuario, LocalDate to);

    List<ModeloReserva> findByUsuarioAndFechaReservaBetweenOrderByFechaReservaAscHoraInicioAsc(
            ModeloUsuario usuario, LocalDate from, LocalDate to);

    // --- CONSULTAS PERSONALIZADAS (@Query) ---

    // Consulta para el panel de administración con filtros opcionales
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

    // Validación de solapes para reservas activas (excluyendo la reserva actual si es una actualización)
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

