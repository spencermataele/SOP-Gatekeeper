package com.woven.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Shared, fail-closed configuration for tests that use real MySQL. */
@SpringBootTest
@ActiveProfiles("test")
public abstract class DatabaseTest {
    @DynamicPropertySource
    static void isolatedDatabase(DynamicPropertyRegistry properties) throws IOException {
        String password = System.getenv("GATEKEEPER_TEST_DB_PASSWORD");
        if (password == null || password.isBlank()) {
            Path root = Path.of(System.getProperty("user.dir")).toAbsolutePath();
            if (root.getFileName().toString().equals("backend")) {
                root = root.getParent();
            }
            Path config = root.resolve(".local/test-db.properties");
            if (!Files.isRegularFile(config)) {
                throw new IllegalStateException("Test database is not configured. Run ./scripts/dev.ps1 prepare-tests. "
                        + "Tests never fall back to application database credentials.");
            }
            Properties local = new Properties();
            try (var input = Files.newInputStream(config)) {
                local.load(input);
            }
            password = local.getProperty("password");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("A dedicated test database password is required.");
        }
        String testPassword = password;
        // Deliberately fixed loopback host, schema, port and user: no production URL override.
        String url = "jdbc:mysql://127.0.0.1:3307/sop_gatekeeper_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        properties.add("spring.datasource.url", () -> url);
        properties.add("spring.datasource.username", () -> "gatekeeper_test");
        properties.add("spring.datasource.password", () -> testPassword);
        // Flyway has independent connection overrides; constrain those too.
        properties.add("spring.flyway.url", () -> url);
        properties.add("spring.flyway.user", () -> "gatekeeper_test");
        properties.add("spring.flyway.password", () -> testPassword);
        properties.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        properties.add("app.jwt.secret", () -> "test-only-signing-key-never-use-outside-automated-tests-1234567890");
        properties.add("spring.flyway.clean-disabled", () -> "true");
        properties.add("spring.flyway.baseline-on-migrate", () -> "false");
    }
}
