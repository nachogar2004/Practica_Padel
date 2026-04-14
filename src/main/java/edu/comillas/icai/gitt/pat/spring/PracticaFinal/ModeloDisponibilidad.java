package edu.comillas.icai.gitt.pat.spring.PracticaFinal;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ModeloDisponibilidad {

    private Long idPista;
    private LocalDate fecha;
    private List<Franja> franjasDisponibles = new ArrayList<>();

    public ModeloDisponibilidad() {
    }

    public ModeloDisponibilidad(Long idPista, LocalDate fecha) {
        this.idPista = idPista;
        this.fecha = fecha;
    }

    public Long getIdPista() {
        return idPista;
    }

    public void setIdPista(Long idPista) {
        this.idPista = idPista;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public List<Franja> getFranjasDisponibles() {
        return franjasDisponibles;
    }

    public void setFranjasDisponibles(List<Franja> franjasDisponibles) {
        this.franjasDisponibles = franjasDisponibles;
    }

    // ------------------- LOGICA -------------------

    /**
     * Calcula franjas disponibles en [apertura, cierre) restando reservas ACTIVAS.
     * Importante: las reservas deben venir ya filtradas por pista + fecha + estado ACTIVA.
     */
    public void calcular(LocalTime apertura, LocalTime cierre, List<ModeloReserva> reservasActivas) {
        if (apertura == null || cierre == null || !apertura.isBefore(cierre)) {
            throw new IllegalArgumentException("Horario del club invalido");
        }

        // Intervalo total
        List<Franja> libres = new ArrayList<>();
        libres.add(new Franja(apertura, cierre));

        // Ordenar reservas por horaInicio
        reservasActivas.sort(Comparator.comparing(ModeloReserva::getHoraInicio));

        for (ModeloReserva r : reservasActivas) {
            LocalTime ini = r.getHoraInicio();
            LocalTime fin = r.getHoraFin();

            // Si una reserva cae fuera del horario, la recortamos al horario (por seguridad)
            if (fin.isBefore(apertura) || !ini.isBefore(cierre)) {
                continue;
            }
            LocalTime recIni = ini.isBefore(apertura) ? apertura : ini;
            LocalTime recFin = fin.isAfter(cierre) ? cierre : fin;

            libres = restarIntervalo(libres, new Franja(recIni, recFin));
            if (libres.isEmpty()) break;
        }

        this.franjasDisponibles = libres;
    }

    private List<Franja> restarIntervalo(List<Franja> libres, Franja ocupada) {
        List<Franja> resultado = new ArrayList<>();

        for (Franja libre : libres) {
            // No solapan
            if (!ocupaSolapa(libre, ocupada)) {
                resultado.add(libre);
                continue;
            }

            // Parte izquierda libre: [libre.inicio, ocupada.inicio)
            if (libre.inicio.isBefore(ocupada.inicio)) {
                resultado.add(new Franja(libre.inicio, ocupada.inicio));
            }

            // Parte derecha libre: [ocupada.fin, libre.fin)
            if (ocupada.fin.isBefore(libre.fin)) {
                resultado.add(new Franja(ocupada.fin, libre.fin));
            }
        }

        return resultado;
    }

    private boolean ocupaSolapa(Franja a, Franja b) {
        // Solape si a.inicio < b.fin && b.inicio < a.fin
        return a.inicio.isBefore(b.fin) && b.inicio.isBefore(a.fin);
    }

// CLASE FRANJA

    public static class Franja {
        private LocalTime inicio;
        private LocalTime fin;

        public Franja() {
        }

        public Franja(LocalTime inicio, LocalTime fin) {
            if (inicio == null || fin == null || !inicio.isBefore(fin)) {
                throw new IllegalArgumentException("Franja invalida");
            }
            this.inicio = inicio;
            this.fin = fin;
        }

        public LocalTime getInicio() {
            return inicio;
        }

        public void setInicio(LocalTime inicio) {
            this.inicio = inicio;
        }

        public LocalTime getFin() {
            return fin;
        }

        public void setFin(LocalTime fin) {
            this.fin = fin;
        }
    }
}