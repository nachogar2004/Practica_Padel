package edu.comillas.icai.gitt.pat.spring.PracticaFinal;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "pistas",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_pistas_nombre", columnNames = "nombre")
        }
)
public class ModeloPista {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pista", nullable = false)
    private Long idPista;

    @Getter
    @NotBlank
    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @NotBlank
    @Column(name = "ubicacion", nullable = false, length = 255)
    private String ubicacion;

    @NotNull
    @PositiveOrZero
    @Column(name = "precio_hora", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioHora;

    @Column(name = "activa", nullable = false)
    private boolean activa = true;

    @Column(name = "fecha_alta", nullable = false)
    private LocalDateTime fechaAlta;

    @OneToMany(mappedBy = "pista")
    @JsonIgnore
    private List<ModeloReserva> reservas;

    public ModeloPista() {
    }

    @PrePersist
    public void prePersist() {
        if (fechaAlta == null) {
            fechaAlta = LocalDateTime.now();
        }
    }
}
