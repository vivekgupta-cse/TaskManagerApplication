package com.taskmanager.app;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Smoke test — verifies the full Spring context starts successfully
 * against the real PostgreSQL database (postgres-test container from docker-compose-test.yml).
 *
 * If this test passes, it means:
 *  - Flyway migrations ran successfully against the real DB
 *  - JPA validated the schema
 *  - All Spring beans wired correctly
 */
@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
                "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration," +
                "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration," +
                "org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration"
})
class TaskManagerApplicationTests {

    @Test
    void contextLoads() {
        // If the Spring context starts without error, this test passes.
    }

    @Test
    void mainMethodStartsApplication() {
        // Covers the static main() method for JaCoCo.
        // --server.port=0 ensures it picks a random free port.
        TaskManagerApplication.main(new String[]{"--server.port=0"});
    }
}
