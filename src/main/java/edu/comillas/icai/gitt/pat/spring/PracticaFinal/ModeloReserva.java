package edu.comillas.icai.gitt.pat.spring.PracticaFinal;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "reservas")
@Getter
@Setter
public class ModeloReserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reserva", nullable = false)
    private Long idReserva;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    // Se marca READ_ONLY porque el controlador lo asigna desde la sesión
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private ModeloUsuario usuario;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "id_pista", nullable = false)
    private ModeloPista pista;

    @NotNull
    @Column(name = "fecha_reserva", nullable = false)
    private LocalDate fechaReserva;

    @NotNull
    @JsonFormat(pattern = "HH:mm:ss") // Asegúrate de que tu test envíe segundos (ej: 10:00:00)
    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @NotNull
    @Min(1)
    @Column(name = "duracion_minutos", nullable = false)
    private Integer duracionMinutos;

    // Campos calculados según la Guía
    // Se marcan READ_ONLY para evitar el error 400 al no venir en el JSON del test
    @Column(name = "hora_fin", nullable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalTime horaFin;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 15)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Estado estado = Estado.ACTIVA;

    @Column(name = "fecha_creacion", nullable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime fechaCreacion;

    public ModeloReserva() {} // Constructor vacío necesario para JPA/Jackson

    public enum Estado {
        ACTIVA, CANCELADA
    }

    @PrePersist
    public void prePersist() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
        recalcularHoraFin();
    }

    @PreUpdate
    public void preUpdate() {
        recalcularHoraFin();
    }

    private void recalcularHoraFin() {
        if (horaInicio != null && duracionMinutos != null) {
            this.horaFin = horaInicio.plusMinutes(duracionMinutos);
        }
    }
}

/*package edu.comillas.icai.gitt.pat.spring.PracticaFinal;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.Setter;

@Entity
@Table(name = "reservas")
@Getter
@Setter

public class ModeloReserva {

    @Id
    @Getter
    @Setter
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reserva", nullable = false)
    private Long idReserva;

    // Exactamente 1 usuario
    @Getter
    @Setter
    @ManyToOne(optional = false)
    // Esto permite que el usuario NO se envíe en el JSON de entrada (POST)
    // pero SÍ aparezca en el JSON de salida (cuando haces el GET)
    @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private ModeloUsuario usuario;


    // Exactamente 1 pista
    @Getter
    @Setter
    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "id_pista", nullable = false)
    private ModeloPista pista;


    // Dia de la reserva
    @Getter
    @Setter
    @NotNull
    @Column(name = "fecha_reserva", nullable = false)
    private LocalDate fechaReserva;

    // Hora de inicio
    @Getter
    @Setter
    @NotNull
    @JsonFormat(pattern = "HH:mm:ss")
    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    // Duracion en minutos
    @Getter
    @Setter
    @NotNull
    @Min(1)
    @Column(name = "duracion_minutos", nullable = false)
    private Integer duracionMinutos;

    // Hora fin (calculada)
    // La guardamos para facilitar consultas, pero se calcula automaticamente.
    @Getter
    @Setter
    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @Getter
    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 15)
    private Estado estado = Estado.ACTIVA;

    @Getter
    @Setter
    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;


    public ModeloReserva(ModeloUsuario usuario) {
        this.usuario = usuario;
    }

    public ModeloReserva() {

    }


    //public @NotNull <U> LocalTime getHoraInicio() {
    //    return horaInicio;
    //}

    @Getter
    public enum Estado {
        ACTIVA, CANCELADA
    }


    //public ModeloReserva(ModeloUsuario usuario) {
    //}

    @PrePersist
    public void prePersist() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
        recalcularHoraFin();
    }

    @PreUpdate
    public void preUpdate() {
        recalcularHoraFin();
    }

    private void recalcularHoraFin() {
        if (horaInicio != null && duracionMinutos != null) {
            this.horaFin = horaInicio.plusMinutes(duracionMinutos);
        }
    }



}*/