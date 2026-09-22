package com.example.store.repository;

import com.example.store.config.ContainerConfig;
import com.example.store.entity.Customer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

// Real Postgres via ContainerConfig, not H2 - ILIKE/ESCAPE need real Postgres semantics.
@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@Import(ContainerConfig.class)
class CustomerSearchRepositoryImplTests {

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    void multiWordQueryAndsTermsTogether() {
        customerRepository.save(newCustomer("Johan Vanzzq Zylzzq"));
        customerRepository.save(newCustomer("Johan Vanzzq Onlyzzq"));

        Page<Customer> result = customerRepository.searchByName(List.of("vanzzq", "zylzzq"), PageRequest.of(0, 20));

        assertThat(result.getContent())
                .extracting(Customer::getName)
                .contains("Johan Vanzzq Zylzzq")
                .doesNotContain("Johan Vanzzq Onlyzzq");
    }

    @Test
    void percentCharacterIsMatchedLiterallyNotAsAWildcard() {
        customerRepository.save(newCustomer("Specialzzq%Charszzq"));

        // 200 covers the whole seeded table in one page
        Page<Customer> result = customerRepository.searchByName(List.of("%"), PageRequest.of(0, 200));

        assertThat(result.getContent()).extracting(Customer::getName).containsExactly("Specialzzq%Charszzq");
    }

    @Test
    void matchesCaseInsensitivelyMidWord() {
        customerRepository.save(newCustomer("Pieter van der Merwezzq"));

        Page<Customer> result = customerRepository.searchByName(List.of("ERWEZZQ"), PageRequest.of(0, 20));

        assertThat(result.getContent()).extracting(Customer::getName).contains("Pieter van der Merwezzq");
    }

    private static Customer newCustomer(String name) {
        Customer customer = new Customer();
        customer.setName(name);
        return customer;
    }
}
