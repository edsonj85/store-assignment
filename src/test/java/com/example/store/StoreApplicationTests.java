package com.example.store;

import com.example.store.config.ContainerConfig;
import com.example.store.repository.CustomerRepository;
import com.example.store.repository.OrderRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Boots the full application context against a real Postgres container (see {@link
 * ContainerConfig}) to prove the Liquibase changelog actually applies cleanly to it, rather than
 * trusting an in-memory database that could silently diverge. Uses the "load" profile so the
 * container ends up seeded with a realistic amount of data — startup takes a few minutes as a
 * result.
 */
@SpringBootTest
@Import(ContainerConfig.class)
@ActiveProfiles("load")
class StoreApplicationTests {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void contextLoadsAndLiquibaseAppliesSchemaAgainstPostgres() {
        assertThat(customerRepository.findAll()).isNotNull();
        assertThat(orderRepository.findAll()).isNotNull();
    }
}
