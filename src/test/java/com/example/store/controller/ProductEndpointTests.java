package com.example.store.controller;

import com.example.store.config.ContainerConfig;
import com.example.store.entity.Customer;
import com.example.store.entity.Order;
import com.example.store.entity.Product;
import com.example.store.repository.CustomerRepository;
import com.example.store.repository.OrderRepository;
import com.example.store.repository.ProductRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Full stack against real Postgres - what mocked controller tests can't prove:
// the create-then-retrieve round trip, and the order-id projection actually
// producing correct results through the real HTTP surface.
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(ContainerConfig.class)
class ProductEndpointTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    void createProductThenRetrieveById() throws Exception {
        String body = mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Zzq1 Widget\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("Zzq1 Widget"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long id = objectMapper.readTree(body).get("id").asLong();

        mockMvc.perform(get("/products/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.description").value("Zzq1 Widget"))
                .andExpect(jsonPath("$.orders").isEmpty());
    }

    @Test
    void getAllProductsIncludesCorrectOrderIdsAndEmptyListForUnlinkedProducts() throws Exception {
        Product linked = saveProduct("Zzq2 Linked");
        Product unlinked = saveProduct("Zzq2 Unlinked");

        Customer customer = saveCustomer("Zzq2 Customer");
        Order order = saveOrder(customer, linked);

        String body = mockMvc.perform(get("/products").param("size", "100"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode content = objectMapper.readTree(body).get("content");
        JsonNode linkedOrders = findProductNode(content, linked.getId()).get("orders");
        assertThat(linkedOrders).as("linked product's orders").hasSize(1);
        assertThat(linkedOrders.get(0).asLong()).isEqualTo(order.getId());

        assertThat(findProductNode(content, unlinked.getId()).get("orders"))
                .as("unlinked product's orders")
                .isEmpty();
    }

    private static JsonNode findProductNode(JsonNode content, Long productId) {
        for (JsonNode node : content) {
            if (node.get("id").asLong() == productId) {
                return node;
            }
        }
        throw new AssertionError("No product with id " + productId + " in response content");
    }

    @Test
    void orderResponseIncludesItsProducts() throws Exception {
        Product product = saveProduct("Zzq3 Widget");
        Customer customer = saveCustomer("Zzq3 Customer");
        Order order = saveOrder(customer, product);

        mockMvc.perform(get("/order/{id}", order.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products.length()").value(1))
                .andExpect(jsonPath("$.products[0].id").value(product.getId()))
                .andExpect(jsonPath("$.products[0].description").value("Zzq3 Widget"));
    }

    private Product saveProduct(String description) {
        Product product = new Product();
        product.setDescription(description);
        return productRepository.save(product);
    }

    private Customer saveCustomer(String name) {
        Customer customer = new Customer();
        customer.setName(name);
        return customerRepository.save(customer);
    }

    private Order saveOrder(Customer customer, Product... products) {
        Order order = new Order();
        order.setDescription("Order for " + customer.getName());
        order.setCustomer(customer);
        order.setProducts(Set.of(products));
        return orderRepository.save(order);
    }
}
