package edu.comillas.icai.gitt.pat.spring.PracticaFinal;

import java.math.BigDecimal;
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

import edu.comillas.icai.gitt.pat.spring.PracticaFinal.repository.RepositorioPista;
import edu.comillas.icai.gitt.pat.spring.PracticaFinal.repository.RepositorioUsuario;

/**
 * Tests de integración end-to-end para el {@code ControladorPistaPadel}.
 *
 * <p>Levanta un servidor HTTP real en un puerto aleatorio y realiza peticiones
 * con {@code TestRestTemplate}, cubriendo todos los endpoints del controlador:</p>
 * <ul>
 *   <li>{@code GET /pistaPadel/courts} — listar pistas (público)</li>
 *   <li>{@code GET /pistaPadel/courts?active=true|false} — filtrar por estado (público)</li>
 *   <li>{@code GET /pistaPadel/courts/{id}} — obtener pista por id (público)</li>
 *   <li>{@code POST /pistaPadel/courts} — crear pista (solo ADMIN)</li>
 *   <li>{@code PATCH /pistaPadel/courts/{id}} — actualizar pista (solo ADMIN)</li>
 *   <li>{@code DELETE /pistaPadel/courts/{id}} — desactivar pista (solo ADMIN)</li>
 * </ul>
 *
 * <p>Se crean dos usuarios: uno ADMIN (para operaciones de escritura) y uno USER
 * (para verificar que las operaciones de escritura devuelven HTTP 403).</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ControladorPistaPadelIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private RepositorioUsuario repoUsuario;

    @Autowired
    private RepositorioPista repoPista;

    private Long idPistaExistente;

    /** Crea usuarios de prueba (ADMIN y USER) y una pista antes de cada test. */
    @BeforeEach
    void setup() {
        repoUsuario.deleteAll();
        repoPista.deleteAll();

        // Usuario con rol ADMIN para las operaciones de escritura
        ModeloUsuario admin = new ModeloUsuario();
        admin.setNombre("Admin");
        admin.setApellidos("Test");
        admin.setEmail("admin@test.com");
        admin.setPassword("admin1234");
        admin.setRol(ModeloRol.ADMIN);
        admin.setFechaRegistro(LocalDateTime.now());
        repoUsuario.save(admin);

        // Usuario con rol USER para verificar restricciones de acceso
        ModeloUsuario user = new ModeloUsuario();
        user.setNombre("User");
        user.setApellidos("Test");
        user.setEmail("user@test.com");
        user.setPassword("user1234");
        user.setRol(ModeloRol.USER);
        user.setFechaRegistro(LocalDateTime.now());
        repoUsuario.save(user);

        // Pista de prueba disponible en todos los tests
        ModeloPista pista = new ModeloPista();
        pista.setNombre("Pista Test");
        pista.setUbicacion("Zona Sur");
        pista.setPrecioHora(new BigDecimal("25.00"));
        pista.setFechaAlta(LocalDateTime.now());
        idPistaExistente = repoPista.save(pista).getIdPista();
    }

    // -------------------------------------------------------------------------
    // Métodos auxiliares de login
    // -------------------------------------------------------------------------

    /**
     * Hace login como ADMIN y devuelve los headers con la cookie de sesión.
     */
    private HttpHeaders loginAdmin() {
        return login("admin@test.com", "admin1234");
    }

    /**
     * Hace login como USER normal y devuelve los headers con la cookie de sesión.
     */
    private HttpHeaders loginUser() {
        return login("user@test.com", "user1234");
    }

    /**
     * Método genérico de login: envía credenciales y extrae la cookie {@code JSESSIONID}.
     */
    private HttpHeaders login(String email, String password) {
        String loginJson = "{\"email\":\"" + email + "\", \"password\":\"" + password + "\"}";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/pistaPadel/auth/login",
                new HttpEntity<>(loginJson, headers),
                String.class
        );

        String cookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        HttpHeaders authHeaders = new HttpHeaders();
        authHeaders.setContentType(MediaType.APPLICATION_JSON);
        authHeaders.set(HttpHeaders.COOKIE, cookie);
        return authHeaders;
    }

    // -------------------------------------------------------------------------
    // GET /pistaPadel/courts — listar pistas
    // -------------------------------------------------------------------------

    /**
     * Verifica que el endpoint GET devuelve HTTP 200 y la lista de pistas.
     * No requiere autenticación (endpoint público).
     */
    @Test
    void listarPistasDevuelveOkConPistas() {
        ResponseEntity<List> response = restTemplate.exchange(
                "/pistaPadel/courts",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                List.class
        );

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertFalse(response.getBody().isEmpty(), "Debería haber al menos una pista");
    }

    /**
     * Verifica que el filtro {@code ?active=true} devuelve solo pistas activas.
     */
    @Test
    void listarPistasFiltradoPorActivaTrue() {
        ResponseEntity<List> response = restTemplate.exchange(
                "/pistaPadel/courts?active=true",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                List.class
        );

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());
        // La pista creada en @BeforeEach está activa por defecto
        Assertions.assertFalse(response.getBody().isEmpty());
    }

    /**
     * Verifica que el filtro {@code ?active=false} devuelve lista vacía
     * (no hay pistas inactivas en el setup).
     */
    @Test
    void listarPistasFiltradoPorActivaFalse() {
        ResponseEntity<List> response = restTemplate.exchange(
                "/pistaPadel/courts?active=false",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                List.class
        );

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertTrue(response.getBody().isEmpty(), "No debería haber pistas inactivas");
    }

    // -------------------------------------------------------------------------
    // GET /pistaPadel/courts/{courtId} — obtener una pista por id
    // -------------------------------------------------------------------------

    /**
     * Verifica que se puede obtener una pista existente por su id.
     * Endpoint público, no requiere autenticación.
     */
    @Test
    void obtenerPistaPorIdExistenteDevuelveOk() {
        ResponseEntity<ModeloPista> response = restTemplate.exchange(
                "/pistaPadel/courts/" + idPistaExistente,
                HttpMethod.GET,
                HttpEntity.EMPTY,
                ModeloPista.class
        );

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertEquals(idPistaExistente, response.getBody().getIdPista());
        Assertions.assertEquals("Pista Test", response.getBody().getNombre());
    }

    /**
     * Verifica que buscar una pista con id inexistente devuelve HTTP 404.
     */
    @Test
    void obtenerPistaPorIdInexistenteDevuelve404() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/pistaPadel/courts/9999",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                String.class
        );

        Assertions.assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    // -------------------------------------------------------------------------
    // POST /pistaPadel/courts — crear pista
    // -------------------------------------------------------------------------

    /**
     * Verifica que un ADMIN puede crear una pista nueva y recibe HTTP 201.
     */
    @Test
    void crearPistaComoAdminDevuelve201() {
        HttpHeaders headers = loginAdmin();
        String nuevaPistaJson = "{\"nombre\":\"Pista Nueva\", \"ubicacion\":\"Zona Norte\", \"precioHora\":30.00}";

        ResponseEntity<ModeloPista> response = restTemplate.exchange(
                "/pistaPadel/courts",
                HttpMethod.POST,
                new HttpEntity<>(nuevaPistaJson, headers),
                ModeloPista.class
        );

        Assertions.assertEquals(HttpStatus.CREATED, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertNotNull(response.getBody().getIdPista(), "Debe tener un id generado");
        Assertions.assertEquals("Pista Nueva", response.getBody().getNombre());
    }

    /**
     * Verifica que crear una pista sin estar autenticado devuelve HTTP 401.
     */
    @Test
    void crearPistaSinAutenticacionDevuelve401() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String nuevaPistaJson = "{\"nombre\":\"Pista Sin Auth\", \"ubicacion\":\"Zona Este\", \"precioHora\":15.00}";

        ResponseEntity<String> response = restTemplate.exchange(
                "/pistaPadel/courts",
                HttpMethod.POST,
                new HttpEntity<>(nuevaPistaJson, headers),
                String.class
        );

        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    /**
     * Verifica que un usuario con rol USER no puede crear pistas (HTTP 403).
     */
    @Test
    void crearPistaComoUserDevuelve403() {
        HttpHeaders headers = loginUser();
        String nuevaPistaJson = "{\"nombre\":\"Pista Prohibida\", \"ubicacion\":\"Zona Oeste\", \"precioHora\":20.00}";

        ResponseEntity<String> response = restTemplate.exchange(
                "/pistaPadel/courts",
                HttpMethod.POST,
                new HttpEntity<>(nuevaPistaJson, headers),
                String.class
        );

        Assertions.assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    // -------------------------------------------------------------------------
    // PATCH /pistaPadel/courts/{courtId} — actualizar pista
    // -------------------------------------------------------------------------

    /**
     * Verifica que un ADMIN puede actualizar el nombre de una pista existente.
     */
    @Test
    void actualizarNombrePistaComoAdminDevuelveOk() {
        HttpHeaders headers = loginAdmin();
        String patchJson = "{\"nombre\":\"Pista Renombrada\"}";

        ResponseEntity<ModeloPista> response = restTemplate.exchange(
                "/pistaPadel/courts/" + idPistaExistente,
                HttpMethod.PATCH,
                new HttpEntity<>(patchJson, headers),
                ModeloPista.class
        );

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertEquals("Pista Renombrada", response.getBody().getNombre());
    }

    /**
     * Verifica que un ADMIN puede actualizar el precio de una pista.
     */
    @Test
    void actualizarPrecioPistaComoAdminDevuelveOk() {
        HttpHeaders headers = loginAdmin();
        String patchJson = "{\"precioHora\":99.99}";

        ResponseEntity<ModeloPista> response = restTemplate.exchange(
                "/pistaPadel/courts/" + idPistaExistente,
                HttpMethod.PATCH,
                new HttpEntity<>(patchJson, headers),
                ModeloPista.class
        );

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertEquals(new BigDecimal("99.99"), response.getBody().getPrecioHora());
    }

    /**
     * Verifica que hacer PATCH con un precio negativo devuelve HTTP 400.
     */
    @Test
    void actualizarPistaConPrecioNegativoDevuelve400() {
        HttpHeaders headers = loginAdmin();
        String patchJson = "{\"precioHora\":-10.00}";

        ResponseEntity<String> response = restTemplate.exchange(
                "/pistaPadel/courts/" + idPistaExistente,
                HttpMethod.PATCH,
                new HttpEntity<>(patchJson, headers),
                String.class
        );

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    /**
     * Verifica que hacer PATCH sobre una pista inexistente devuelve HTTP 404.
     */
    @Test
    void actualizarPistaInexistenteDevuelve404() {
        HttpHeaders headers = loginAdmin();
        String patchJson = "{\"nombre\":\"No existe\"}";

        ResponseEntity<String> response = restTemplate.exchange(
                "/pistaPadel/courts/9999",
                HttpMethod.PATCH,
                new HttpEntity<>(patchJson, headers),
                String.class
        );

        Assertions.assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    /**
     * Verifica que un usuario con rol USER no puede hacer PATCH (HTTP 403).
     */
    @Test
    void actualizarPistaComoUserDevuelve403() {
        HttpHeaders headers = loginUser();
        String patchJson = "{\"nombre\":\"Intento USER\"}";

        ResponseEntity<String> response = restTemplate.exchange(
                "/pistaPadel/courts/" + idPistaExistente,
                HttpMethod.PATCH,
                new HttpEntity<>(patchJson, headers),
                String.class
        );

        Assertions.assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    // -------------------------------------------------------------------------
    // DELETE /pistaPadel/courts/{courtId} — desactivar pista (soft-delete)
    // -------------------------------------------------------------------------

    /**
     * Verifica que un ADMIN puede desactivar una pista (soft-delete) y recibe HTTP 204.
     * Después comprueba que la pista ya no aparece en la lista de activas.
     */
    @Test
    void eliminarPistaComoAdminDevuelve204YLaDesactiva() {
        HttpHeaders headers = loginAdmin();

        // DELETE → debería devolver 204 No Content
        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                "/pistaPadel/courts/" + idPistaExistente,
                HttpMethod.DELETE,
                new HttpEntity<>(headers),
                Void.class
        );

        Assertions.assertEquals(HttpStatus.NO_CONTENT, deleteResponse.getStatusCode());

        // Verificar que la pista ya no aparece como activa
        ResponseEntity<List> getResponse = restTemplate.exchange(
                "/pistaPadel/courts?active=true",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                List.class
        );

        Assertions.assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        Assertions.assertTrue(getResponse.getBody().isEmpty(), "La pista desactivada no debería aparecer en activas");
    }

    /**
     * Verifica que intentar borrar una pista inexistente devuelve HTTP 404.
     */
    @Test
    void eliminarPistaInexistenteDevuelve404() {
        HttpHeaders headers = loginAdmin();

        ResponseEntity<String> response = restTemplate.exchange(
                "/pistaPadel/courts/9999",
                HttpMethod.DELETE,
                new HttpEntity<>(headers),
                String.class
        );

        Assertions.assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    /**
     * Verifica que un usuario con rol USER no puede eliminar pistas (HTTP 403).
     */
    @Test
    void eliminarPistaComoUserDevuelve403() {
        HttpHeaders headers = loginUser();

        ResponseEntity<String> response = restTemplate.exchange(
                "/pistaPadel/courts/" + idPistaExistente,
                HttpMethod.DELETE,
                new HttpEntity<>(headers),
                String.class
        );

        Assertions.assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    /**
     * Verifica que intentar borrar una pista sin sesión devuelve HTTP 401.
     */
    @Test
    void eliminarPistaSinAutenticacionDevuelve401() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/pistaPadel/courts/" + idPistaExistente,
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                String.class
        );

        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }
}
