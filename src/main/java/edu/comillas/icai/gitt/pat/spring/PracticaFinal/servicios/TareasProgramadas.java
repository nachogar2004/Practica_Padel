package edu.comillas.icai.gitt.pat.spring.PracticaFinal.servicios;

import edu.comillas.icai.gitt.pat.spring.PracticaFinal.ModeloPista;
import edu.comillas.icai.gitt.pat.spring.PracticaFinal.ModeloReserva;
import edu.comillas.icai.gitt.pat.spring.PracticaFinal.ModeloUsuario;
import edu.comillas.icai.gitt.pat.spring.PracticaFinal.repository.RepositorioPista;
import edu.comillas.icai.gitt.pat.spring.PracticaFinal.repository.RepositorioReserva;
import edu.comillas.icai.gitt.pat.spring.PracticaFinal.repository.RepositorioUsuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class TareasProgramadas {

    private static final Logger logger = LoggerFactory.getLogger(TareasProgramadas.class);

    private final RepositorioReserva repositorioReserva;
    private final RepositorioUsuario repositorioUsuario;
    private final RepositorioPista repositorioPista;

    public TareasProgramadas(RepositorioReserva repositorioReserva,
                             RepositorioUsuario repositorioUsuario,
                             RepositorioPista repositorioPista) {
        this.repositorioReserva = repositorioReserva;
        this.repositorioUsuario = repositorioUsuario;
        this.repositorioPista = repositorioPista;
    }

    // Tarea diaria: cada día a las 02:00 AM (Recordatorio de reservas de hoy)
    @Scheduled(cron = "0 0 2 * * *")
    public void enviarRecordatoriosDiarios() {
        LocalDate hoy = LocalDate.now();
        logger.info(">>> INICIANDO TAREA PROGRAMADA: Recordatorios diarios del {}", hoy);

        List<ModeloReserva> reservasHoy = repositorioReserva.findByFechaReservaAndEstado(hoy, ModeloReserva.Estado.ACTIVA);

        if (reservasHoy.isEmpty()) {
            logger.debug("No se han encontrado reservas activas para hoy ({}).", hoy);
        }

        for (ModeloReserva reserva : reservasHoy) {
            if (reserva.getUsuario() != null) {
                // Simulación de envío de correo según guía
                logger.info("[EMAIL SENT] Recordatorio enviado a: {} | Pista: {} | Hora: {}",
                        reserva.getUsuario().getEmail(),
                        reserva.getPista().getNombre(),
                        reserva.getHoraInicio());
            }
        }
        logger.info("<<< TAREA FINALIZADA: Recordatorios diarios");
    }

    // Tarea mensual: el día 1 de cada mes a las 09:00 AM (Boletín de disponibilidad)
    @Scheduled(cron = "0 0 9 1 * *")
    public void enviarDisponibilidadMensual() {
        logger.info(">>> INICIANDO TAREA PROGRAMADA: Boletín mensual de disponibilidad");

        List<ModeloUsuario> usuarios = repositorioUsuario.findAll();
        List<ModeloPista> pistas = repositorioPista.findAll();

        logger.debug("Enviando información de {} pistas a {} usuarios registrados", pistas.size(), usuarios.size());

        for (ModeloUsuario usuario : usuarios) {
            // Aquí se simularía el envío de la tabla de horarios a cada usuario
            logger.info("[BOLETÍN ENVIADO] Usuario: {} recibió la disponibilidad del mes", usuario.getEmail());
        }

        logger.info("<<< TAREA FINALIZADA: Boletín mensual");
    }
}
