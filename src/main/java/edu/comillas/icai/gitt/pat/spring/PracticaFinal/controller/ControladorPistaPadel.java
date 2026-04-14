package edu.comillas.icai.gitt.pat.spring.PracticaFinal.controller;

import edu.comillas.icai.gitt.pat.spring.PracticaFinal.*;
import edu.comillas.icai.gitt.pat.spring.PracticaFinal.dto.PistaPatchRequest;
import edu.comillas.icai.gitt.pat.spring.PracticaFinal.repository.RepositorioPista;
import edu.comillas.icai.gitt.pat.spring.PracticaFinal.repository.RepositorioUsuario;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/pistaPadel/courts")
public class ControladorPistaPadel {

    private final RepositorioPista repositorioPista;
    private final RepositorioUsuario repositorioUsuario;

    private static final String USUARIO_SESION = "USUARIO_LOGUEADO";

    public ControladorPistaPadel(RepositorioPista repositorioPista, RepositorioUsuario repositorioUsuario) {
        this.repositorioPista = repositorioPista;
        this.repositorioUsuario = repositorioUsuario;
    }

    // Método auxiliar para validar que el usuario es ADMIN
    private void validarAdmin(HttpSession session) {
        String email = (String) session.getAttribute(USUARIO_SESION);
        if (email == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Debes iniciar sesión");
        }

        ModeloUsuario usuario = repositorioUsuario.findByEmail(email);
        if (usuario == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado");
        }

        if (usuario.getRol() != ModeloRol.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permisos de administrador");
        }
    }

    // GET /pistaPadel/courts?active=true/false (Público)
    @GetMapping
    public List<ModeloPista> listarPistas(@RequestParam(required = false) Boolean active) {
        if (active != null) {
            return repositorioPista.findByActiva(active);
        }
        return (List<ModeloPista>) repositorioPista.findAll();
    }

    @GetMapping("/{courtId}")
    public ModeloPista obtenerPistaPorId(@PathVariable Long courtId) {
        return repositorioPista.findById(courtId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pista no encontrada"));
    }

    // POST /pistaPadel/courts (Solo ADMIN)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ModeloPista crearPista(@RequestBody ModeloPista pista, HttpSession session) {
        validarAdmin(session); // Comprobación de seguridad

        pista.setFechaAlta(LocalDateTime.now());
        return repositorioPista.save(pista);
    }

    // PATCH /pistaPadel/courts/{courtId} (Solo ADMIN)
    @PatchMapping("/{courtId}")
    public ModeloPista actualizarPista(
            @PathVariable Long courtId,
            @RequestBody PistaPatchRequest body,
            HttpSession session) {

        validarAdmin(session); // Comprobación de seguridad

        ModeloPista pista = repositorioPista.findById(courtId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pista no encontrada"));

        // Actualizar nombre
        if (body.getNombre() != null) {
            if (body.getNombre().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre no puede estar vacío");
            }
            pista.setNombre(body.getNombre());
        }

        // Actualizar ubicación
        if (body.getUbicacion() != null) {
            pista.setUbicacion(body.getUbicacion());
        }

        // Actualizar precio
        if (body.getPrecioHora() != null) {
            if (body.getPrecioHora().compareTo(BigDecimal.ZERO) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El precio no puede ser negativo");
            }
            pista.setPrecioHora(body.getPrecioHora());
        }

        // Actualizar estado activa
        if (body.getActiva() != null) {
            pista.setActiva(body.getActiva());
        }

        return repositorioPista.save(pista);
    }

    // DELETE /pistaPadel/courts/{courtId} (Solo ADMIN)
    @DeleteMapping("/{courtId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarPista(@PathVariable Long courtId, HttpSession session) {

        validarAdmin(session); // Comprobación de seguridad

        ModeloPista pista = repositorioPista.findById(courtId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pista no encontrada"));

        // En lugar de borrar, desactivamos la pista
        pista.setActiva(false);
        repositorioPista.save(pista);
    }
}
