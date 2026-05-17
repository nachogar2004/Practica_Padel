package edu.comillas.icai.gitt.pat.spring.PracticaFinal;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuración CORS (Cross-Origin Resource Sharing) para la aplicación.
 *
 * <p>CORS es un mecanismo de seguridad de los navegadores que bloquea las peticiones
 * HTTP que vienen de un dominio diferente al del servidor. Sin esta configuración,
 * el frontend (HTML/JS en {@code file://} o GitHub Pages) no podría llamar a la API.</p>
 *
 * <h3>Decisiones de diseño</h3>
 * <ul>
 *   <li><b>{@code allowedOriginPatterns("*")}</b>: acepta peticiones de cualquier origen,
 *       incluyendo {@code file://} (abrir el HTML directamente) y GitHub Pages.
 *       Se usa {@code allowedOriginPatterns} en lugar de {@code allowedOrigins} porque
 *       es compatible con {@code allowCredentials(true)}.</li>
 *   <li><b>{@code allowCredentials(true)}</b>: necesario para que el navegador envíe
 *       la cookie de sesión ({@code JSESSIONID}) en las peticiones cross-origin.
 *       Sin esto, la sesión HTTP no funcionaría desde el frontend.</li>
 *   <li><b>Solo para {@code /pistaPadel/**}</b>: el mapping limita CORS al path de la API,
 *       dejando otros posibles recursos sin esa política permisiva.</li>
 * </ul>
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    /** Registra las reglas CORS en Spring MVC. Spring llama a este método al arrancar. */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/pistaPadel/**")
                .allowedOriginPatterns("*")   // permite file://, localhost y GitHub Pages
                .allowedMethods("GET", "POST", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
