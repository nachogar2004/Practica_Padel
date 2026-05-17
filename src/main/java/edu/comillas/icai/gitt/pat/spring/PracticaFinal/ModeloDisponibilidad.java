package edu.comillas.icai.gitt.pat.spring.PracticaFinal;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * POJO (Plain Old Java Object) que representa la disponibilidad de una pista en un d\u00eda concreto.
 *
 * <p><b>No es una entidad JPA</b>: esta clase NO tiene {@code @Entity} y nunca se persiste en BD.
 * Es un objeto de respuesta que se construye al vuelo calculando qu\u00e9 franjas horarias
 * quedan libres en una pista despu\u00e9s de restar las reservas activas de ese d\u00eda.</p>
 *
 * <h3>Algoritmo de c\u00e1lculo (m\u00e9todo {@code calcular})</h3>
 * <ol>
 *   <li>Se parte de un \u00fanico intervalo libre que cubre todo el horario del club:
 *       [{@code apertura}, {@code cierre}] (p.ej. [09:00, 22:00]).</li>
 *   <li>Se ordenan las reservas activas del d\u00eda por hora de inicio.</li>
 *   <li>Por cada reserva, se llama a {@code restarIntervalo()} que recorta ese bloque
 *       de tiempo de la lista de franjas libres. Una franja libre puede dividirse en
 *       hasta dos partes (la parte anterior y la posterior a la reserva).</li>
 *   <li>El resultado final es la lista de franjas que quedan libres.</li>
 * </ol>
 *
 * <h3>Clase interna {@link Franja}</h3>
 * <p>Representa un bloque horario con {@code inicio} y {@code fin}. El constructor valida
 * que {@code inicio} sea siempre estrictamente anterior a {@code fin}.</p>
 */
@Getter
@Setter
public class ModeloDisponibilidad {

    /** Identificador de la pista a la que corresponde esta disponibilidad. */
    private Long idPista;

    /** D\u00eda al que corresponde el c\u00e1lculo de disponibilidad. */
    private LocalDate fecha;

    /** Lista de franjas horarias libres calculadas por {@link #calcular}. Empieza vac\u00eda. */
    private List<Franja> franjasDisponibles = new ArrayList<>();

    /** Constructor vac\u00edo para uso interno / Jackson. */
    public ModeloDisponibilidad() {
    }

    /**
     * Constructor de conveniencia que fija la pista y la fecha.
     *
     * @param idPista identificador de la pista
     * @param fecha   d\u00eda a consultar
     */
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

    // ------------------- L\u00d3GICA DE DISPONIBILIDAD -------------------

    /**
     * Calcula las franjas horarias disponibles en el intervalo [{@code apertura}, {@code cierre}]
     * restando los intervalos ocupados por las reservas activas del d\u00eda.
     *
     * <p>Las reservas que se pasan deben estar ya filtradas por pista, fecha y estado ACTIVA.</p>
     *
     * @param apertura       hora de apertura del club (p.ej. 09:00)
     * @param cierre         hora de cierre del club (p.ej. 22:00)
     * @param reservasActivas lista de reservas activas de la pista en ese d\u00eda
     * @throws IllegalArgumentException si el horario es inv\u00e1lido (nulo o apertura &gt;= cierre)
     */
    public void calcular(LocalTime apertura, LocalTime cierre, List<ModeloReserva> reservasActivas) {
        if (apertura == null || cierre == null || !apertura.isBefore(cierre)) {
            throw new IllegalArgumentException("Horario del club invalido");
        }

        // Punto de partida: todo el horario del club est\u00e1 libre
        List<Franja> libres = new ArrayList<>();
        libres.add(new Franja(apertura, cierre));

        // Ordenar reservas de menor a mayor hora de inicio para procesarlas en orden
        reservasActivas.sort(Comparator.comparing(ModeloReserva::getHoraInicio));

        for (ModeloReserva r : reservasActivas) {
            LocalTime ini = r.getHoraInicio();
            LocalTime fin = r.getHoraFin();

            // Si la reserva est\u00e1 completamente fuera del horario del club, la ignoramos
            if (fin.isBefore(apertura) || !ini.isBefore(cierre)) {
                continue;
            }
            // Recortamos los extremos de la reserva al horario del club (por seguridad)
            LocalTime recIni = ini.isBefore(apertura) ? apertura : ini;
            LocalTime recFin = fin.isAfter(cierre) ? cierre : fin;

            // Restamos este bloque de todas las franjas libres actuales
            libres = restarIntervalo(libres, new Franja(recIni, recFin));
            if (libres.isEmpty()) break; // Ya no queda ning\u00fan hueco libre
        }

        this.franjasDisponibles = libres;
    }

