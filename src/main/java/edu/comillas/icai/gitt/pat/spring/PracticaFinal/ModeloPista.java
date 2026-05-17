package edu.comillas.icai.gitt.pat.spring.PracticaFinal;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

/**
 * Entidad JPA que representa una pista de pádel en el sistema.
 *
 * <p>Se mapea a la tabla {@code pistas} en la base de datos. Cada instancia
 * corresponde a una pista física de las instalaciones.</p>
 *
 * <h3>Decisiones de diseño</h3>
 * <ul>
 *   <li><b>Constraint única en nombre</b>: la anotación {@code @UniqueConstraint}
 *       garantiza a nivel de BD que no pueden existir dos pistas con el mismo nombre,
 *       complementando la validación en código del controlador.</li>
 *   <li><b>{@code activa} (soft-delete)</b>: en lugar de borrar físicamente una pista
 *       (lo que eliminaría el historial de reservas), se desactiva con este flag.
 *       El DELETE del API simplemente pone {@code activa = false}.</li>
 *   <li><b>{@code @PrePersist}</b>: rellena automáticamente {@code fechaAlta} justo
 *       antes del primer INSERT, sin que el cliente tenga que enviarlo.</li>
 *   <li><b>{@code @JsonIgnore} en reservas</b>: evita la serialización circular;
 *       si se devolviese la lista de reservas dentro de la pista, cada reserva
 *       incluiría de nuevo la pista, creando un bucle infinito de JSON.</li>
 *   <li><b>Lombok {@code @Getter}/{@code @Setter} a nivel de clase</b>: genera todos
 *       los getters y setters automáticamente para todos los campos.</li>
 * </ul>
 */
@Getter
@Setter
@Entity
@Table(
        name = "pistas",
        uniqueConstraints = {
                // Garantiza que no haya dos pistas con el mismo nombre en BD
                @UniqueConstraint(name = "uk_pistas_nombre", columnNames = "nombre")
        }
)
public class ModeloPista {

    /** Clave primaria generada automáticamente por la base de datos (autoincremento). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pista", nullable = false)
    private Long idPista;

    /** Nombre identificativo de la pista (p.ej. "Pista 1"). Debe ser único en todo el sistema. */
    @NotBlank
    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    /** Descripción de dónde está ubicada la pista dentro de las instalaciones. */
    @NotBlank
    @Column(name = "ubicacion", nullable = false, length = 255)
    private String ubicacion;

    /**
     * Precio por hora de uso de la pista, en euros.
     * {@code BigDecimal} se usa para dinero porque evita errores de redondeo de {@code double}.
     * {@code @PositiveOrZero} impide precios negativos.
     */
    @NotNull
    @PositiveOrZero
    @Column(name = "precio_hora", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioHora;

    /**
     * Indica si la pista está disponible para ser reservada.
     * Una pista inactiva no aparece en resultados de disponibilidad.
     * Valor por defecto: {@code true} (activa al crearla).
     */
    @Column(name = "activa", nullable = false)
    private Boolean activa = true;

    /** Fecha y hora en que se dió de alta la pista. La rellena {@code @PrePersist} automáticamente. */
    @Column(name = "fecha_alta", nullable = false)
    private LocalDateTime fechaAlta;

    /**
     * Lista de reservas asociadas a esta pista.
     * {@code @JsonIgnore} evita la serialización circular pista → reservas → pista → ...
     */
    @OneToMany(mappedBy = "pista")
    @JsonIgnore
    private List<ModeloReserva> reservas;

    /** Constructor vacío requerido por JPA/Hibernate para instanciar la entidad via reflexión. */
    public ModeloPista() {
    }

    /**
     * Callback de JPA ejecutado automáticamente antes de cada INSERT en BD.
     * Rellena {@code fechaAlta} con el momento actual si no se ha establecido ya.
     */
    @PrePersist
    public void prePersist() {
        if (fechaAlta == null) {
            fechaAlta = LocalDateTime.now();
        }
    }
}
