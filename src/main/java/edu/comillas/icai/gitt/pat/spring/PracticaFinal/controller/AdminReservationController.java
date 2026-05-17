package edu.comillas.icai.gitt.pat.spring.PracticaFinal.controller;

import edu.comillas.icai.gitt.pat.spring.PracticaFinal.ModeloReserva;
import edu.comillas.icai.gitt.pat.spring.PracticaFinal.repository.RepositorioReserva;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Controlador REST para el panel de administración de reservas.
 *
 * <p>Proporciona una vista completa de todas las reservas del sistema con filtros opcionales.
 * Accede directamente al repositorio (sin capa de servicio) porque es una operación
 * de solo lectura sin lógica de negocio adicional.</p>
 *
 * <p><b>Nota de seguridad</b>: actualmente este endpoint no verifica la sesión del usuario.
 * En un sistema en producción debería comprobar que el solicitante tiene rol ADMIN.</p>
 *
 * <h3>Endpoint</h3>
 * {@code GET /pistaPadel/admin/reservations?date=...&courtId=...&userId=...}
 * (todos los parámetros son opcionales)
 */
@RestController
@RequestMapping("/pistaPadel/admin")
public class AdminReservationController {

    private final RepositorioReserva reservaRepository;

    /** Inyección de dependencias por constructor. */
    public AdminReservationController(RepositorioReserva reservaRepository) {
        this.reservaRepository = reservaRepository;
    }

    /**
     * Devuelve todas las reservas del sistema con filtros opcionales.
     *
     * @param date     filtra por fecha exacta (o {@code null} para todas las fechas)
     * @param courtId  filtra por id de pista (o {@code null} para todas las pistas)
     * @param userId   filtra por id de usuario (o {@code null} para todos los usuarios)
     * @return lista de reservas que cumplen los criterios, ordenadas por fecha y hora
     */
    @GetMapping("/reservations")
    public List<ModeloReserva> getReservations(
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) Long courtId,
            @RequestParam(required = false) Long userId
    ) {
        return reservaRepository.adminFindAllWithFilters(date, courtId, userId);
    }
}


