package edu.comillas.icai.gitt.pat.spring.PracticaFinal.repository;


import edu.comillas.icai.gitt.pat.spring.PracticaFinal.ModeloUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio JPA para la entidad {@link ModeloUsuario}.
 *
 * <p>Al extender {@code JpaRepository<ModeloUsuario, Long>}, Spring genera automáticamente
 * en tiempo de arranque una implementación con los métodos CRUD básicos
 * ({@code save}, {@code findById}, {@code findAll}, {@code delete}, etc.).
 * No es necesario escribir ningún SQL para esas operaciones.</p>
 *
 * <p><b>Convención de nombres de Spring Data JPA</b>: los métodos como
 * {@code findByEmail} se traducen automáticamente a {@code SELECT * FROM modelo_usuario WHERE email = ?}.
 * Spring analiza el nombre del método para generar la query.</p>
 */
@Repository
public interface RepositorioUsuario extends JpaRepository<ModeloUsuario, Long> {

    /** Hereda de JpaRepository: devuelve todos los usuarios. */
    List<ModeloUsuario> findAll();

    /**
     * Busca un usuario por su email.
     * Spring genera: {@code SELECT u FROM ModeloUsuario u WHERE u.email = :email}
     *
     * @param email email a buscar
     * @return el usuario encontrado, o {@code null} si no existe
     */
    ModeloUsuario findByEmail(String email);

    /**
     * Comprueba si ya existe un usuario con ese email (para evitar registros duplicados).
     * Spring genera: {@code SELECT COUNT(u) > 0 FROM ModeloUsuario u WHERE u.email = :email}
     *
     * @param email email a comprobar
     * @return {@code true} si ya existe un usuario con ese email
     */
    boolean existsByEmail(String email);
}



