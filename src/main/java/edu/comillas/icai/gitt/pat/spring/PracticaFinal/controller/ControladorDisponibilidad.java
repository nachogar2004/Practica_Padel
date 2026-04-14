package edu.comillas.icai.gitt.pat.spring.PracticaFinal.controller;
import edu.comillas.icai.gitt.pat.spring.PracticaFinal.ModeloDisponibilidad;
import edu.comillas.icai.gitt.pat.spring.PracticaFinal.servicios.ServicioDisponibilidad;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/pistaPadel")
public class ControladorDisponibilidad {

    private final ServicioDisponibilidad servicioDisponibilidad;

    public ControladorDisponibilidad(ServicioDisponibilidad servicioDisponibilidad) {
        this.servicioDisponibilidad = servicioDisponibilidad;
    }

    // GET /pistaPadel/availability?date=YYYY-MM-DD&courtId=...
    @GetMapping("/availability")
    public List<ModeloDisponibilidad> availability(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long courtId
    ) {
        return servicioDisponibilidad.disponibilidadGeneral(date, courtId);
    }

    // GET /pistaPadel/courts/{courtId}/availability?date=YYYY-MM-DD
    @GetMapping("/courts/{courtId}/availability")
    public ModeloDisponibilidad availabilityCourt(
            @PathVariable Long courtId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
       return servicioDisponibilidad.disponibilidadDePista(courtId, date);

    }


}
