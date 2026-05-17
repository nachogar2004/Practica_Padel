package edu.comillas.icai.gitt.pat.spring.PracticaFinal.servicios;



import edu.comillas.icai.gitt.pat.spring.PracticaFinal.ModeloDisponibilidad;
import edu.comillas.icai.gitt.pat.spring.PracticaFinal.ModeloPista;
import edu.comillas.icai.gitt.pat.spring.PracticaFinal.ModeloReserva;
import edu.comillas.icai.gitt.pat.spring.PracticaFinal.repository.RepositorioPista;
import edu.comillas.icai.gitt.pat.spring.PracticaFinal.repository.RepositorioReserva;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio que calcula la disponibilidad horaria de las pistas de pádel.
 *
 * <p>Obtiene las reservas activas de la BD y delega el cálculo de franjas libres
 * al método {@link ModeloDisponibilidad#calcular} (lógica de intervalos).</p>
 *
 * <h3>Constantes de horario del club</h3>
 * <ul>
 *   <li>{@code HORA_APERTURA = 09:00} — inicio del horario disponible.</li>
 *   <li>{@code HORA_CIERRE = 22:00} — fin del horario disponible.</li>
 * </ul>
 *
 * <p>Estos valores son constantes de clase ({@code static final}) porque son
 * configuración fija del sistema; si el horario cambiase podrían moverse a
 * {@code application.properties}.</p>
 */
@Service
public class ServicioDisponibilidad {

    /** Hora de apertura del club. Ninguna reserva puede empezar antes. */
    private static final LocalTime HORA_APERTURA = LocalTime.of(9, 0);
    /** Hora de cierre del club. Ninguna reserva puede terminar después. */
    private static final LocalTime HORA_CIERRE   = LocalTime.of(22, 0);

    private final RepositorioPista repositorioPista;
    private final RepositorioReserva repositorioReserva;

    /** Inyección de dependencias por constructor. */
    public ServicioDisponibilidad(RepositorioPista repositorioPista, RepositorioReserva repositorioReserva) {
        this.repositorioPista = repositorioPista;
        this.repositorioReserva = repositorioReserva;
    }

    /**
     * Calcula la disponibilidad de una pista concreta en un día.
     * Usado por: {@code GET /pistaPadel/courts/{courtId}/availability?date=...}
     *
     * @param courtId id de la pista
     * @param date    día a consultar
     * @return objeto con las franjas horarias libres
     */
    // GET /courts/{courtId}/availability?date=...
    public ModeloDisponibilidad disponibilidadDePista(Long courtId, LocalDate date) {
        if (courtId == null || date == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parametros invalidos");
        }

        ModeloPista pista = repositorioPista.findById(courtId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pista no encontrada"));

        List<ModeloReserva> reservasActivas = repositorioReserva
                .findByPistaAndFechaReservaAndEstadoOrderByHoraInicioAsc(
                        pista, date, ModeloReserva.Estado.ACTIVA
                );

        ModeloDisponibilidad disp = new ModeloDisponibilidad(pista.getIdPista(), date);
        disp.calcular(HORA_APERTURA, HORA_CIERRE, reservasActivas);
        return disp;
    }

    /**
     * Calcula la disponibilidad de todas las pistas (o una sola si se filtra por {@code courtIdOpcional})
     * en un día concreto.
     * Usado por: {@code GET /pistaPadel/availability?date=...&courtId=...}
     *
     * @param date             día a consultar
     * @param courtIdOpcional  si no es {@code null}, limita el resultado a esa pista
     * @return lista de disponibilidades, una por pista
     */
    // GET /availability?date=...&courtId=... (courtId opcional)
    public List<ModeloDisponibilidad> disponibilidadGeneral(LocalDate date, Long courtIdOpcional) {
        if (date == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Falta date");
        }

        // Si mandan courtId, devolvemos lista de 1 elemento (o puedes devolver un solo objeto)
        if (courtIdOpcional != null) {
            List<ModeloDisponibilidad> out = new ArrayList<>();
            out.add(disponibilidadDePista(courtIdOpcional, date));
            return out;
        }

        // Si no mandan courtId, calculamos para todas las pistas
        List<ModeloPista> pistas = repositorioPista.findAll();
        List<ModeloDisponibilidad> result = new ArrayList<>();

        for (ModeloPista pista : pistas) {
            List<ModeloReserva> reservasActivas = repositorioReserva
                    .findByPistaAndFechaReservaAndEstadoOrderByHoraInicioAsc(
                            pista, date, ModeloReserva.Estado.ACTIVA
                    );

            ModeloDisponibilidad disp = new ModeloDisponibilidad(pista.getIdPista(), date);
            disp.calcular(HORA_APERTURA, HORA_CIERRE, reservasActivas);
            result.add(disp);
        }

        return result;
    }
}
