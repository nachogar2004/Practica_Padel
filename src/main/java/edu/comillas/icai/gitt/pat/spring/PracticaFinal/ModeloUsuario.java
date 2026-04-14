package edu.comillas.icai.gitt.pat.spring.PracticaFinal;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;


@Entity
public class ModeloUsuario {

    @Setter
    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario", nullable = false)
    private Long idUsuario;

    @Setter
    @Getter
    @NotBlank
    @Size(max = 100)
    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Setter
    @Getter
    @NotBlank
    @Size(max = 150)
    @Column(name = "apellidos", nullable = false, length = 150)
    private String apellidos;

    @Setter
    @Getter
    @NotBlank
    @Email
    @Size(max = 200)
    @Column(name = "email", nullable = false, length = 200)
    private String email;

    // Guardar siempre hash, nunca texto plano
    @Setter
    @Getter
    @NotBlank
    @Size(max = 255)
    @Column(name = "password", nullable = false, length = 255)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @Setter
    @Getter
    @Size(max = 30)
    @Column(name = "telefono", length = 30)
    private String telefono;

    @Setter
    @Getter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ModeloRol rol;


    @Setter
    @Getter
    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    @Setter
    @Getter
    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    // 0..n reservas
    // Cuando creeis ModeloReserva, ajustad mappedBy al nombre del atributo en Reserva.
    @OneToMany(mappedBy = "usuario")
    @JsonIgnore
    private List<ModeloReserva> reservas;



    public ModeloUsuario() {
    }

    @PrePersist
    public void prePersist() {
        if (fechaRegistro == null) {
            fechaRegistro = LocalDateTime.now();
        }
    }

}