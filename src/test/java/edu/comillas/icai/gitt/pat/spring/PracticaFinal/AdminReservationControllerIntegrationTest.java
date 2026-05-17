package edu.comillas.icai.gitt.pat.spring.PracticaFinal;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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
 * Tests de integración end-to-end para el {@link edu.comillas.icai.gitt.pat.spring.PracticaFinal.controller.AdminReservationController}.
 *
 * <p>Cubre el endpoint {@code GET /pistaPadel/admin/reservations} con todos sus
 * filtros opcionales:</p>
 * <ul>
 *   <li>Sin filtros — devuelve todas las reservas del sistema.</li>
 *   <li>{@code ?date=YYYY-MM-DD} — filtra por fecha exacta.</li>
 *   <li>{@code ?courtId=N} — filtra por id de pista.</li>
 *   <li>{@code ?userId=N} — filtra por id de usuario.</li>
 *   <li>Sin reservas en BD — devuelve lista vacía (no error).</li>
 * </ul>
 *
 * <h3>Setup</h3>
 * <p>Se crean dos usuarios (USER), dos pistas y varias reservas en distintas fechas
 * para poder verificar que los filtros funcionan correctamente.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AdminReservationControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private RepositorioUsuario repoUsuario;

    @Autowired
    private RepositorioPista repoPista;

    private Long idPista1;
    private Long idPista2;
    private Long idUser1;
    private Long idUser2;

    @BeforeEach
    void setup() {
        repoUsuario.deleteAll();
        repoPista.deleteAll();

        ModeloUsuario user1 = new ModeloUsuario();
        user1.setNombre("Usuario1");
        user1.setApellidos("Test");
        user1.setEmail("user1@test.com");
        user1.setPassword("pass1234");
        user1.setRol(ModeloRol.USER);
        user1.setFechaRegistro(LocalDateTime.now());
        idUser1 = repoUsuario.save(user1).getIdUsuario();

        ModeloUsuario user2 = new ModeloUsuario();
        user2.setNombre("Usuario2");
        user2.setApellidos("Test");
        user2.setEmail("user2@test.com");
        user2.setPassword("pass1234");
        user2.setRol(ModeloRol.USER);
        user2.setFechaRegistro(LocalDateTime.now());
        idUser2 = repoUsuario.save(user2).getIdUsuario();

        ModeloPista p1 = new ModeloPista();
        p1.setNombre("Pista Admin 1");
        p1.setUbicacion("Zona Norte");
        p1.setPrecioHora(new BigDecimal("20.00"));
        p1.setFechaAlta(LocalDateTime.now());
        idPista1 = repoPista.save(p1).getIdPista();

        ModeloPista p2 = new ModeloPista();
        p2.setNombre("Pista Admin 2");
        p2.setUbicacion("Zona Sur");
        p2.setPrecioHora(new BigDecimal("25.00"));
        p2.setFechaAlta(LocalDateTime.now());
        idPista2 = repoPista.save(p2).getIdPista();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private HttpHeaders login(String email, String password) {
        String body = "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> r = restTemplate.postForEntity(
                "/pistaPadel/auth/login", new HttpEntity<>(body, h), String.class);
        String cookie = r.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        HttpHeaders auth = new HttpHeaders();
        auth.setContentType(MediaType.APPLICATION_JSON);
        auth.set(HttpHeaders.COOKIE, cookie);
        return auth;
    }

    /** Crea una reserva como el usuario dado y devuelve la entidad guardada. */
    private ModeloReserva crearReserva(String email, String password, Long idPista,
                                       String fecha, String horaInicio) {
        HttpHeaders headers = login(email, password);
        String json = "{\"pista\":{\"idPista\":" + idPista + "},"
                + "\"fechaReserva\":\"" + fecha + "\","
                + "\"horaInicio\":\"" + horaInicio + "\","
                + "\"duracionMinutos\":60}";
        return restTemplate.exchange("/pistaPadel/reservations", HttpMethod.POST,
                new HttpEntity<>(json, headers), ModeloReserva.class).getBody();
    }

    // =========================================================================
    // GET /pistaPadel/admin/reservations — sin filtros
    // =========================================================================

    /** Sin filtros el endpoint devuelve todas las reservas del sistema. */
    @Test
    void getTodasLasReservasSinFiltros() {
        crearReserva("user1@test.com", "pass1234", idPista1, "2027-10-01", "09:00:00");
        crearReserva("user1@test.com", "pass1234", idPista1, "2027-10-02", "09:00:00");
        crearReserva("user2@test.com", "pass1234", idPista2, "2027-10-03", "09:00:00");

        ResponseEntity<List> response = restTemplate.getForEntity(
                "/pistaPadel/admin/reservations", List.class);

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertEquals(3, response.getBody().size(),
                "Sin filtros deben devolverse las 3 reservas creadas");
    }

    /** Sin reservas en BD el endpoint devuelve una lista vacía, no un error. */
    @Test
    void getReservasSinReservasDevuelveListaVacia() {
        ResponseEntity<List> response = restTemplate.getForEntity(
                "/pistaPadel/admin/reservations", List.class);

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertTrue(response.getBody().isEmpty(),
                "Sin reservas la lista debe estar vacía");
    }

    // =========================================================================
    // GET /pistaPadel/admin/reservations?date=... — filtro por fecha
    // =========================================================================

    /** El filtro {@code ?date} devuelve solo las reservas de ese día. */
    @Test
    void getReservasFiltrandoPorFecha() {
        crearReserva("user1@test.com", "pass1234", idPista1, "2027-10-01", "09:00:00");
        crearReserva("user1@test.com", "pass1234", idPista1, "2027-10-02", "09:00:00");

        ResponseEntity<List> response = restTemplate.getForEntity(
                "/pistaPadel/admin/reservations?date=2027-10-01", List.class);

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertEquals(1, response.getBody().size(),
                "Solo debe aparecer la reserva del 2027-10-01");

        Map<?, ?> reserva = (Map<?, ?>) response.getBody().get(0);
        Assertions.assertEquals("2027-10-01", reserva.get("fechaReserva"));
    }

    /** El filtro {@code ?date} para un día sin reservas devuelve lista vacía. */
    @Test
    void getReservasFiltrandoPorFechaSinResultadosDevuelveListaVacia() {
        crearReserva("user1@test.com", "pass1234", idPista1, "2027-10-01", "09:00:00");

        ResponseEntity<List> response = restTemplate.getForEntity(
                "/pistaPadel/admin/reservations?date=2027-12-31", List.class);

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertTrue(response.getBody().isEmpty());
    }

    // =========================================================================
    // GET /pistaPadel/admin/reservations?courtId=... — filtro por pista
    // =========================================================================

    /** El filtro {@code ?courtId} devuelve solo las reservas de esa pista. */
    @Test
    void getReservasFiltrandoPorPista() {
        crearReserva("user1@test.com", "pass1234", idPista1, "2027-10-01", "09:00:00");
        crearReserva("user2@test.com", "pass1234", idPista2, "2027-10-01", "09:00:00");

        ResponseEntity<List> response = restTemplate.getForEntity(
                "/pistaPadel/admin/reservations?courtId=" + idPista1, List.class);

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertEquals(1, response.getBody().size(),
                "Solo debe aparecer la reserva de la pista filtrada");
    }

    /** El filtro {@code ?courtId} combinado con {@code ?date} aplica ambos criterios. */
    @Test
    void getReservasFiltrandoPorPistaYFecha() {
        crearReserva("user1@test.com", "pass1234", idPista1, "2027-10-01", "09:00:00");
        crearReserva("user1@test.com", "pass1234", idPista1, "2027-10-02", "09:00:00");
        crearReserva("user2@test.com", "pass1234", idPista2, "2027-10-01", "09:00:00");

        ResponseEntity<List> response = restTemplate.getForEntity(
                "/pistaPadel/admin/reservations?date=2027-10-01&courtId=" + idPista1, List.class);

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertEquals(1, response.getBody().size(),
                "Debe aparecer solo la reserva que cumple ambos filtros");
    }

    // =========================================================================
    // GET /pistaPadel/admin/reservations?userId=... — filtro por usuario
    // =========================================================================

    /** El filtro {@code ?userId} devuelve solo las reservas de ese usuario. */
    @Test
    void getReservasFiltrandoPorUsuario() {
        crearReserva("user1@test.com", "pass1234", idPista1, "2027-10-01", "09:00:00");
        crearReserva("user1@test.com", "pass1234", idPista1, "2027-10-02", "09:00:00");
        crearReserva("user2@test.com", "pass1234", idPista2, "2027-10-01", "09:00:00");

        ResponseEntity<List> response = restTemplate.getForEntity(
                "/pistaPadel/admin/reservations?userId=" + idUser1, List.class);

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertEquals(2, response.getBody().size(),
                "Deben aparecer las 2 reservas del usuario 1");
    }

    /** El filtro {@code ?userId} combinado con {@code ?date} aplica ambos criterios. */
    @Test
    void getReservasFiltrandoPorUsuarioYFecha() {
        crearReserva("user1@test.com", "pass1234", idPista1, "2027-10-01", "09:00:00");
        crearReserva("user1@test.com", "pass1234", idPista1, "2027-10-02", "09:00:00");
        crearReserva("user2@test.com", "pass1234", idPista2, "2027-10-01", "09:00:00");

        ResponseEntity<List> response = restTemplate.getForEntity(
                "/pistaPadel/admin/reservations?date=2027-10-01&userId=" + idUser1, List.class);

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertEquals(1, response.getBody().size(),
                "Debe aparecer solo la reserva del usuario 1 en esa fecha");
    }

    /** Los tres filtros combinados (fecha + pista + usuario) aplican todos a la vez. */
    @Test
    void getReservasFiltrandoPorFechaPistaYUsuario() {
        crearReserva("user1@test.com", "pass1234", idPista1, "2027-10-01", "09:00:00");
        crearReserva("user1@test.com", "pass1234", idPista2, "2027-10-01", "11:00:00");
        crearReserva("user2@test.com", "pass1234", idPista1, "2027-10-01", "13:00:00");

        ResponseEntity<List> response = restTemplate.getForEntity(
                "/pistaPadel/admin/reservations?date=2027-10-01&courtId=" + idPista1
                        + "&userId=" + idUser1,
                List.class);

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertEquals(1, response.getBody().size(),
                "Los tres filtros combinados deben dejar una sola reserva");
    }

    /** Un filtro {@code ?userId} de un usuario sin reservas devuelve lista vacía. */
    @Test
    void getReservasFiltrandoPorUsuarioSinReservasDevuelveListaVacia() {
        crearReserva("user1@test.com", "pass1234", idPista1, "2027-10-01", "09:00:00");

        ResponseEntity<List> response = restTemplate.getForEntity(
                "/pistaPadel/admin/reservations?userId=" + idUser2, List.class);

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertTrue(response.getBody().isEmpty(),
                "El usuario 2 no tiene reservas; debe devolverse lista vacía");
    }
}
