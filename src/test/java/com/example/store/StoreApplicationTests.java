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
 * ContainerConfig}), proving the Liquibase changelog applies cleanly to it rather than relying on
 * a divergent in-memory database. The "load" profile (see {@code application-load.yaml}, also used
 * to load-test the app via {@code bootRun}) points Liquibase at the realistic generated dataset
 * ({@code data-load.sql}, ~135k rows) instead of the small dataset the default changelog seeds for
 * everyday {@code bootRun} use — later integration tests (query-count assertions, index
 * verification) need the realistic volume to be meaningful. Expect this test to be slow (minutes,
 * not seconds) as a result.
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
