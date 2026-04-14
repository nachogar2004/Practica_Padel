package edu.comillas.icai.gitt.pat.spring.PracticaFinal.controller;

import edu.comillas.icai.gitt.pat.spring.PracticaFinal.ModeloReserva;
import edu.comillas.icai.gitt.pat.spring.PracticaFinal.repository.RepositorioReserva;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/pistaPadel/admin")
public class AdminReservationController {

    private final RepositorioReserva reservaRepository;

    public AdminReservationController(RepositorioReserva reservaRepository) {
        this.reservaRepository = reservaRepository;
    }

    @GetMapping("/reservations")
    public List<ModeloReserva> getReservations(
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) Long courtId,
            @RequestParam(required = false) Long userId
    ) {
        return reservaRepository.adminFindAllWithFilters(date, courtId, userId);
    }
}


