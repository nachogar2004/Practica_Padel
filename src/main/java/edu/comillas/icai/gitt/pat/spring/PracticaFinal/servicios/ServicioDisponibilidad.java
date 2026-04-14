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

@Service
public class ServicioDisponibilidad {

    // Ajusta horario si el enunciado lo define
    private static final LocalTime HORA_APERTURA = LocalTime.of(9, 0);
    private static final LocalTime HORA_CIERRE   = LocalTime.of(22, 0);

    private final RepositorioPista repositorioPista;
    private final RepositorioReserva repositorioReserva;

    public ServicioDisponibilidad(RepositorioPista repositorioPista, RepositorioReserva repositorioReserva) {
        this.repositorioPista = repositorioPista;
        this.repositorioReserva = repositorioReserva;
    }

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
