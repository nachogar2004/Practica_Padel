package edu.comillas.icai.gitt.pat.spring.PracticaFinal.controller;
import edu.comillas.icai.gitt.pat.spring.PracticaFinal.ModeloDisponibilidad;
import edu.comillas.icai.gitt.pat.spring.PracticaFinal.servicios.ServicioDisponibilidad;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Controlador REST para consultar la disponibilidad de pistas de pádel.
 *
 * <p>Expone endpoints públicos (no requieren autenticación) para que cualquier
 * usuario pueda consultar qué franjas horarias están libres en una pista o en
 * todas las pistas para un día dado.</p>
 *
 * <p>Delega el cálculo al {@link ServicioDisponibilidad}.</p>
 *
 * <h3>Endpoints</h3>
 * <ul>
 *   <li>{@code GET /pistaPadel/availability?date=YYYY-MM-DD[&courtId=N]}
 *       — disponibilidad de todas las pistas (o solo una si se pasa courtId).</li>
 *   <li>{@code GET /pistaPadel/courts/{courtId}/availability?date=YYYY-MM-DD}
 *       — disponibilidad de una pista concreta.</li>
 * </ul>
 */
@RestController
@RequestMapping("/pistaPadel")
public class ControladorDisponibilidad {

    private final ServicioDisponibilidad servicioDisponibilidad;

    /** Inyección de dependencias por constructor. */
    public ControladorDisponibilidad(ServicioDisponibilidad servicioDisponibilidad) {
        this.servicioDisponibilidad = servicioDisponibilidad;
    }

    /**
     * Devuelve la disponibilidad de todas las pistas (o solo una si se filtra por {@code courtId})
     * en la fecha indicada.
     *
     * @param date     fecha a consultar en formato ISO (YYYY-MM-DD)
     * @param courtId  (opcional) filtra solo la pista con este id
     * @return lista de disponibilidades, una por pista consultada
     */
    @GetMapping("/availability")
    public List<ModeloDisponibilidad> availability(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long courtId
    ) {
        return servicioDisponibilidad.disponibilidadGeneral(date, courtId);
    }

    /**
     * Devuelve la disponibilidad de una pista concreta en la fecha indicada.
     *
     * @param courtId  id de la pista a consultar
     * @param date     fecha a consultar en formato ISO (YYYY-MM-DD)
     * @return disponibilidad con las franjas libres
     */
    @GetMapping("/courts/{courtId}/availability")
    public ModeloDisponibilidad availabilityCourt(
            @PathVariable Long courtId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
       return servicioDisponibilidad.disponibilidadDePista(courtId, date);
    }
}
