package com.example.store;

import com.example.store.config.ContainerConfig;
import com.example.store.repository.CustomerRepository;
import com.example.store.repository.OrderRepository;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

// Confirms Liquibase applies cleanly against real Postgres, not an in-memory stand-in.
@Tag("integration")
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