    /**
     * Recorta el intervalo {@code ocupada} de cada franja libre de la lista.
     *
     * <p>Si la franja libre y la franja ocupada se solapan, la franja libre puede
     * dividirse en hasta dos partes: la parte anterior ({@code libre.inicio} hasta
     * {@code ocupada.inicio}) y la parte posterior ({@code ocupada.fin} hasta
     * {@code libre.fin}).</p>
     *
     * @param libres  lista actual de franjas libres
     * @param ocupada franja que se quiere restar (bloque ocupado por una reserva)
     * @return nueva lista de franjas libres despu\u00e9s de restar {@code ocupada}
     */
    private List<Franja> restarIntervalo(List<Franja> libres, Franja ocupada) {
        List<Franja> resultado = new ArrayList<>();

        for (Franja libre : libres) {
            // Si no hay solape, la franja libre queda intacta
            if (!ocupaSolapa(libre, ocupada)) {
                resultado.add(libre);
                continue;
            }

            // Parte izquierda libre: desde el inicio del hueco hasta donde empieza la reserva
            if (libre.inicio.isBefore(ocupada.inicio)) {
                resultado.add(new Franja(libre.inicio, ocupada.inicio));
            }

            // Parte derecha libre: desde donde termina la reserva hasta el final del hueco
            if (ocupada.fin.isBefore(libre.fin)) {
                resultado.add(new Franja(ocupada.fin, libre.fin));
            }
        }

        return resultado;
    }

    /**
     * Determina si dos franjas se solapan en el tiempo.
     *
     * <p>Dos franjas A y B se solapan si y solo si:<br>
     * {@code A.inicio < B.fin  &&  B.inicio < A.fin}</p>
     *
     * <p>Esta condici\u00f3n es la forma est\u00e1ndar para detectar solapamiento de intervalos.
     * Si alguna de las dos condiciones falla, los intervalos son adyacentes o disjuntos.</p>
     *
     * @param a primera franja
     * @param b segunda franja
     * @return {@code true} si se solapan, {@code false} si no
     */
    private boolean ocupaSolapa(Franja a, Franja b) {
        return a.inicio.isBefore(b.fin) && b.inicio.isBefore(a.fin);
    }

    // ------------------- CLASE INTERNA FRANJA -------------------

    /**
     * Representa un bloque horario con hora de inicio y hora de fin.
     *
     * <p>Usada internamente para representar tanto franjas libres como franjas ocupadas.
     * El constructor valida que {@code inicio} sea siempre anterior a {@code fin}.</p>
     */
    public static class Franja {
        /** Hora de inicio de la franja (inclusiva). */
        private LocalTime inicio;
        /** Hora de fin de la franja (exclusiva). */
        private LocalTime fin;

        /** Constructor vac\u00edo para Jackson (serializaci\u00f3n JSON). */
        public Franja() {
        }

        /**
         * Crea una franja con las horas indicadas.
         *
         * @param inicio hora de inicio
         * @param fin    hora de fin (debe ser posterior a {@code inicio})
         * @throws IllegalArgumentException si los par\u00e1metros son nulos o inv\u00e1lidos
         */
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