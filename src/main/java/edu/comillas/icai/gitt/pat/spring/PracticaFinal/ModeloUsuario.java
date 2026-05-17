package edu.comillas.icai.gitt.pat.spring.PracticaFinal;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Entidad JPA que representa a un usuario registrado en el sistema de reservas.
 *
 * <p>Al estar anotada con {@code @Entity}, Hibernate gestiona automáticamente
 * la correspondencia entre esta clase y la tabla {@code modelo_usuario} en la
 * base de datos. Cada instancia de esta clase equivale a una fila en esa tabla.</p>
 *
 * <h3>Decisiones de diseño</h3>
 * <ul>
 *   <li><b>Contraseña con WRITE_ONLY</b>: el campo {@code password} está marcado
 *       con {@code @JsonProperty(Access.WRITE_ONLY)}, lo que significa que Spring
 *       puede leerlo al recibir JSON (registro/login) pero NUNCA lo incluye al
 *       serializar la respuesta. Así la contraseña nunca viaja al cliente.</li>
 *   <li><b>Rol como Enum</b>: en lugar de guardar un String libre, se usa el enum
 *       {@link ModeloRol} con {@code @Enumerated(EnumType.STRING)} para que la BD
 *       almacene "USER" o "ADMIN" de forma legible y segura.</li>
 *   <li><b>Soft-delete con {@code activo}</b>: en lugar de borrar físicamente el
 *       usuario, se marca {@code activo = false}. Esto preserva el historial de
 *       reservas asociadas.</li>
 *   <li><b>Lombok {@code @Getter}/{@code @Setter}</b>: generan automáticamente los
 *       métodos get/set de cada campo, evitando código repetitivo.</li>
 * </ul>
 */
@Entity
public class ModeloUsuario {

    /** Clave primaria generada autom\u00e1ticamente por la base de datos (autoincremento). */
    @Setter
    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario", nullable = false)
    private Long idUsuario;

    /** Nombre de pila del usuario. Obligatorio, m\u00e1ximo 100 caracteres. */
    @Setter
    @Getter
    @NotBlank
    @Size(max = 100)
    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    /** Apellidos del usuario. Obligatorio, m\u00e1ximo 150 caracteres. */
    @Setter
    @Getter
    @NotBlank
    @Size(max = 150)
    @Column(name = "apellidos", nullable = false, length = 150)
    private String apellidos;

    /** Email \u00fanico del usuario, usado como identificador en el login y en la sesi\u00f3n HTTP. */
    @Setter
    @Getter
    @NotBlank
    @Email
    @Size(max = 200)
    @Column(name = "email", nullable = false, length = 200)
    private String email;

    /**
     * Contrase\u00f1a del usuario.
     *
     * <p><b>WRITE_ONLY</b>: Jackson puede leer este campo al deserializar JSON entrante
     * (p.ej. en el registro), pero NUNCA lo incluye al serializar la respuesta.
     * Esto impide que la contrase\u00f1a viaje al navegador en ning\u00fan endpoint.</p>
     *
     * <p><b>NOTA</b>: en este proyecto la contrase\u00f1a se guarda en texto plano por simplicidad.
     * En producci\u00f3n se deber\u00eda hashear con BCrypt antes de persistir.</p>
     */
    @Setter
    @Getter
    @NotBlank
    @Size(max = 255)
    @Column(name = "password", nullable = false, length = 255)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    /** Tel\u00e9fono de contacto opcional. M\u00e1ximo 30 caracteres. */
    @Setter
    @Getter
    @Size(max = 30)
    @Column(name = "telefono", length = 30)
    private String telefono;

    /**
     * Rol del usuario en el sistema: USER o ADMIN.
     *
     * <p>{@code @Enumerated(EnumType.STRING)} hace que Hibernate guarde la cadena
     * "USER" o "ADMIN" en la BD, no su \u00edndice num\u00e9rico. As\u00ed la tabla es legible
     * por cualquier persona que consulte la BD directamente.</p>
     */
    @Setter
    @Getter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ModeloRol rol;

    /** Fecha y hora exacta en que se cre\u00f3 la cuenta. La fija el controlador al registrar. */
    @Setter
    @Getter
    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    /**
     * Indica si la cuenta est\u00e1 activa.
     *
     * <p><b>Soft-delete</b>: en lugar de borrar f\u00edsicamente el usuario de la BD
     * (lo que romper\u00eda las claves for\u00e1neas en la tabla de reservas), se desactiva
     * cambiando este campo a {@code false}. As\u00ed se conserva el historial.</p>
     */
    @Setter
    @Getter
    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    /**
     * Lista de reservas que ha realizado este usuario.
     *
     * <p>{@code @OneToMany(mappedBy = "usuario")}: indica la relaci\u00f3n uno-a-muchos.
     * "mappedBy" apunta al campo {@code usuario} dentro de {@link ModeloReserva},
     * que es quien tiene la clave for\u00e1nea en BD.</p>
     *
     * <p>{@code @JsonIgnore}: evita la serializaci\u00f3n circular infinita. Sin \u00e9l,
     * al serializar un Usuario se incluir\u00edan sus Reservas, cada Reserva incluir\u00eda
     * de nuevo el Usuario, y as\u00ed hasta un StackOverflowError.</p>
     */
    @OneToMany(mappedBy = "usuario")
    @JsonIgnore
    private List<ModeloReserva> reservas;

    /** Constructor vac\u00edo requerido por JPA/Hibernate para instanciar la entidad. */
    public ModeloUsuario() {
    }

    /**
     * Callback de JPA que se ejecuta autom\u00e1ticamente ANTES de insertar ({@code INSERT})
     * el registro en la BD. Rellena {@code fechaRegistro} si no se ha establecido ya.
     */
    @PrePersist
    public void prePersist() {
        if (fechaRegistro == null) {
            fechaRegistro = LocalDateTime.now();
        }
    }

}