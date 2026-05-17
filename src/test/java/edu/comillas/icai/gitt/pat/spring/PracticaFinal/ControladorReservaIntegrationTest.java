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

import static edu.comillas.icai.gitt.pat.spring.PracticaFinal.ModeloRol.USER;
import edu.comillas.icai.gitt.pat.spring.PracticaFinal.repository.RepositorioPista;
import edu.comillas.icai.gitt.pat.spring.PracticaFinal.repository.RepositorioUsuario;

/**
 * Tests de integración end-to-end para el {@link ControladorReserva}.
 *
 * <p>Levanta un servidor HTTP real en un puerto aleatorio ({@code RANDOM_PORT})
 * y realiza peticiones HTTP con {@code TestRestTemplate}, igual que haría un
 * cliente externo. Esto permite probar el flujo completo: controlador → servicio
 * → repositorio → base de datos H2 en memoria.</p>
 *
 * <h3>Patrón de test</h3>
 * <ol>
 *   <li>{@code @BeforeEach} crea los datos de prueba (usuarios + pista).</li>
 *   <li>Cada test hace login para obtener la cookie de sesión.</li>
 *   <li>Las peticiones se envían con esa cookie para simular un usuario autenticado.</li>
 *   <li>{@code @DirtiesContext} reinicia el contexto después de cada test para
 *       garantizar aislamiento completo entre tests.</li>
 * </ol>
 *
 * <h3>Usuarios del setup</h3>
 * <ul>
 *   <li>{@code test@comillas.edu} — rol USER (usuario principal)</li>
 *   <li>{@code otro@comillas.edu} — rol USER (segundo usuario para tests de autorización)</li>
 *   <li>{@code admin@comillas.edu} — rol ADMIN (para tests de permisos de administrador)</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ControladorReservaIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private RepositorioUsuario repoUsuario;

    @Autowired
    private RepositorioPista repoPista;

    private Long idPistaExistente;
    private Long idPistaInactiva;

    /** Limpia los repositorios y crea usuarios y pistas de prueba antes de cada test. */
    @BeforeEach
    void setup() {
        repoUsuario.deleteAll();
        repoPista.deleteAll();

        ModeloUsuario usuario = new ModeloUsuario();
        usuario.setNombre("Nacho");
        usuario.setApellidos("Prueba");
        usuario.setEmail("test@comillas.edu");
        usuario.setPassword("1234");
        usuario.setRol(USER);
        usuario.setFechaRegistro(LocalDateTime.now());
        repoUsuario.save(usuario);

        ModeloUsuario otroUsuario = new ModeloUsuario();
        otroUsuario.setNombre("Otro");
        otroUsuario.setApellidos("Usuario");
        otroUsuario.setEmail("otro@comillas.edu");
        otroUsuario.setPassword("1234");
        otroUsuario.setRol(USER);
        otroUsuario.setFechaRegistro(LocalDateTime.now());
        repoUsuario.save(otroUsuario);

        ModeloUsuario admin = new ModeloUsuario();
        admin.setNombre("Admin");
        admin.setApellidos("Test");
        admin.setEmail("admin@comillas.edu");
        admin.setPassword("admin1234");
        admin.setRol(ModeloRol.ADMIN);
        admin.setFechaRegistro(LocalDateTime.now());
        repoUsuario.save(admin);

        ModeloPista pista = new ModeloPista();
        pista.setNombre("Pista 1");
        pista.setUbicacion("Zona Norte");
        pista.setPrecioHora(new BigDecimal("20.00"));
        pista.setFechaAlta(LocalDateTime.now());
        idPistaExistente = repoPista.save(pista).getIdPista();

        ModeloPista pistaInactiva = new ModeloPista();
        pistaInactiva.setNombre("Pista Inactiva");
        pistaInactiva.setUbicacion("Zona Sur");
        pistaInactiva.setPrecioHora(new BigDecimal("15.00"));
        pistaInactiva.setFechaAlta(LocalDateTime.now());
        pistaInactiva.setActiva(false);
        idPistaInactiva = repoPista.save(pistaInactiva).getIdPista();
    }

    // -------------------------------------------------------------------------
    // Helpers de login
    // -------------------------------------------------------------------------

    private HttpHeaders loginUser() {
        return login("test@comillas.edu", "1234");
    }

    private HttpHeaders loginOtroUser() {
        return login("otro@comillas.edu", "1234");
    }

    private HttpHeaders loginAdmin() {
        return login("admin@comillas.edu", "admin1234");
    }

    private HttpHeaders login(String email, String password) {
        String loginJson = "{\"email\":\"" + email + "\", \"password\":\"" + password + "\"}";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = restTemplate.postForEntity("/pistaPadel/auth/login",
                new HttpEntity<>(loginJson, headers), String.class);

        String cookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        HttpHeaders authHeaders = new HttpHeaders();
        authHeaders.setContentType(MediaType.APPLICATION_JSON);
        authHeaders.set(HttpHeaders.COOKIE, cookie);
        return authHeaders;
    }

    /** Crea una reserva para el usuario autenticado y devuelve la entidad guardada. */
    private ModeloReserva crearReserva(HttpHeaders headers, Long idPista, String fecha, String horaInicio) {
        String json = "{\"pista\":{\"idPista\":" + idPista + "}, \"fechaReserva\":\"" + fecha
                + "\", \"horaInicio\":\"" + horaInicio + "\", \"duracionMinutos\":60}";
        return restTemplate.exchange(
                "/pistaPadel/reservations", HttpMethod.POST,
                new HttpEntity<>(json, headers), ModeloReserva.class).getBody();
    }

    // =========================================================================
    // POST /pistaPadel/reservations — crear reserva
    // =========================================================================

    /** Crear una reserva válida devuelve HTTP 201 y la reserva persistida con id. */
    @Test
    public void crearReservaOkTest() {
        HttpHeaders headers = loginUser();
        String reservaJson = "{\"pista\":{\"idPista\":" + idPistaExistente + "}, \"fechaReserva\":\"2027-06-14\", \"horaInicio\":\"10:00:00\", \"duracionMinutos\":60}";

        ResponseEntity<ModeloReserva> response = restTemplate.exchange(
                "/pistaPadel/reservations", HttpMethod.POST,
                new HttpEntity<>(reservaJson, headers), ModeloReserva.class);

        Assertions.assertEquals(HttpStatus.CREATED, response.getStatusCode(), "Debería devolver 201");
        Assertions.assertNotNull(response.getBody());
        Assertions.assertNotNull(response.getBody().getIdReserva(), "Debería haber generado un ID de reserva");
        Assertions.assertEquals(idPistaExistente, response.getBody().getPista().getIdPista());
    }

    /** Crear dos reservas solapadas en la misma pista y hora devuelve HTTP 409. */
    @Test
    public void crearReservaConSolapeDevuelve409() {
        HttpHeaders headers = loginUser();
        crearReserva(headers, idPistaExistente, "2027-08-01", "10:00:00");

        String json = "{\"pista\":{\"idPista\":" + idPistaExistente + "}, \"fechaReserva\":\"2027-08-01\", \"horaInicio\":\"10:30:00\", \"duracionMinutos\":60}";
        ResponseEntity<String> response = restTemplate.exchange(
                "/pistaPadel/reservations", HttpMethod.POST,
                new HttpEntity<>(json, headers), String.class);

        Assertions.assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    /** Crear una reserva en una fecha pasada devuelve HTTP 400. */
    @Test
    public void crearReservaFechaPasadaDevuelve400() {
        HttpHeaders headers = loginUser();
        String json = "{\"pista\":{\"idPista\":" + idPistaExistente + "}, \"fechaReserva\":\"2020-01-01\", \"horaInicio\":\"10:00:00\", \"duracionMinutos\":60}";

        ResponseEntity<String> response = restTemplate.exchange(
                "/pistaPadel/reservations", HttpMethod.POST,
                new HttpEntity<>(json, headers), String.class);

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    /** Crear una reserva en una pista inactiva devuelve HTTP 400. */
    @Test
    public void crearReservaPistaInactivaDevuelve400() {
        HttpHeaders headers = loginUser();
        String json = "{\"pista\":{\"idPista\":" + idPistaInactiva + "}, \"fechaReserva\":\"2027-08-01\", \"horaInicio\":\"10:00:00\", \"duracionMinutos\":60}";

        ResponseEntity<String> response = restTemplate.exchange(
                "/pistaPadel/reservations", HttpMethod.POST,
                new HttpEntity<>(json, headers), String.class);

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    /** Un ADMIN no puede realizar reservas — devuelve HTTP 403. */
    @Test
    public void adminNoPuedeReservarDevuelve403() {
        HttpHeaders headers = loginAdmin();
        String json = "{\"pista\":{\"idPista\":" + idPistaExistente + "}, \"fechaReserva\":\"2027-08-01\", \"horaInicio\":\"10:00:00\", \"duracionMinutos\":60}";

        ResponseEntity<String> response = restTemplate.exchange(
                "/pistaPadel/reservations", HttpMethod.POST,
                new HttpEntity<>(json, headers), String.class);

        Assertions.assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    /** Crear una reserva en una pista que no existe devuelve HTTP 404. */
    @Test
    public void crearReservaPistaInexistenteDevuelve404() {
        HttpHeaders headers = loginUser();
        String json = "{\"pista\":{\"idPista\":9999}, \"fechaReserva\":\"2027-08-01\", \"horaInicio\":\"10:00:00\", \"duracionMinutos\":60}";

        ResponseEntity<String> response = restTemplate.exchange(
                "/pistaPadel/reservations", HttpMethod.POST,
                new HttpEntity<>(json, headers), String.class);

        Assertions.assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    /** Crear una reserva sin sesión activa devuelve HTTP 401. */
    @Test
    public void crearReservaSinAutorizacionTest() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String reservaJson = "{\"pista\":{\"idPista\":" + idPistaExistente + "}, \"fechaReserva\":\"2026-02-14\", \"horaInicio\":\"10:00:00\", \"duracionMinutos\":60}";

        ResponseEntity<String> response = restTemplate.exchange(
                "/pistaPadel/reservations", HttpMethod.POST,
                new HttpEntity<>(reservaJson, headers), String.class);

        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    // =========================================================================
    // GET /pistaPadel/reservations — listar mis reservas
    // =========================================================================

    /** Crea una reserva y verifica que el GET la devuelve correctamente. */
    @Test
    public void crearYPersistirReservaTest() {
        HttpHeaders headers = loginUser();
        String reservaJson = "{\"pista\":{\"idPista\":" + idPistaExistente + "}, \"fechaReserva\":\"2026-05-20\", \"horaInicio\":\"10:00:00\", \"duracionMinutos\":60}";

        ResponseEntity<ModeloReserva> postResponse = restTemplate.exchange(
                "/pistaPadel/reservations", HttpMethod.POST,
                new HttpEntity<>(reservaJson, headers), ModeloReserva.class);

        ResponseEntity<ModeloReserva[]> getResponse = restTemplate.exchange(
                "/pistaPadel/reservations", HttpMethod.GET,
                new HttpEntity<>(headers), ModeloReserva[].class);

        Assertions.assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        Assertions.assertTrue(getResponse.getBody().length > 0);
        Assertions.assertEquals(postResponse.getBody().getIdReserva(), getResponse.getBody()[0].getIdReserva());
    }

    /** Crea dos reservas y verifica que el GET devuelve ambas. */
    @Test
    public void listarMisReservasTest() {
        HttpHeaders headers = loginUser();
        crearReserva(headers, idPistaExistente, "2026-06-01", "09:00:00");
        crearReserva(headers, idPistaExistente, "2026-06-02", "11:00:00");

        ResponseEntity<List> response = restTemplate.exchange(
                "/pistaPadel/reservations", HttpMethod.GET,
                new HttpEntity<>(headers), List.class);

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertTrue(response.getBody().size() >= 2);
    }

    /** El filtro {@code ?from} excluye reservas anteriores a la fecha dada. */
    @Test
    public void listarReservasConFiltroFromExcluyeAnteriores() {
        HttpHeaders headers = loginUser();
        crearReserva(headers, idPistaExistente, "2027-09-01", "09:00:00");
        crearReserva(headers, idPistaExistente, "2027-09-10", "09:00:00");

        ResponseEntity<List> response = restTemplate.exchange(
                "/pistaPadel/reservations?from=2027-09-05T00:00:00",
                HttpMethod.GET, new HttpEntity<>(headers), List.class);

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertEquals(1, response.getBody().size(),
                "Solo debe aparecer la reserva posterior al filtro from");
    }

    /** El filtro {@code ?to} excluye reservas posteriores a la fecha dada. */
    @Test
    public void listarReservasConFiltroToExcluyePosteriores() {
        HttpHeaders headers = loginUser();
        crearReserva(headers, idPistaExistente, "2027-09-01", "09:00:00");
        crearReserva(headers, idPistaExistente, "2027-09-10", "09:00:00");

        ResponseEntity<List> response = restTemplate.exchange(
                "/pistaPadel/reservations?to=2027-09-05T23:59:59",
                HttpMethod.GET, new HttpEntity<>(headers), List.class);

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertEquals(1, response.getBody().size(),
                "Solo debe aparecer la reserva anterior al filtro to");
    }

    /** El GET solo devuelve reservas del usuario autenticado, no las de otros usuarios. */
    @Test
    public void listarReservasDevuelveSoloLasDelUsuarioActual() {
        HttpHeaders headersUser = loginUser();
        HttpHeaders headersOtro = loginOtroUser();

        crearReserva(headersUser, idPistaExistente, "2027-09-01", "09:00:00");
        crearReserva(headersOtro, idPistaExistente, "2027-09-02", "09:00:00");

        ResponseEntity<List> response = restTemplate.exchange(
                "/pistaPadel/reservations", HttpMethod.GET,
                new HttpEntity<>(headersUser), List.class);

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertEquals(1, response.getBody().size(),
                "Un USER solo debe ver sus propias reservas");
    }

    // =========================================================================
    // GET /pistaPadel/reservations/{id} — obtener reserva por id
    // =========================================================================

    /** El dueño de la reserva puede consultarla y recibe HTTP 200. */
    @Test
    public void obtenerReservaPropiaDevolveOk() {
        HttpHeaders headers = loginUser();
        ModeloReserva creada = crearReserva(headers, idPistaExistente, "2027-09-01", "09:00:00");

        ResponseEntity<ModeloReserva> response = restTemplate.exchange(
                "/pistaPadel/reservations/" + creada.getIdReserva(),
                HttpMethod.GET, new HttpEntity<>(headers), ModeloReserva.class);

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertEquals(creada.getIdReserva(), response.getBody().getIdReserva());
    }

    /** Un usuario no puede ver la reserva de otro usuario — devuelve HTTP 403. */
    @Test
    public void obtenerReservaAjenaDevuelve403() {
        HttpHeaders headersUser = loginUser();
        HttpHeaders headersOtro = loginOtroUser();

        ModeloReserva reservaDeOtro = crearReserva(headersOtro, idPistaExistente, "2027-09-01", "09:00:00");

        ResponseEntity<String> response = restTemplate.exchange(
                "/pistaPadel/reservations/" + reservaDeOtro.getIdReserva(),
                HttpMethod.GET, new HttpEntity<>(headersUser), String.class);

        Assertions.assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    /** Un ADMIN puede ver la reserva de cualquier usuario — devuelve HTTP 200. */
    @Test
    public void adminPuedeVerReservaDeOtroUser() {
        HttpHeaders headersUser = loginUser();
        HttpHeaders headersAdmin = loginAdmin();

        ModeloReserva reserva = crearReserva(headersUser, idPistaExistente, "2027-09-01", "09:00:00");

        ResponseEntity<ModeloReserva> response = restTemplate.exchange(
                "/pistaPadel/reservations/" + reserva.getIdReserva(),
                HttpMethod.GET, new HttpEntity<>(headersAdmin), ModeloReserva.class);

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertEquals(reserva.getIdReserva(), response.getBody().getIdReserva());
    }

    /** Buscar una reserva inexistente devuelve HTTP 404. */
    @Test
    public void obtenerReservaInexistenteTest() {
        HttpHeaders headers = loginUser();

        ResponseEntity<String> response = restTemplate.exchange(
                "/pistaPadel/reservations/9999",
                HttpMethod.GET, new HttpEntity<>(headers), String.class);

        Assertions.assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    // =========================================================================
    // PATCH /pistaPadel/reservations/{id} — editar reserva
    // =========================================================================

    /** El dueño puede cambiar la fecha de su reserva y recibe HTTP 200 con los nuevos datos. */
    @Test
    public void patchReservaHappyPath() {
        HttpHeaders headers = loginUser();
        ModeloReserva creada = crearReserva(headers, idPistaExistente, "2027-09-01", "09:00:00");

        String patch = "{\"fechaReserva\":\"2027-09-15\", \"horaInicio\":\"11:00:00\", \"duracionMinutos\":90}";
        ResponseEntity<ModeloReserva> response = restTemplate.exchange(
                "/pistaPadel/reservations/" + creada.getIdReserva(),
                HttpMethod.PATCH, new HttpEntity<>(patch, headers), ModeloReserva.class);

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertEquals("2027-09-15", response.getBody().getFechaReserva().toString());
        Assertions.assertEquals(90, response.getBody().getDuracionMinutos());
    }

    /** PATCH con un horario que solapa con otra reserva activa devuelve HTTP 409. */
    @Test
    public void patchReservaConflictoDevuelve409() {
        HttpHeaders headers = loginUser();
        crearReserva(headers, idPistaExistente, "2027-09-01", "10:00:00");
        ModeloReserva segunda = crearReserva(headers, idPistaExistente, "2027-09-01", "12:00:00");

        // Intentamos mover la segunda reserva para que solape con la primera
        String patch = "{\"horaInicio\":\"10:30:00\"}";
        ResponseEntity<String> response = restTemplate.exchange(
                "/pistaPadel/reservations/" + segunda.getIdReserva(),
                HttpMethod.PATCH, new HttpEntity<>(patch, headers), String.class);

        Assertions.assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    /** PATCH sobre una reserva inexistente devuelve HTTP 404. */
    @Test
    public void patchReservaInexistenteDevuelve404() {
        HttpHeaders headers = loginUser();
        String patch = "{\"fechaReserva\":\"2027-09-15\"}";

        ResponseEntity<String> response = restTemplate.exchange(
                "/pistaPadel/reservations/9999",
                HttpMethod.PATCH, new HttpEntity<>(patch, headers), String.class);

        Assertions.assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    /** PATCH sobre la reserva de otro usuario devuelve HTTP 403. */
    @Test
    public void patchReservaAjenaDevuelve403() {
        HttpHeaders headersUser = loginUser();
        HttpHeaders headersOtro = loginOtroUser();

        ModeloReserva reservaDeOtro = crearReserva(headersOtro, idPistaExistente, "2027-09-01", "09:00:00");

        String patch = "{\"fechaReserva\":\"2027-09-20\"}";
        ResponseEntity<String> response = restTemplate.exchange(
                "/pistaPadel/reservations/" + reservaDeOtro.getIdReserva(),
                HttpMethod.PATCH, new HttpEntity<>(patch, headersUser), String.class);

        Assertions.assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    /** PATCH sin sesión activa devuelve HTTP 401. */
    @Test
    public void patchReservaSinSesionDevuelve401() {
        HttpHeaders headers = loginUser();
        ModeloReserva creada = crearReserva(headers, idPistaExistente, "2027-09-01", "09:00:00");

        HttpHeaders sinSesion = new HttpHeaders();
        sinSesion.setContentType(MediaType.APPLICATION_JSON);
        String patch = "{\"fechaReserva\":\"2027-09-20\"}";

        ResponseEntity<String> response = restTemplate.exchange(
                "/pistaPadel/reservations/" + creada.getIdReserva(),
                HttpMethod.PATCH, new HttpEntity<>(patch, sinSesion), String.class);

        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    // =========================================================================
    // DELETE /pistaPadel/reservations/{id} — cancelar reserva
    // =========================================================================

    /** El dueño puede cancelar su reserva y recibe HTTP 204. */
    @Test
    public void cancelarReservaHappyPathDevuelve204() {
        HttpHeaders headers = loginUser();
        ModeloReserva creada = crearReserva(headers, idPistaExistente, "2027-09-01", "09:00:00");

        ResponseEntity<Void> response = restTemplate.exchange(
                "/pistaPadel/reservations/" + creada.getIdReserva(),
                HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);

        Assertions.assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    /**
     * Tras cancelar, la reserva sigue en BD (soft-delete) y el JSON de respuesta
     * contiene "CANCELADA". Se verifica contra el String crudo porque el campo
     * {@code estado} tiene {@code @JsonProperty(READ_ONLY)} y Jackson no lo
     * deserializa al convertir la respuesta a un objeto Java.
     */
    @Test
    public void cancelarReservaCambiaEstadoACancelada() {
        HttpHeaders headers = loginUser();
        ModeloReserva creada = crearReserva(headers, idPistaExistente, "2027-09-01", "09:00:00");

        restTemplate.exchange("/pistaPadel/reservations/" + creada.getIdReserva(),
                HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);

        // La reserva sigue existiendo (soft-delete): verificamos el JSON crudo
        ResponseEntity<String> getResponse = restTemplate.exchange(
                "/pistaPadel/reservations/" + creada.getIdReserva(),
                HttpMethod.GET, new HttpEntity<>(headers), String.class);

        Assertions.assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        Assertions.assertTrue(getResponse.getBody().contains("\"estado\":\"CANCELADA\""),
                "El JSON de la reserva debe indicar estado CANCELADA tras el soft-delete");
    }

    /** Cancelar una reserva inexistente devuelve HTTP 404. */
    @Test
    public void cancelarReservaInexistenteDevuelve404() {
        HttpHeaders headers = loginUser();

        ResponseEntity<String> response = restTemplate.exchange(
                "/pistaPadel/reservations/9999",
                HttpMethod.DELETE, new HttpEntity<>(headers), String.class);

        Assertions.assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    /** Cancelar la reserva de otro usuario devuelve HTTP 403. */
    @Test
    public void cancelarReservaAjenaDevuelve403() {
        HttpHeaders headersUser = loginUser();
        HttpHeaders headersOtro = loginOtroUser();

        ModeloReserva reservaDeOtro = crearReserva(headersOtro, idPistaExistente, "2027-09-01", "09:00:00");

        ResponseEntity<String> response = restTemplate.exchange(
                "/pistaPadel/reservations/" + reservaDeOtro.getIdReserva(),
                HttpMethod.DELETE, new HttpEntity<>(headersUser), String.class);

        Assertions.assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    /** Cancelar una reserva sin sesión activa devuelve HTTP 401. */
    @Test
    public void cancelarReservaSinSesionDevuelve401() {
        HttpHeaders headersUser = loginUser();
        ModeloReserva creada = crearReserva(headersUser, idPistaExistente, "2027-09-01", "09:00:00");

        ResponseEntity<String> response = restTemplate.exchange(
                "/pistaPadel/reservations/" + creada.getIdReserva(),
                HttpMethod.DELETE, HttpEntity.EMPTY, String.class);

        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    /** Un ADMIN puede cancelar la reserva de otro usuario y recibe HTTP 204. */
    @Test
    public void adminPuedeCancelarReservaDeOtroUser() {
        HttpHeaders headersUser = loginUser();
        HttpHeaders headersAdmin = loginAdmin();

        ModeloReserva reserva = crearReserva(headersUser, idPistaExistente, "2027-09-01", "09:00:00");

        ResponseEntity<Void> response = restTemplate.exchange(
                "/pistaPadel/reservations/" + reserva.getIdReserva(),
                HttpMethod.DELETE, new HttpEntity<>(headersAdmin), Void.class);

        Assertions.assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }
}
