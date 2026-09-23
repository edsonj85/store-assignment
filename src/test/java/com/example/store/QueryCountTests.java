package com.example.store;

import com.example.store.config.ContainerConfig;
import com.example.store.entity.Product;
import com.example.store.repository.ProductRepository;
import com.example.store.support.QueryCountHarness;

import jakarta.persistence.EntityManagerFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
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
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(ContainerConfig.class)
@ActiveProfiles("load")
class QueryCountTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private ProductRepository productRepository;

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
                .as("GET /order/{id} should fetch-join both customer and products via the entity"
                        + " graph in the same query, not lazily load them separately")
                .isEqualTo(1);
    }

    @Test
    void getAllOrdersIssuesAConstantNumberOfStatements() throws Exception {
        long statementCount = queryCountHarness.countStatements(
                () -> mockMvc.perform(get("/order")).andExpect(status().isOk()));

        // Floor is 4, not 3: content + COUNT(*) + a batch-fetch for customer
        // (@ManyToOne) + a separate batch-fetch for products (@ManyToMany) -
        // two distinct lazy associations, so two distinct batch queries, not
        // one. Still constant regardless of page size or dataset size.
        assertThat(statementCount)
                .as("GET /order should batch-fetch both customer and products for the whole page,"
                        + " not one query per order")
                .isLessThanOrEqualTo(4);
    }

    @Test
    void getAllProductsIssuesAConstantNumberOfStatements() throws Exception {
        // product isn't part of the generated load dataset (didn't exist when
        // it was created) - seed a full page ourselves.
        for (int i = 0; i < 100; i++) {
            Product product = new Product();
            product.setDescription("Zzq7QueryCount Product " + i);
            productRepository.save(product);
        }

        long statementCount = queryCountHarness.countStatements(
                () -> mockMvc.perform(get("/products").param("size", "100")).andExpect(status().isOk()));

        assertThat(statementCount)
                .as("GET /products should issue a constant number of statements regardless of page"
                        + " size: content query + count query + 1 batched order-id lookup,"
                        + " not one lookup per product")
                .isLessThanOrEqualTo(3);
    }
}
