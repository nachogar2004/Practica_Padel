package edu.comillas.icai.gitt.pat.spring.PracticaFinal.repository;

import edu.comillas.icai.gitt.pat.spring.PracticaFinal.ModeloPista;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Repositorio JPA para la entidad {@link ModeloPista}.
 *
 * <p>Hereda de {@code JpaRepository} obteniendo CRUD completo de forma automática.
 * Los métodos adicionales usan la convención de nombres de Spring Data JPA para
 * generar queries automáticamente según el nombre del método.</p>
 */
@Repository
public interface RepositorioPista extends JpaRepository<ModeloPista, Long> {

    /** Hereda de JpaRepository. Comprueba existencia por id. */
    boolean existsById(Long id);

    /**
     * Devuelve solo las pistas activas.
     * Spring genera: {@code SELECT p FROM ModeloPista p WHERE p.activa = true}
 */
    List<ModeloPista> findByActivaTrue();

    /**
     * Busca una pista por su nombre exacto.
     * Útil para comprobar duplicados antes de crear una nueva pista.
     *
     * @param nombre nombre exacto de la pista
     * @return la pista con ese nombre, o {@code null} si no existe
     */
    ModeloPista findByNombre(String nombre);

    /**
     * Devuelve pistas según su estado activa/inactiva.
     * Permite obtener tanto las activas ({@code true}) como las inactivas ({@code false}).
     *
     * @param activa {@code true} para pistas activas, {@code false} para inactivas
     */
    List<ModeloPista> findByActiva(boolean activa);

}
