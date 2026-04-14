package edu.comillas.icai.gitt.pat.spring.PracticaFinal.repository;

import edu.comillas.icai.gitt.pat.spring.PracticaFinal.ModeloPista;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RepositorioPista extends JpaRepository<ModeloPista, Long> {

    boolean existsById(Long id);
    List<ModeloPista> findByActivaTrue();
    ModeloPista findByNombre(String nombre);

    List<ModeloPista> findByActiva(boolean activa);

}
