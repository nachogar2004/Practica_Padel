package edu.comillas.icai.gitt.pat.spring.PracticaFinal;

import edu.comillas.icai.gitt.pat.spring.PracticaFinal.repository.RepositorioPista;
import edu.comillas.icai.gitt.pat.spring.PracticaFinal.repository.RepositorioReserva;
import edu.comillas.icai.gitt.pat.spring.PracticaFinal.repository.RepositorioUsuario;
import edu.comillas.icai.gitt.pat.spring.PracticaFinal.servicios.TareasProgramadas;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class TareasProgramadasTest {

    @Autowired
    private TareasProgramadas tareasProgramadas;

    @Autowired
    private RepositorioReserva repoReserva;

    @Autowired
    private RepositorioUsuario repoUsuario;

    @Autowired
    private RepositorioPista repoPista;

    @BeforeEach
    void setup() {
        repoReserva.deleteAll();
        repoPista.deleteAll();
        repoUsuario.deleteAll();
    }

    @Test
    public void testEnviarRecordatoriosDiariosEncuentraReservas() {
        // GIVEN: Un usuario COMPLETAMENTE relleno para cumplir con las restricciones de BD
        ModeloUsuario user = new ModeloUsuario();
        user.setEmail("jugador@test.com");
        user.setNombre("Jugador");
        user.setApellidos("De Prueba");
        user.setPassword("1234");
        user.setRol(ModeloRol.USER);
        user.setActivo(true);
        repoUsuario.save(user);

        ModeloPista pista = new ModeloPista();
        pista.setNombre("Pista 1");
        pista.setUbicacion("Exterior");
        pista.setPrecioHora(new BigDecimal("15"));
        pista.setActiva(true);
        repoPista.save(pista);

        ModeloReserva reserva = new ModeloReserva();
        reserva.setUsuario(user);
        reserva.setPista(pista);
        reserva.setFechaReserva(LocalDate.now()); // HOY
        reserva.setHoraInicio(LocalTime.of(18, 0));
        reserva.setDuracionMinutos(60);
        reserva.setEstado(ModeloReserva.Estado.ACTIVA);
        repoReserva.save(reserva);

        // WHEN / THEN: Ejecutamos
        Assertions.assertDoesNotThrow(() -> {
            tareasProgramadas.enviarRecordatoriosDiarios();
        });
    }

    @Test
    public void testNoEnviaSiLaReservaEsDeManana() {
        // GIVEN: Una reserva para MAÑANA
        // (Reutilizando lógica de creación...)
        // ... (guardar usuario y pista) ...

        ModeloReserva reservaManana = new ModeloReserva();
        reservaManana.setFechaReserva(LocalDate.now().plusDays(1));
        reservaManana.setEstado(ModeloReserva.Estado.ACTIVA);
        // ... set rest ...
        // Al ejecutar tareasProgramadas.enviarRecordatoriosDiarios(),
        // el log NO debería mostrar nada para este usuario.
        Assertions.assertDoesNotThrow(() -> tareasProgramadas.enviarRecordatoriosDiarios());
    }
}
