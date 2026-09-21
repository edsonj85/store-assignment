package com.example.store.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Shared Postgres container for integration tests. Imported (rather than declared per test
 * class) so Spring's test context cache can reuse one running container across test classes,
 * instead of starting a new one per class. This only applies to classes that end up with an
 * identical context configuration (same profile, same test annotations, etc) — a class that
 * adds something like {@code @AutoConfigureMockMvc} gets its own cache entry and its own
 * container, even though it imports this same config.
 */
@TestConfiguration(proxyBeanMethods = false)
public class ContainerConfig {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>(DockerImageName.parse("postgres:16.2"));
    }
}
