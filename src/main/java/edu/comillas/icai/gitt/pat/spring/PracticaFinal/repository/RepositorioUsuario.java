package edu.comillas.icai.gitt.pat.spring.PracticaFinal.repository;


import edu.comillas.icai.gitt.pat.spring.PracticaFinal.ModeloUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RepositorioUsuario extends JpaRepository<ModeloUsuario, Long> {
    List<ModeloUsuario> findAll();

    ModeloUsuario findByEmail(String email);
    boolean existsByEmail(String email);
}



