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
 * Tests de integración end-to-end para el {@code ControladorDisponibilidad}.
 *
 * <p>Los endpoints de disponibilidad son <b>públicos</b> (no requieren sesión).
 * No obstante, algunos tests crean reservas (vía {@code POST /pistaPadel/reservations})
 * para verificar que la disponibilidad se reduce correctamente tras una reserva.</p>
 *
 * <h3>Endpoints cubiertos</h3>
 * <ul>
 *   <li>{@code GET /pistaPadel/courts/{courtId}/availability?date=...}</li>
 *   <li>{@code GET /pistaPadel/availability?date=...}</li>
 *   <li>{@code GET /pistaPadel/availability?date=...&courtId=...}</li>
 * </ul>
 *
 * <h3>Horario del club</h3>
 * <p>09:00 – 22:00. Sin reservas → una única franja libre.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ControladorDisponibilidadIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private RepositorioUsuario repoUsuario;

    @Autowired
    private RepositorioPista repoPista;

    private Long idPista1;
    private Long idPista2;

    /** Fecha futura usada en todos los tests para evitar conflictos de "fecha pasada". */
    private static final String FECHA = "2027-07-15";

    @BeforeEach
    void setup() {
        repoUsuario.deleteAll();
        repoPista.deleteAll();

        ModeloPista p1 = new ModeloPista();
        p1.setNombre("Pista A");
        p1.setUbicacion("Zona Norte");
        p1.setPrecioHora(new BigDecimal("20.00"));
        p1.setFechaAlta(LocalDateTime.now());
        idPista1 = repoPista.save(p1).getIdPista();

        ModeloPista p2 = new ModeloPista();
        p2.setNombre("Pista B");
        p2.setUbicacion("Zona Sur");
        p2.setPrecioHora(new BigDecimal("25.00"));
        p2.setFechaAlta(LocalDateTime.now());
        idPista2 = repoPista.save(p2).getIdPista();

        // Usuario necesario para crear reservas en los tests que lo requieran
        ModeloUsuario user = new ModeloUsuario();
        user.setNombre("User");
        user.setApellidos("Test");
        user.setEmail("user@test.com");
        user.setPassword("user1234");
        user.setRol(ModeloRol.USER);
        user.setFechaRegistro(LocalDateTime.now());
        repoUsuario.save(user);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Hace login y devuelve headers con la cookie de sesión. */
    private HttpHeaders loginUser() {
        String body = "{\"email\":\"user@test.com\",\"password\":\"user1234\"}";
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

    /** Crea una reserva para la pista dada, con la fecha y hora de inicio indicadas (60 min). */
    private void crearReserva(Long idPista, String fecha, String horaInicio) {
        HttpHeaders headers = loginUser();
        String json = "{\"pista\":{\"idPista\":" + idPista + "},"
                + "\"fechaReserva\":\"" + fecha + "\","
                + "\"horaInicio\":\"" + horaInicio + "\","
                + "\"duracionMinutos\":60}";
        restTemplate.exchange("/pistaPadel/reservations", HttpMethod.POST,
                new HttpEntity<>(json, headers), String.class);
    }

    // -------------------------------------------------------------------------
    // GET /pistaPadel/courts/{courtId}/availability?date=...
    // -------------------------------------------------------------------------

    /** Pista sin reservas → una sola franja libre que cubre todo el horario del club. */
    @Test
    void disponibilidadPistaLibreDevuelveUnaFranja() {
        ResponseEntity<ModeloDisponibilidad> response = restTemplate.getForEntity(
                "/pistaPadel/courts/" + idPista1 + "/availability?date=" + FECHA,
                ModeloDisponibilidad.class
        );

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());

        List<ModeloDisponibilidad.Franja> franjas = response.getBody().getFranjasDisponibles();
        Assertions.assertEquals(1, franjas.size(), "Sin reservas debe haber 1 franja libre");
        Assertions.assertEquals("09:00", franjas.get(0).getInicio().toString());
        Assertions.assertEquals("22:00", franjas.get(0).getFin().toString());
    }

    /**
     * Al crear una reserva de 10:00 a 11:00, la disponibilidad debe dividirse
     * en dos franjas: [09:00, 10:00] y [11:00, 22:00].
     */
    @Test
    void disponibilidadConUnaReservaDevuelveDosFragjas() {
        crearReserva(idPista1, FECHA, "10:00:00");

        ResponseEntity<ModeloDisponibilidad> response = restTemplate.getForEntity(
                "/pistaPadel/courts/" + idPista1 + "/availability?date=" + FECHA,
                ModeloDisponibilidad.class
        );

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        List<ModeloDisponibilidad.Franja> franjas = response.getBody().getFranjasDisponibles();
        Assertions.assertEquals(2, franjas.size(), "Con una reserva a mitad debe haber 2 franjas");
        Assertions.assertEquals("09:00", franjas.get(0).getInicio().toString());
        Assertions.assertEquals("10:00", franjas.get(0).getFin().toString());
        Assertions.assertEquals("11:00", franjas.get(1).getInicio().toString());
        Assertions.assertEquals("22:00", franjas.get(1).getFin().toString());
    }

    /** Reserva al inicio del horario (09:00) → solo queda la franja posterior. */
    @Test
    void disponibilidadConReservaAlInicioDevuelveUnaFranja() {
        crearReserva(idPista1, FECHA, "09:00:00");

        ResponseEntity<ModeloDisponibilidad> response = restTemplate.getForEntity(
                "/pistaPadel/courts/" + idPista1 + "/availability?date=" + FECHA,
                ModeloDisponibilidad.class
        );

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        List<ModeloDisponibilidad.Franja> franjas = response.getBody().getFranjasDisponibles();
        Assertions.assertEquals(1, franjas.size());
        Assertions.assertEquals("10:00", franjas.get(0).getInicio().toString());
        Assertions.assertEquals("22:00", franjas.get(0).getFin().toString());
    }

    /** Consultar disponibilidad en un día sin reservas devuelve la franja completa. */
    @Test
    void disponibilidadEnDiaDistintoNoAfectaOtroDia() {
        crearReserva(idPista1, FECHA, "10:00:00");

        // Consultamos un día diferente — no debe verse afectado por la reserva
        ResponseEntity<ModeloDisponibilidad> response = restTemplate.getForEntity(
                "/pistaPadel/courts/" + idPista1 + "/availability?date=2027-07-16",
                ModeloDisponibilidad.class
        );

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        List<ModeloDisponibilidad.Franja> franjas = response.getBody().getFranjasDisponibles();
        Assertions.assertEquals(1, franjas.size(), "Otro día debe seguir libre");
        Assertions.assertEquals("09:00", franjas.get(0).getInicio().toString());
    }

    /** La disponibilidad de una pista no se ve afectada por reservas de otra pista. */
    @Test
    void reservaEnPista2NoAfectaDisponibilidadDePista1() {
        crearReserva(idPista2, FECHA, "10:00:00");

        ResponseEntity<ModeloDisponibilidad> response = restTemplate.getForEntity(
                "/pistaPadel/courts/" + idPista1 + "/availability?date=" + FECHA,
                ModeloDisponibilidad.class
        );

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        List<ModeloDisponibilidad.Franja> franjas = response.getBody().getFranjasDisponibles();
        Assertions.assertEquals(1, franjas.size(), "Pista 1 debe seguir completamente libre");
    }

    /** Consultar disponibilidad de una pista inexistente devuelve HTTP 404. */
    @Test
    void disponibilidadPistaInexistenteDevuelve404() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/pistaPadel/courts/9999/availability?date=" + FECHA,
                String.class
        );

        Assertions.assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    /** El endpoint devuelve el id de la pista correctamente en la respuesta. */
    @Test
    void disponibilidadDevuelveIdPistaCorreto() {
        ResponseEntity<ModeloDisponibilidad> response = restTemplate.getForEntity(
                "/pistaPadel/courts/" + idPista1 + "/availability?date=" + FECHA,
                ModeloDisponibilidad.class
        );

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertEquals(idPista1, response.getBody().getIdPista());
    }

    // -------------------------------------------------------------------------
    // GET /pistaPadel/availability?date=... (todas las pistas)
    // -------------------------------------------------------------------------

    /** Sin filtro de pista, el endpoint devuelve disponibilidad de todas las pistas. */
    @Test
    void disponibilidadGeneralDevuelveTodasLasPistas() {
        ResponseEntity<List> response = restTemplate.getForEntity(
                "/pistaPadel/availability?date=" + FECHA,
                List.class
        );

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertEquals(2, response.getBody().size(),
                "Deben aparecer las 2 pistas creadas en el setup");
    }

    /** Sin pistas en BD el endpoint devuelve una lista vacía (no un error). */
    @Test
    void disponibilidadGeneralSinPistasDevuelveListaVacia() {
        repoPista.deleteAll();

        ResponseEntity<List> response = restTemplate.getForEntity(
                "/pistaPadel/availability?date=" + FECHA,
                List.class
        );

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertTrue(response.getBody().isEmpty(),
                "Sin pistas la lista debe estar vacía");
    }

    // -------------------------------------------------------------------------
    // GET /pistaPadel/availability?date=...&courtId=N (filtro por pista)
    // -------------------------------------------------------------------------

    /** Con filtro courtId, el endpoint devuelve solo esa pista. */
    @Test
    void disponibilidadGeneralFiltradaPorPistaDevuelveUnaSolaEntrada() {
        ResponseEntity<List> response = restTemplate.getForEntity(
                "/pistaPadel/availability?date=" + FECHA + "&courtId=" + idPista1,
                List.class
        );

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertEquals(1, response.getBody().size(),
                "Con courtId debe devolverse solo 1 pista");

        // Verificamos que el idPista devuelto es el correcto
        Map<?, ?> disp = (Map<?, ?>) response.getBody().get(0);
        Assertions.assertEquals(idPista1.intValue(), disp.get("idPista"));
    }

    /** Con filtro courtId de pista inexistente, el endpoint devuelve HTTP 404. */
    @Test
    void disponibilidadGeneralFiltradaPorPistaInexistenteDevuelve404() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/pistaPadel/availability?date=" + FECHA + "&courtId=9999",
                String.class
        );

        Assertions.assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
