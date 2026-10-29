package chnu.edu.inno.config;

/**
 * @author Rout
 * @version 1.0.0
 * @project inno
 * @class JaversConfiguration
 * @since 29.10.2026 - 23.09
 */

import org.javers.spring.auditable.AuthorProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JaversConfiguration {

    @Bean
    public AuthorProvider authorProvider() {
        return () -> "ADMIN";
    }
}
