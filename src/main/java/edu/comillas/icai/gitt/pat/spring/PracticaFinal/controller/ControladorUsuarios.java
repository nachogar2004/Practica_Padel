package edu.comillas.icai.gitt.pat.spring.PracticaFinal.controller;

import edu.comillas.icai.gitt.pat.spring.PracticaFinal.ModeloRol;
import edu.comillas.icai.gitt.pat.spring.PracticaFinal.ModeloUsuario;
import edu.comillas.icai.gitt.pat.spring.PracticaFinal.repository.RepositorioUsuario;
import edu.comillas.icai.gitt.pat.spring.PracticaFinal.dto.LoginRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/pistaPadel")
public class ControladorUsuarios {

    private final RepositorioUsuario repositorioUsuario;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private static final String USUARIO_SESION = "USUARIO_LOGUEADO";

    // Inyección por constructor (como se ve en la lógica de Spring buena)
    public ControladorUsuarios(RepositorioUsuario repositorioUsuario) {
        this.repositorioUsuario = repositorioUsuario;
    }

    @PostMapping("/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ModeloUsuario register(@Valid @RequestBody ModeloUsuario usuario) {
        logger.info("Intentando registrar usuario con email: {}", usuario.getEmail());

        if (repositorioUsuario.existsByEmail(usuario.getEmail())) {
            logger.warn("El email {} ya está registrado", usuario.getEmail());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El email ya existe");
        }

        // Si el usuario no trae rol en el JSON, le ponemos USER por defecto
        if (usuario.getRol() == null) {
            usuario.setRol(ModeloRol.USER);
        }
        // Si trae un rol, dejamos el que viene (ADMIN o USER)
        // Spring Boot se encarga de convertir el String del JSON al Enum ModeloRol automáticamente

        return repositorioUsuario.save(usuario);
    }

    @PostMapping("/auth/login")
    public String login(@Valid @RequestBody LoginRequest loginRequest, HttpSession session) {
        ModeloUsuario usuario = repositorioUsuario.findByEmail(loginRequest.getEmail());

        if (usuario == null || !usuario.getPassword().equals(loginRequest.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas");
        }

        // Guardamos el email en la sesión
        session.setAttribute(USUARIO_SESION, usuario.getEmail());
        return "Login exitoso";
    }

    @PostMapping("/auth/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpSession session) {
        if (session.getAttribute(USUARIO_SESION) == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        session.invalidate();
        logger.info("Sesión cerrada correctamente");
    }

    @GetMapping("/auth/me")
    public ModeloUsuario obtenerPerfil(HttpSession session) {
        String email = (String) session.getAttribute(USUARIO_SESION);
        if (email == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No hay sesión activa");
        }

        ModeloUsuario usuario = repositorioUsuario.findByEmail(email);
        if (usuario == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado en la base de datos");
        }
        return usuario;
    }

    @PatchMapping("/users/{id}")
    public ModeloUsuario actualizarPerfil(@PathVariable Long id, HttpSession session, @RequestBody ModeloUsuario datosNuevos) {
        // 1. Obtener el email del usuario logueado desde la sesión
        String emailSesion = (String) session.getAttribute(USUARIO_SESION);
        if (emailSesion == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesión no válida");
        }

        // 2. Buscar al usuario que hace la petición (quién es) y al que se quiere modificar (destino)
        ModeloUsuario usuarioAutenticado = repositorioUsuario.findByEmail(emailSesion);
        ModeloUsuario usuarioDestino = repositorioUsuario.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario a modificar no encontrado"));

        // 3. SEGURIDAD: Solo puede editar si es ADMIN o si el ID de la URL es el SUYO (Dueño)
        boolean esAdmin = usuarioAutenticado.getRol() == ModeloRol.ADMIN;
        boolean esElDueno = usuarioAutenticado.getIdUsuario().equals(id);

        if (!esAdmin && !esElDueno) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso para modificar este perfil");
        }

        // 4. Actualización de campos (solo si vienen en el JSON)
        if (datosNuevos.getNombre() != null) usuarioDestino.setNombre(datosNuevos.getNombre());
        if (datosNuevos.getApellidos() != null) usuarioDestino.setApellidos(datosNuevos.getApellidos());
        if (datosNuevos.getTelefono() != null) usuarioDestino.setTelefono(datosNuevos.getTelefono());

        // El password se actualiza si el modelo permite WRITE_ONLY
        if (datosNuevos.getPassword() != null) usuarioDestino.setPassword(datosNuevos.getPassword());

        // 5. IMPORTANTE: Solo un ADMIN debería poder cambiarse el ROL a sí mismo o a otros
        if (datosNuevos.getRol() != null && esAdmin) {
            usuarioDestino.setRol(datosNuevos.getRol());
        }

        return repositorioUsuario.save(usuarioDestino);
    }

    @GetMapping("/users")
    public List<ModeloUsuario> listarTodosLosUsuarios(HttpSession session) {
        validarAdmin(session); // Comprueba que es admin
        return repositorioUsuario.findAll();
    }

    @GetMapping("/users/{id}")
    public ModeloUsuario obtenerUsuarioPorId(@PathVariable Long id, HttpSession session) {
        // 1. Verificar que hay una sesión activa
        String emailSesion = (String) session.getAttribute(USUARIO_SESION);
        if (emailSesion == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No hay sesión activa");
        }

        // 2. Buscar al usuario que hace la consulta y al usuario objetivo
        ModeloUsuario usuarioLogueado = repositorioUsuario.findByEmail(emailSesion);
        ModeloUsuario usuarioObjetivo = repositorioUsuario.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        // 3. Lógica de Seguridad: Admin o Dueño
        boolean esAdmin = usuarioLogueado.getRol() == ModeloRol.ADMIN;
        boolean esElDueno = usuarioLogueado.getIdUsuario().equals(id);

        if (!esAdmin && !esElDueno) {
            // Si no es admin ni es su propio ID, prohibimos el acceso
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso para ver este perfil");
        }

        // 4. Devolver los datos (Jackson omitirá la password por el @JsonProperty WRITE_ONLY)
        return usuarioObjetivo;
    }

    // Método útil para que otros controladores verifiquen el rol
    public void validarAdmin(HttpSession session) {
        String email = (String) session.getAttribute(USUARIO_SESION);
        if (email == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado");
        }
        ModeloUsuario usuario = repositorioUsuario.findByEmail(email);
        if (usuario == null || usuario.getRol() != ModeloRol.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acceso denegado: Se requiere ser Administrador");
        }
    }
}