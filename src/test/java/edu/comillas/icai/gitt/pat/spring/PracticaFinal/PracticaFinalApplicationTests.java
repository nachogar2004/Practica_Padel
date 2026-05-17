package edu.comillas.icai.gitt.pat.spring.PracticaFinal;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Test de smoke (humo) que verifica que el contexto de Spring Boot arranca correctamente.
 *
 * <p>Si este test pasa, significa que:
 * <ul>
 *   <li>Todos los beans de Spring se han creado sin errores.</li>
 *   <li>La base de datos H2 de test se ha inicializado correctamente.</li>
 *   <li>No hay conflictos de configuración ni dependencias circulares.</li>
 * </ul>
 * </p>
 *
 * <h3>Anotaciones clave</h3>
 * <ul>
 *   <li>{@code @SpringBootTest}: arranca el contexto completo de la aplicación para el test.
 *       {@code RANDOM_PORT} inicia un servidor en un puerto aleatorio para evitar conflictos.</li>
 *   <li>{@code @ActiveProfiles("test")}: activa el perfil "test", que puede tener su propia
 *       configuración en {@code application-test.properties}.</li>
 *   <li>{@code spring.task.scheduling.enabled=false}: desactiva los cron jobs durante los tests
 *       para que no interfieran con la ejecución.</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = {
				"spring.task.scheduling.enabled=false",
				"spring.profiles.active=test"
		}
)
@ActiveProfiles("test")
class PracticaFinalApplicationTests {
	/**
	 * Verifica que el contexto de Spring arranca sin errores.
	 * Si el contexto falla al arrancar (configuración errónea, bean no encontrado, etc.),
	 * este test lanzará una excepción y fallará automáticamente.
	 */
	@Test
	void contextLoads() {
		// Si este test pasa, es que la base de datos y Spring están bien configurados
	}

	/**
	 * Llama directamente al método {@code main} para que JaCoCo lo marque como cubierto.
	 * Se pasan {@code --server.port=0} y {@code --spring.profiles.active=test} para que
	 * el nuevo contexto arranque en un puerto aleatorio y use la BD H2 de test,
	 * evitando conflictos con otros tests en ejecución.
	 */
	@Test
	void mainArrancsCorrectamente() {
		PracticaFinalApplication.main(new String[]{
				"--spring.profiles.active=test",
				"--server.port=0"
		});
	}
}
