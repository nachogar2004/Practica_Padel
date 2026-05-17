package edu.comillas.icai.gitt.pat.spring.PracticaFinal;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import edu.comillas.icai.gitt.pat.spring.PracticaFinal.repository.RepositorioUsuario;

/**
 * Tests de integración end-to-end para el {@code ControladorUsuarios}.
 *
 * <p>Cubre todos los endpoints:</p>
 * <ul>
 *   <li>{@code POST /pistaPadel/auth/register} — registro</li>
 *   <li>{@code POST /pistaPadel/auth/login} — login</li>
 *   <li>{@code POST /pistaPadel/auth/logout} — logout</li>
 *   <li>{@code GET  /pistaPadel/auth/me} — perfil propio</li>
 *   <li>{@code GET  /pistaPadel/users} — listar todos (solo ADMIN)</li>
 *   <li>{@code GET  /pistaPadel/users/{id}} — obtener por id (propio o ADMIN)</li>
 *   <li>{@code PATCH /pistaPadel/users/{id}} — actualizar perfil (propio o ADMIN)</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ControladorUsuariosIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private RepositorioUsuario repoUsuario;

    private Long idAdmin;
    private Long idUser;

    /** Crea un ADMIN y un USER antes de cada test. */
    @BeforeEach
    void setup() {
        repoUsuario.deleteAll();

        ModeloUsuario admin = new ModeloUsuario();
        admin.setNombre("Admin");
        admin.setApellidos("Test");
        admin.setEmail("admin@test.com");
        admin.setPassword("admin1234");
        admin.setRol(ModeloRol.ADMIN);
        admin.setFechaRegistro(LocalDateTime.now());
        idAdmin = repoUsuario.save(admin).getIdUsuario();

        ModeloUsuario user = new ModeloUsuario();
        user.setNombre("User");
        user.setApellidos("Test");
        user.setEmail("user@test.com");
        user.setPassword("user1234");
        user.setRol(ModeloRol.USER);
        user.setFechaRegistro(LocalDateTime.now());
        idUser = repoUsuario.save(user).getIdUsuario();
    }

    // -------------------------------------------------------------------------
    // Helpers de login
    // -------------------------------------------------------------------------

    private HttpHeaders loginAdmin() {
        return login("admin@test.com", "admin1234");
    }

    private HttpHeaders loginUser() {
        return login("user@test.com", "user1234");
    }

    private HttpHeaders login(String email, String password) {
        String body = "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/pistaPadel/auth/login",
                new HttpEntity<>(body, headers),
                String.class
        );

        String cookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        HttpHeaders auth = new HttpHeaders();
        auth.setContentType(MediaType.APPLICATION_JSON);
        auth.set(HttpHeaders.COOKIE, cookie);
        return auth;
    }

    // -------------------------------------------------------------------------
    // POST /pistaPadel/auth/register
    // -------------------------------------------------------------------------

    /** Registrar un usuario nuevo devuelve HTTP 201 y el usuario creado. */
    @Test
    void registrarUsuarioNuevoDevuelve201() {
        String json = "{\"nombre\":\"Nuevo\",\"apellidos\":\"Usuario\","
                + "\"email\":\"nuevo@test.com\",\"password\":\"pass1234\"}";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<ModeloUsuario> response = restTemplate.postForEntity(
                "/pistaPadel/auth/register",
                new HttpEntity<>(json, headers),
                ModeloUsuario.class
        );

        Assertions.assertEquals(HttpStatus.CREATED, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertNotNull(response.getBody().getIdUsuario());
        Assertions.assertEquals("Nuevo", response.getBody().getNombre());
        Assertions.assertEquals(ModeloRol.USER, response.getBody().getRol(),
                "El rol por defecto debe ser USER");
    }

    /** Registrar con un email ya existente devuelve HTTP 409. */
    @Test
    void registrarEmailDuplicadoDevuelve409() {
        String json = "{\"nombre\":\"Dup\",\"apellidos\":\"Test\","
                + "\"email\":\"user@test.com\",\"password\":\"otra\"}";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/pistaPadel/auth/register",
                new HttpEntity<>(json, headers),
                String.class
        );

        Assertions.assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    /** Registrar sin nombre (campo obligatorio) devuelve HTTP 400. */
    @Test
    void registrarSinNombreDevuelve400() {
        String json = "{\"apellidos\":\"Test\",\"email\":\"sin@nombre.com\",\"password\":\"1234\"}";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/pistaPadel/auth/register",
                new HttpEntity<>(json, headers),
                String.class
        );

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    // -------------------------------------------------------------------------
    // POST /pistaPadel/auth/login
    // -------------------------------------------------------------------------

    /** Login con credenciales correctas devuelve HTTP 200. */
    @Test
    void loginCorrectoDevuelve200() {
        String json = "{\"email\":\"user@test.com\",\"password\":\"user1234\"}";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<ModeloUsuario> response = restTemplate.postForEntity(
                "/pistaPadel/auth/login",
                new HttpEntity<>(json, headers),
                ModeloUsuario.class
        );

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertEquals("user@test.com", response.getBody().getEmail());
        // La contraseña no debe aparecer en la respuesta (WRITE_ONLY)
        Assertions.assertNull(response.getBody().getPassword(),
                "La contraseña no debe serializarse en la respuesta");
    }

    /** Login con contraseña incorrecta devuelve HTTP 401. */
    @Test
    void loginPasswordIncorrectaDevuelve401() {
        String json = "{\"email\":\"user@test.com\",\"password\":\"incorrecta\"}";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/pistaPadel/auth/login",
                new HttpEntity<>(json, headers),
                String.class
        );

        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    /** Login con email inexistente devuelve HTTP 401. */
    @Test
    void loginEmailInexistenteDevuelve401() {
        String json = "{\"email\":\"noexiste@test.com\",\"password\":\"1234\"}";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/pistaPadel/auth/login",
                new HttpEntity<>(json, headers),
                String.class
        );

        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    // -------------------------------------------------------------------------
    // POST /pistaPadel/auth/logout
    // -------------------------------------------------------------------------

    /** Logout con sesión activa devuelve HTTP 204. */
    @Test
    void logoutConSesionActivaDevuelve204() {
        HttpHeaders headers = loginUser();

        ResponseEntity<Void> response = restTemplate.exchange(
                "/pistaPadel/auth/logout",
                HttpMethod.POST,
                new HttpEntity<>(headers),
                Void.class
        );

        Assertions.assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    /** Logout sin sesión activa devuelve HTTP 401. */
    @Test
    void logoutSinSesionDevuelve401() {
        HttpHeaders headers = new HttpHeaders();

        ResponseEntity<String> response = restTemplate.exchange(
                "/pistaPadel/auth/logout",
                HttpMethod.POST,
                new HttpEntity<>(headers),
                String.class
        );

        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    // -------------------------------------------------------------------------
    // GET /pistaPadel/auth/me
    // -------------------------------------------------------------------------

    /** Obtener el perfil propio devuelve HTTP 200 con los datos del usuario. */
    @Test
    void obtenerPerfilPropioDevuelveOk() {
        HttpHeaders headers = loginUser();

        ResponseEntity<ModeloUsuario> response = restTemplate.exchange(
                "/pistaPadel/auth/me",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                ModeloUsuario.class
        );

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertEquals("user@test.com", response.getBody().getEmail());
    }

    /** Acceder a /me sin sesión devuelve HTTP 401. */
    @Test
    void obtenerPerfilSinSesionDevuelve401() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/pistaPadel/auth/me",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                String.class
        );

        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    // -------------------------------------------------------------------------
    // GET /pistaPadel/users — listar todos (solo ADMIN)
    // -------------------------------------------------------------------------

    /** Un ADMIN puede listar todos los usuarios (HTTP 200). */
    @Test
    void listarUsuariosComoAdminDevuelveOk() {
        HttpHeaders headers = loginAdmin();

        ResponseEntity<List> response = restTemplate.exchange(
                "/pistaPadel/users",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                List.class
        );

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertEquals(2, response.getBody().size(),
                "Deben aparecer el ADMIN y el USER creados en el setup");
    }

    /** Un USER no puede listar todos los usuarios (HTTP 403). */
    @Test
    void listarUsuariosComoUserDevuelve403() {
        HttpHeaders headers = loginUser();

        ResponseEntity<String> response = restTemplate.exchange(
                "/pistaPadel/users",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        Assertions.assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    /** Listar usuarios sin sesión devuelve HTTP 401. */
    @Test
    void listarUsuariosSinSesionDevuelve401() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/pistaPadel/users",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                String.class
        );

        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    // -------------------------------------------------------------------------
    // GET /pistaPadel/users/{id} — obtener por id
    // -------------------------------------------------------------------------

    /** Un USER puede ver su propio perfil por id (HTTP 200). */
    @Test
    void obtenerUsuarioPropioDevuelveOk() {
        HttpHeaders headers = loginUser();

        ResponseEntity<ModeloUsuario> response = restTemplate.exchange(
                "/pistaPadel/users/" + idUser,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                ModeloUsuario.class
        );

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertEquals(idUser, response.getBody().getIdUsuario());
    }

    /** Un ADMIN puede ver el perfil de cualquier usuario (HTTP 200). */
    @Test
    void adminPuedeVerCualquierUsuario() {
        HttpHeaders headers = loginAdmin();

        ResponseEntity<ModeloUsuario> response = restTemplate.exchange(
                "/pistaPadel/users/" + idUser,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                ModeloUsuario.class
        );

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertEquals(idUser, response.getBody().getIdUsuario());
    }

    /** Un USER no puede ver el perfil de otro usuario (HTTP 403). */
    @Test
    void userNoPuedeVerOtroUsuarioDevuelve403() {
        HttpHeaders headers = loginUser();

        ResponseEntity<String> response = restTemplate.exchange(
                "/pistaPadel/users/" + idAdmin,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        Assertions.assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    /** Buscar un id inexistente devuelve HTTP 404. */
    @Test
    void obtenerUsuarioInexistenteDevuelve404() {
        HttpHeaders headers = loginAdmin();

        ResponseEntity<String> response = restTemplate.exchange(
                "/pistaPadel/users/9999",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        Assertions.assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    // -------------------------------------------------------------------------
    // PATCH /pistaPadel/users/{id} — actualizar perfil
    // -------------------------------------------------------------------------

    /** Un USER puede actualizar su propio nombre (HTTP 200). */
    @Test
    void userActualizaSuPropioNombreDevuelveOk() {
        HttpHeaders headers = loginUser();
        String patch = "{\"nombre\":\"NombreActualizado\"}";

        ResponseEntity<ModeloUsuario> response = restTemplate.exchange(
                "/pistaPadel/users/" + idUser,
                HttpMethod.PATCH,
                new HttpEntity<>(patch, headers),
                ModeloUsuario.class
        );

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertEquals("NombreActualizado", response.getBody().getNombre());
    }

    /** Un ADMIN puede actualizar el perfil de otro usuario (HTTP 200). */
    @Test
    void adminActualizaOtroUsuarioDevuelveOk() {
        HttpHeaders headers = loginAdmin();
        String patch = "{\"nombre\":\"ModificadoPorAdmin\"}";

        ResponseEntity<ModeloUsuario> response = restTemplate.exchange(
                "/pistaPadel/users/" + idUser,
                HttpMethod.PATCH,
                new HttpEntity<>(patch, headers),
                ModeloUsuario.class
        );

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertEquals("ModificadoPorAdmin", response.getBody().getNombre());
    }

    /** Un USER no puede actualizar el perfil de otro usuario (HTTP 403). */
    @Test
    void userNoPuedeActualizarOtroUsuarioDevuelve403() {
        HttpHeaders headers = loginUser();
        String patch = "{\"nombre\":\"Intruso\"}";

        ResponseEntity<String> response = restTemplate.exchange(
                "/pistaPadel/users/" + idAdmin,
                HttpMethod.PATCH,
                new HttpEntity<>(patch, headers),
                String.class
        );

        Assertions.assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    /** Un ADMIN puede cambiar el rol de un USER a ADMIN. */
    @Test
    void adminPuedeCambiarRolDeUser() {
        HttpHeaders headers = loginAdmin();
        String patch = "{\"rol\":\"ADMIN\"}";

        ResponseEntity<ModeloUsuario> response = restTemplate.exchange(
                "/pistaPadel/users/" + idUser,
                HttpMethod.PATCH,
                new HttpEntity<>(patch, headers),
                ModeloUsuario.class
        );

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertEquals(ModeloRol.ADMIN, response.getBody().getRol());
    }

    /** Un USER no puede cambiar su propio rol (el campo rol se ignora si no es ADMIN). */
    @Test
    void userNoPuedeCambiarSuPropioRol() {
        HttpHeaders headers = loginUser();
        String patch = "{\"rol\":\"ADMIN\"}";

        ResponseEntity<ModeloUsuario> response = restTemplate.exchange(
                "/pistaPadel/users/" + idUser,
                HttpMethod.PATCH,
                new HttpEntity<>(patch, headers),
                ModeloUsuario.class
        );

        // La petición es aceptada (es su propio perfil) pero el rol NO debe cambiar
        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertEquals(ModeloRol.USER, response.getBody().getRol(),
                "Un USER no debe poder elevarse a ADMIN");
    }

    /** PATCH sin sesión devuelve HTTP 401. */
    @Test
    void patchSinSesionDevuelve401() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String patch = "{\"nombre\":\"Anónimo\"}";

        ResponseEntity<String> response = restTemplate.exchange(
                "/pistaPadel/users/" + idUser,
                HttpMethod.PATCH,
                new HttpEntity<>(patch, headers),
                String.class
        );

        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }
}
