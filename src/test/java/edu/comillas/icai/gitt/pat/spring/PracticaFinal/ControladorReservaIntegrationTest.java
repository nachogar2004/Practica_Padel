package edu.comillas.icai.gitt.pat.spring.PracticaFinal;

import edu.comillas.icai.gitt.pat.spring.PracticaFinal.repository.RepositorioPista;
import edu.comillas.icai.gitt.pat.spring.PracticaFinal.repository.RepositorioUsuario;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static edu.comillas.icai.gitt.pat.spring.PracticaFinal.ModeloRol.USER;

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

    @BeforeEach
    void setup() {
        repoUsuario.deleteAll();
        repoPista.deleteAll();

        // Crear usuario para las pruebas
        ModeloUsuario usuario = new ModeloUsuario();
        usuario.setNombre("Nacho");
        usuario.setApellidos("Prueba");
        usuario.setEmail("test@comillas.edu");
        usuario.setPassword("1234");
        usuario.setRol(USER);
        usuario.setFechaRegistro(LocalDateTime.now());
        repoUsuario.save(usuario);

        // Crear pista para las pruebas
        ModeloPista pista = new ModeloPista();
        pista.setNombre("Pista 1");
        pista.setUbicacion("Zona Norte");
        pista.setPrecioHora(new BigDecimal("20.00"));
        pista.setFechaAlta(LocalDateTime.now());
        idPistaExistente = repoPista.save(pista).getIdPista();
    }

    /**
     * Método auxiliar para simular el Login y obtener la Cookie de sesión.
     */
    private HttpHeaders loginYObtenerHeaders() {
        String loginJson = "{\"email\":\"test@comillas.edu\", \"password\":\"1234\"}";
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

    @Test
    public void crearReservaOkTest() {
        // 1. Obtener headers (debe incluir la cookie de sesión del login)
        HttpHeaders headers = loginYObtenerHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 2. Construir el JSON usando bloques de texto (más limpio y evita errores de comillas)
        // Usamos "idPista" dentro del objeto "pista" porque tu modelo tiene una relación @ManyToOne

        String reservaJson = "{\"pista\":{\"idPista\":" + idPistaExistente + "}, \"fechaReserva\":\"2026-02-14\", \"horaInicio\":\"10:00:00\", \"duracionMinutos\":60}";

        HttpEntity<String> entity = new HttpEntity<>(reservaJson, headers);

        // 3. Ejecutar POST según la guía
        ResponseEntity<ModeloReserva> response = restTemplate.exchange(
                "/pistaPadel/reservations",
                HttpMethod.POST,
                entity,
                ModeloReserva.class
        );

        // 4. Verificaciones
        // La guía especifica que la creación exitosa debe devolver 201 CREATED
        Assertions.assertEquals(HttpStatus.CREATED, response.getStatusCode(), "Debería devolver 201");

        Assertions.assertNotNull(response.getBody(), "El cuerpo de la respuesta no debería ser nulo");
        Assertions.assertNotNull(response.getBody().getIdReserva(), "Debería haber generado un ID de reserva");

        // Verificación adicional: comprobar que la pista asignada es la correcta
        Assertions.assertEquals(idPistaExistente, response.getBody().getPista().getIdPista());
    }

    @Test
    public void crearYPersistirReservaTest() {
        HttpHeaders headers = loginYObtenerHeaders();
        String reservaJson = "{\"pista\":{\"idPista\":" + idPistaExistente + "}, \"fechaReserva\":\"2026-05-20\", \"horaInicio\":\"10:00:00\", \"duracionMinutos\":60}";

        // 1. Crear la reserva
        ResponseEntity<ModeloReserva> postResponse = restTemplate.exchange(
                "/pistaPadel/reservations", HttpMethod.POST, new HttpEntity<>(reservaJson, headers), ModeloReserva.class
        );

        // 2. Verificar persistencia con GET
        ResponseEntity<ModeloReserva[]> getResponse = restTemplate.exchange(
                "/pistaPadel/reservations", HttpMethod.GET, new HttpEntity<>(headers), ModeloReserva[].class
        );

        Assertions.assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        Assertions.assertTrue(getResponse.getBody().length > 0);
        Assertions.assertEquals(postResponse.getBody().getIdReserva(), getResponse.getBody()[0].getIdReserva());
    }

    @Test
    public void listarMisReservasTest() {
        HttpHeaders headers = loginYObtenerHeaders();

        // Creamos dos reservas para el usuario
        String r1 = "{\"pista\":{\"idPista\":" + idPistaExistente + "}, \"fechaReserva\":\"2026-06-01\", \"horaInicio\":\"09:00:00\", \"duracionMinutos\":60}";
        String r2 = "{\"pista\":{\"idPista\":" + idPistaExistente + "}, \"fechaReserva\":\"2026-06-02\", \"horaInicio\":\"11:00:00\", \"duracionMinutos\":60}";

        restTemplate.exchange("/pistaPadel/reservations", HttpMethod.POST, new HttpEntity<>(r1, headers), String.class);
        restTemplate.exchange("/pistaPadel/reservations", HttpMethod.POST, new HttpEntity<>(r2, headers), String.class);

        // Llamamos al GET para listar
        ResponseEntity<List> response = restTemplate.exchange(
                "/pistaPadel/reservations",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                List.class
        );

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertTrue(response.getBody().size() >= 2);
    }

    @Test
    public void obtenerReservaInexistenteTest() {
        HttpHeaders headers = loginYObtenerHeaders();

        ResponseEntity<String> response = restTemplate.exchange(
                "/pistaPadel/reservations/9999",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        Assertions.assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    public void crearReservaSinAutorizacionTest() {
        // Enviamos la petición sin haber llamado a loginYObtenerHeaders()
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String reservaJson = "{\"pista\":{\"idPista\":" + idPistaExistente + "}, \"fechaReserva\":\"2026-02-14\", \"horaInicio\":\"10:00:00\", \"duracionMinutos\":60}";

        ResponseEntity<String> response = restTemplate.exchange(
                "/pistaPadel/reservations", HttpMethod.POST, new HttpEntity<>(reservaJson, headers), String.class
        );

        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }
}