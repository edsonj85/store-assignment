package com.example.store;

import com.example.store.config.ContainerConfig;
import com.example.store.support.QueryCountHarness;

import jakarta.persistence.EntityManagerFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// N+1 regression test, run against the "load" profile so batch-fetch round trips actually show up.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(ContainerConfig.class)
@ActiveProfiles("load")
class QueryCountTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private QueryCountHarness queryCountHarness;

    @BeforeEach
    void setUp() {
        queryCountHarness = new QueryCountHarness(entityManagerFactory);
    }

    @Test
    void getAllCustomersIssuesAConstantNumberOfStatements() throws Exception {
        long statementCount = queryCountHarness.countStatements(
                () -> mockMvc.perform(get("/customer")).andExpect(status().isOk()));

        assertThat(statementCount)
                .as("GET /customer should issue a constant number of statements regardless of"
                        + " how many customers/orders exist, not one batch per page of customers")
                .isLessThanOrEqualTo(3);
    }

    @Test
    void getOrderByIdIssuesExactlyOneStatement() throws Exception {
        long statementCount = queryCountHarness.countStatements(
                () -> mockMvc.perform(get("/order/{id}", 1)).andExpect(status().isOk()));

        assertThat(statementCount)
                .as("GET /order/{id} should fetch-join the customer via the entity graph, not lazily"
                        + " load it in a second statement")
                .isEqualTo(1);
    }
}
