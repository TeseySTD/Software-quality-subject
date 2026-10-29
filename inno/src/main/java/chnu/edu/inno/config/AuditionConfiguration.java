package chnu.edu.inno.config;

/**
 * @author Rout
 * @version 1.0.0
 * @project inno
 * @class AuditionConfiguration
 * @since 29.10.2026 - 23.09
 */

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Configuration
@EnableMongoAuditing(dateTimeProviderRef = "auditDateTimeProvider")
public class AuditionConfiguration {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> Optional.of("george");
    }

    // MongoDB stores dates with millisecond precision; truncating here keeps Javers from reporting phantom date changes
    @Bean
    public DateTimeProvider auditDateTimeProvider() {
        return () -> Optional.of(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS));
    }
}
