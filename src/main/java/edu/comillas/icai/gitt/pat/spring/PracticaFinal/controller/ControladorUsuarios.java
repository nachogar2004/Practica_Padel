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

/**
 * Controlador REST para la autenticación y gestión de usuarios.
 *
 * <p>Gestiona el ciclo de vida de la sesión (registro, login, logout, perfil)
 * y las operaciones CRUD de usuarios accesibles según el rol.</p>
 *
 * <h3>Autenticación</h3>
 * <p>Se usa autenticación basada en sesiones HTTP (no JWT). Tras el login, el email
 * del usuario se almacena en la sesión bajo la clave {@code "USUARIO_LOGUEADO"}.
 * El navegador envía automáticamente la cookie de sesión ({@code JSESSIONID}) en
 * cada petición posterior.</p>
 *
 * <h3>Endpoints</h3>
 * <ul>
 *   <li>{@code POST /pistaPadel/auth/register} — registro de nuevo usuario (HTTP 201)</li>
 *   <li>{@code POST /pistaPadel/auth/login} — inicio de sesión (crea sesión HTTP)</li>
 *   <li>{@code POST /pistaPadel/auth/logout} — cierre de sesión (invalida sesión)</li>
 *   <li>{@code GET /pistaPadel/auth/me} — devuelve el perfil del usuario autenticado</li>
 *   <li>{@code GET /pistaPadel/users} — listar todos los usuarios (solo ADMIN)</li>
 *   <li>{@code GET /pistaPadel/users/{id}} — obtener usuario por id (propio o ADMIN)</li>
 *   <li>{@code PATCH /pistaPadel/users/{id}} — actualizar perfil (propio o ADMIN)</li>
 * </ul>
 */
@RestController
@RequestMapping("/pistaPadel")
public class ControladorUsuarios {

    private final RepositorioUsuario repositorioUsuario;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    /** Clave usada en la sesión HTTP para almacenar el email del usuario autenticado. */
    private static final String USUARIO_SESION = "USUARIO_LOGUEADO";

    /** Inyección de dependencias por constructor (patrón recomendado por Spring). */
    public ControladorUsuarios(RepositorioUsuario repositorioUsuario) {
        this.repositorioUsuario = repositorioUsuario;
    }

    /**
     * Registra un nuevo usuario en el sistema.
     * Devuelve HTTP 201 Created con los datos del usuario guardado.
     *
     * @param usuario datos del nuevo usuario validados con Bean Validation ({@code @Valid})
     * @return el usuario guardado (con id asignado; la contraseña no se serializa por WRITE_ONLY)
     */
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

    /**
     * Autentica al usuario y crea una sesión HTTP.
     * Almacena el email del usuario en la sesión bajo la clave {@code USUARIO_LOGUEADO}.
     * Si las credenciales son incorrectas, devuelve HTTP 401.
     *
     * @param loginRequest email y contraseña del usuario
     * @param session      sesión HTTP donde se guardará el email
     * @return el usuario autenticado (sin contraseña, por {@code @JsonProperty(WRITE_ONLY)})
     */
    @PostMapping("/auth/login")
    public ModeloUsuario login(@Valid @RequestBody LoginRequest loginRequest, HttpSession session) {
        ModeloUsuario usuario = repositorioUsuario.findByEmail(loginRequest.getEmail());

        if (usuario == null || !usuario.getPassword().equals(loginRequest.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas");
        }

        // Guardamos el email en la sesión
        session.setAttribute(USUARIO_SESION, usuario.getEmail());
        return usuario;
    }

    /**
     * Cierra la sesión del usuario autenticado.
     * Llama a {@code session.invalidate()} para eliminar todos los datos de sesión.
     * Devuelve HTTP 204 No Content.
     *
     * @param session sesión HTTP a invalidar
     */
    @PostMapping("/auth/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpSession session) {
        if (session.getAttribute(USUARIO_SESION) == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        session.invalidate();
        logger.info("Sesión cerrada correctamente");
    }

    /**
     * Devuelve el perfil del usuario actualmente autenticado.
     * Utiliza el email almacenado en sesión para recuperar sus datos de la BD.
     *
     * @param session sesión HTTP activa
     * @return datos del usuario autenticado (sin contraseña, por {@code @JsonProperty(WRITE_ONLY)})
     */
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

    /**
     * Actualiza parcialmente el perfil de un usuario.
     * Solo puede hacerlo el propio usuario (dueño) o un ADMIN.
     * Los campos nulos en el cuerpo JSON no se modifican.
     * El rol solo puede ser cambiado por un ADMIN.
     *
     * @param id          id del usuario a modificar
     * @param session     sesión HTTP
     * @param datosNuevos campos a actualizar
     * @return el usuario con los cambios guardados
     */
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

    /**
     * Lista todos los usuarios del sistema. Solo accesible por ADMIN.
     *
     * @param session sesión HTTP (debe pertenecer a un ADMIN)
     * @return lista de todos los usuarios registrados
     */
    @GetMapping("/users")
    public List<ModeloUsuario> listarTodosLosUsuarios(HttpSession session) {
        validarAdmin(session); // Comprueba que es admin
        return repositorioUsuario.findAll();
    }

    /**
     * Devuelve los datos de un usuario por su id.
     * Solo puede verlo el propio usuario o un ADMIN.
     *
     * @param id      id del usuario a consultar
     * @param session sesión HTTP activa
     * @return los datos del usuario (sin contraseña)
     */
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

    /**
     * Método auxiliar para verificar que el usuario de la sesión tiene rol ADMIN.
     * Se reutiliza en este controlador y puede ser llamado desde otros.
     * Lanza HTTP 401 si no hay sesión activa, o HTTP 403 si no es ADMIN.
     *
     * @param session sesión HTTP de la petición
     */
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