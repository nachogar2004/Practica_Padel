package edu.comillas.icai.gitt.pat.spring.PracticaFinal.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO (Data Transfer Object) con los datos de inicio de sesión.
 *
 * <p>Se recibe en el cuerpo JSON del endpoint {@code POST /pistaPadel/auth/login}.
 * Un DTO es un objeto cuyo único propósito es transportar datos entre el cliente y el servidor;
 * no contiene lógica de negocio.</p>
 *
 * <p>Las anotaciones de Bean Validation ({@code @NotBlank}, {@code @Email}) se comprueban
 * automáticamente cuando el controlador usa {@code @Valid} en el parámetro del método.</p>
 *
 * <p>Lombok genera los getters, setters y constructores automáticamente:
 * {@code @NoArgsConstructor} crea el constructor vacío (necesario para Jackson al
 * deserializar JSON), {@code @AllArgsConstructor} crea un constructor con todos los campos.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    /** Email del usuario. No puede estar en blanco y debe tener formato válido (usuario@dominio.com). */
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Formato de email inválido")
    private String email;

    /** Contraseña del usuario en texto plano. No puede estar en blanco. */
    @NotBlank(message = "La contraseña es obligatoria")
    private String password;
}
