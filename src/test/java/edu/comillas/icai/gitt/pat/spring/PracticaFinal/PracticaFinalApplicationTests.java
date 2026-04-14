package edu.comillas.icai.gitt.pat.spring.PracticaFinal;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = {
				"spring.task.scheduling.enabled=false",
				"spring.profiles.active=test"
		}
)
@ActiveProfiles("test")
class PracticaFinalApplicationTests {
	@Test
	void contextLoads() {
		// Si este test pasa, es que la base de datos y Spring están bien configurados
	}
}
