package edu.comillas.icai.gitt.pat.spring.PracticaFinal;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Entidad JPA que representa una reserva de pista de pádel.
 *
 * <p>Se mapea a la tabla {@code reservas} en la base de datos. Relaciona a un
 * {@link ModeloUsuario} con una {@link ModeloPista} en una fecha y hora concretas.</p>
 *
 * <h3>Decisiones de diseño</h3>
 * <ul>
 *   <li><b>Campos calculados READ_ONLY</b>: {@code horaFin}, {@code estado} y
 *       {@code fechaCreacion} tienen {@code @JsonProperty(Access.READ_ONLY)}. Esto
 *       significa que Jackson los ignora al deserializar el JSON de la petición
 *       (el cliente NO los envía), pero sí los incluye en la respuesta. El servidor
 *       los calcula y asigna internamente.</li>
 *   <li><b>{@code @PrePersist}/{@code @PreUpdate}</b>: callbacks de JPA que recalculan
 *       {@code horaFin = horaInicio + duracionMinutos} de forma automática antes de
 *       cada INSERT o UPDATE, garantizando consistencia.</li>
 *   <li><b>Enum {@code Estado} interno</b>: ACTIVA/CANCELADA. Se usa soft-delete;
 *       cancelar una reserva solo cambia su estado, no la borra de la BD.</li>
 *   <li><b>Usuario asignado por sesión</b>: el campo {@code usuario} es READ_ONLY
 *       porque el controlador lo asigna desde la sesión HTTP, no del cuerpo JSON.
 *       Así se evita que un usuario pueda hacer reservas en nombre de otro.</li>
 * </ul>
 */
@Entity
@Table(name = "reservas")
@Getter
@Setter
public class ModeloReserva {

    /** Clave primaria generada autom\u00e1ticamente por la base de datos (autoincremento). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reserva", nullable = false)
    private Long idReserva;

    /**
     * Usuario que realizó la reserva.
     * READ_ONLY: el controlador lo asigna desde la sesión HTTP, no del cuerpo JSON.
     * Así un usuario no puede hacer una reserva en nombre de otra persona.
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private ModeloUsuario usuario;

    /**
     * Pista que se reserva.
     * El cliente enviaba solo el id (p.ej. {@code {"idPista": 3}}); el servicio
     * recupera la entidad completa de la BD para validar que existe y está activa.
     */
    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "id_pista", nullable = false)
    private ModeloPista pista;

    /** Día en que se quiere reservar la pista. Formato ISO: YYYY-MM-DD. */
    @NotNull
    @Column(name = "fecha_reserva", nullable = false)
    private LocalDate fechaReserva;

    /**
     * Hora a la que comienza la reserva.
     * {@code @JsonFormat(pattern = "HH:mm:ss")} indica que el JSON debe venir
     * con segundos (ej: "10:00:00") para evitar errores de parseo.
     */
    @NotNull
    @JsonFormat(pattern = "HH:mm:ss")
    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    /**
     * Duración de la reserva en minutos.
     * {@code @Min(1)} garantiza que sea al menos 1 minuto (validación Bean Validation).
     */
    @NotNull
    @Min(1)
    @Column(name = "duracion_minutos", nullable = false)
    private Integer duracionMinutos;

    /**
     * Hora de fin calculada automáticamente: {@code horaInicio + duracionMinutos}.
     * READ_ONLY: el cliente nunca la enviaba; se recalcula en {@code @PrePersist} y {@code @PreUpdate}.
     */
    @Column(name = "hora_fin", nullable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalTime horaFin;

    /**
     * Estado actual de la reserva: ACTIVA o CANCELADA.
     * READ_ONLY: el cliente no puede establecerlo directamente; se gestiona
     * mediante el endpoint DELETE (cancelación). Valor inicial: ACTIVA.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 15)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Estado estado = Estado.ACTIVA;

    /**
     * Fecha y hora en que se creó la reserva en el sistema.
     * READ_ONLY: la asigna {@code @PrePersist} automáticamente.
     */
    @Column(name = "fecha_creacion", nullable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime fechaCreacion;

    /** Constructor vacío requerido por JPA y por Jackson para deserializar JSON. */
    public ModeloReserva() {}

    /**
     * Posibles estados de una reserva. Se usa dentro de la propia clase (clase interna).
     * ACTIVA = reserva vigente; CANCELADA = cancelada por el usuario (soft-delete).
     */
    public enum Estado {
        ACTIVA, CANCELADA
    }

    /**
     * Callback de JPA ejecutado automáticamente antes de cada INSERT.
     * Rellena {@code fechaCreacion} y calcula {@code horaFin}.
     */
    @PrePersist
    public void prePersist() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
        recalcularHoraFin();
    }

    /** Callback de JPA ejecutado automáticamente antes de cada UPDATE. Recalcula {@code horaFin}. */
    @PreUpdate
    public void preUpdate() {
        recalcularHoraFin();
    }

    /**
     * Calcula y actualiza {@code horaFin} a partir de {@code horaInicio} y {@code duracionMinutos}.
     * Es un método privado reutilizado por {@code @PrePersist} y {@code @PreUpdate}.
     */
    private void recalcularHoraFin() {
        if (horaInicio != null && duracionMinutos != null) {
            this.horaFin = horaInicio.plusMinutes(duracionMinutos);
        }
    }
}

