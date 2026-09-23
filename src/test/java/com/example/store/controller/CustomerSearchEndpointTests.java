package com.example.store.controller;

import com.example.store.config.ContainerConfig;
import com.example.store.entity.Customer;
import com.example.store.repository.CustomerRepository;
import com.example.store.support.QueryCountHarness;

import jakarta.persistence.EntityManagerFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(ContainerConfig.class)
class CustomerSearchEndpointTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private QueryCountHarness queryCountHarness;

    @BeforeEach
    void setUp() {
        queryCountHarness = new QueryCountHarness(entityManagerFactory);
    }

    @Test
    void matchesMidWordSubstring() throws Exception {
        saveCustomer("Pieter van der Merwezzq1");

        mockMvc.perform(get("/customer").param("name", "erwezzq1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Pieter van der Merwezzq1"));
    }

    @Test
    void matchesCaseInsensitively() throws Exception {
        saveCustomer("Pieter van der Merwezzq2");

        mockMvc.perform(get("/customer").param("name", "ERWEZZQ2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Pieter van der Merwezzq2"));
    }

    @Test
    void multiWordQueryAndsTermsTogether() throws Exception {
        saveCustomer("Johan Vanzzq3 Zylzzq3");
        saveCustomer("Johan Vanzzq3 Onlyzzq3");

        mockMvc.perform(get("/customer").param("name", "vanzzq3 zylzzq3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Johan Vanzzq3 Zylzzq3"));
    }

    @Test
    void percentCharacterIsMatchedLiterallyNotAsAWildcard() throws Exception {
        saveCustomer("Specialzzq4%Charszzq4");

        mockMvc.perform(get("/customer").param("name", "zzq4%char"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Specialzzq4%Charszzq4"));
    }

    @Test
    void queryShorterThanMinimumReturns400() throws Exception {
        mockMvc.perform(get("/customer").param("name", "ab"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void noResultsReturnsEmptyPageNot404() throws Exception {
        mockMvc.perform(get("/customer").param("name", "zzqnonexistentzzq5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void queryCountIsBoundedRegardlessOfResultSize() throws Exception {
        for (int i = 0; i < 5; i++) {
            saveCustomer("Zzq6matchezzq6" + i);
        }

        long statementCount = queryCountHarness.countStatements(
                () -> mockMvc.perform(get("/customer").param("name", "zzq6matchezzq6"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content.length()").value(5)));

        assertThat(statementCount)
                .as("search should issue a constant number of statements regardless of result size:"
                        + " content query + count query + 1 batch-fetch for orders")
                .isLessThanOrEqualTo(3);
    }

    private Customer saveCustomer(String name) {
        Customer customer = new Customer();
        customer.setName(name);
        return customerRepository.save(customer);
    }
}
