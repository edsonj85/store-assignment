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

/**
 * Regression test for the N+1 on GET /customer. Runs against the "load" profile's realistic
 * dataset so the batch-fetch round trips actually show up — on a handful of rows they'd be too
 * small to notice.
 *
 * The 3-statement ceiling (not 2) is the real floor for a paginated, accurately-counted list
 * endpoint: the content query, a separate COUNT query for totalElements/totalPages, and one
 * batch-fetch for the lazy "orders" collection across the page (a single batch as long as the
 * page holds no more distinct customers than the batch-fetch size, which holds for any page size
 * up to the enforced 100 max). That's constant regardless of dataset size or page size.
 */
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
}
