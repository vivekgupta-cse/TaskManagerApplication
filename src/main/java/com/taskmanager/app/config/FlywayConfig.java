package com.taskmanager.app.config;

import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Conditional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.core.env.Environment;

@Configuration
@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
@Conditional(FlywayEnabledCondition.class)
@ConditionalOnMissingBean(Flyway.class) // avoid overriding an explicitly provided Flyway bean
public class FlywayConfig {

    @Bean(initMethod = "migrate")
    public Flyway flyway(Environment env) {
        // Read JDBC connection info from environment/properties to avoid
        // compile-time dependency on a particular DataSource class.
        String url = env.getProperty("spring.datasource.url");
        String username = env.getProperty("spring.datasource.username");
        String password = env.getProperty("spring.datasource.password");

        return Flyway.configure()
                .dataSource(url, username, password)
                .baselineOnMigrate(true)
                .locations("classpath:db/migration")
                .load();
    }
}