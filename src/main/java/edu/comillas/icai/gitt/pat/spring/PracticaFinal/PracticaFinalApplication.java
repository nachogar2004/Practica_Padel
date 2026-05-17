package edu.comillas.icai.gitt.pat.spring.PracticaFinal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Clase principal de la aplicación Spring Boot.
 *
 * <p>Es el punto de entrada del programa. El método {@code main} arranca el contenedor
 * de Spring, que:
 * <ol>
 *   <li>Configura el contexto de la aplicación (carga beans, repositorios, servicios...).</li>
 *   <li>Crea el esquema de BD (Hibernate DDL-auto update).</li>
 *   <li>Arranca el servidor Tomcat embebido en el puerto 8080.</li>
 * </ol>
 * </p>
 *
 * <h3>Qué hace {@code @SpringBootApplication}</h3>
 * <p>Es un atajo que combina tres anotaciones:
 * <ul>
 *   <li>{@code @Configuration}: marca la clase como fuente de beans de Spring.</li>
 *   <li>{@code @EnableAutoConfiguration}: activa la autoconfiguración de Spring Boot
 *       (detecta dependencias y configura Tomcat, JPA, H2, etc. automáticamente).</li>
 *   <li>{@code @ComponentScan}: escanea el paquete y subpaquetes buscando
 *       {@code @Component}, {@code @Service}, {@code @Repository}, {@code @Controller}...</li>
 * </ul>
 * </p>
 */
@SpringBootApplication
public class PracticaFinalApplication {

	/**
	 * Método principal: punto de entrada de la JVM.
	 * Delega en {@code SpringApplication.run()} que hace todo el arranque.
	 *
	 * @param args argumentos de línea de comandos (no se usan en este proyecto)
	 */
	public static void main(String[] args) {
		SpringApplication.run(PracticaFinalApplication.class, args);
	}

}