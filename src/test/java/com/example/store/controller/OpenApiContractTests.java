package com.example.store.controller;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.report.LevelResolver;
import com.atlassian.oai.validator.report.ValidationReport;
import com.example.store.config.ContainerConfig;
import com.example.store.entity.Customer;
import com.example.store.entity.Order;
import com.example.store.entity.Product;
import com.example.store.repository.CustomerRepository;
import com.example.store.repository.OrderRepository;
import com.example.store.repository.ProductRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers.openApi;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Validates real HTTP responses against OpenAPI.yaml itself, for both success
// and error shapes, so the spec can't silently drift from what the API
// actually returns. Other controller tests assert behaviour; this asserts
// the contract describing it.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(ContainerConfig.class)
class OpenApiContractTests {

    private static final String SPEC = "OpenAPI.yaml";

    // Request-side validation deliberately turned off: these two tests send a
    // request that is intentionally invalid per the spec (non-numeric id,
    // zero products) to exercise the server's own 400 handling - the thing
    // under test is that the *response* still matches the documented
    // ProblemDetail shape, not that the test's own bad input is spec-valid.
    private static final OpenApiInteractionValidator RESPONSE_ONLY_VALIDATOR = OpenApiInteractionValidator.createFor(
                    SPEC)
            .withLevelResolver(LevelResolver.create()
                    .withLevel("validation.request", ValidationReport.Level.IGNORE)
                    .build())
            .build();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void getCustomersMatchesSpec() throws Exception {
        mockMvc.perform(get("/customer"))
                .andExpect(status().isOk())
                .andExpect(openApi().isValid(SPEC));
    }

    @Test
    void searchQueryTooShortMatchesSpec() throws Exception {
        mockMvc.perform(get("/customer").param("name", "zz"))
                .andExpect(status().isBadRequest())
                .andExpect(openApi().isValid(SPEC));
    }

    @Test
    void createCustomerBlankNameMatchesSpec() throws Exception {
        mockMvc.perform(post("/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(openApi().isValid(SPEC));
    }

    @Test
    void createCustomerMatchesSpec() throws Exception {
        mockMvc.perform(post("/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Zzq5 Contract Customer\"}"))
                .andExpect(status().isCreated())
                .andExpect(openApi().isValid(SPEC));
    }

    @Test
    void getOrdersMatchesSpec() throws Exception {
        mockMvc.perform(get("/order"))
                .andExpect(status().isOk())
                .andExpect(openApi().isValid(SPEC));
    }

    @Test
    void getOrderByIdMatchesSpec() throws Exception {
        Customer customer = saveCustomer("Zzq5 Order Customer");
        Product product = saveProduct("Zzq5 Order Product");
        Order order = saveOrder(customer, product);

        mockMvc.perform(get("/order/{id}", order.getId()))
                .andExpect(status().isOk())
                .andExpect(openApi().isValid(SPEC));
    }

    @Test
    void orderNotFoundMatchesSpec() throws Exception {
        mockMvc.perform(get("/order/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(openApi().isValid(SPEC));
    }

    @Test
    void orderNonNumericIdMatchesSpec() throws Exception {
        mockMvc.perform(get("/order/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(openApi().isValid(RESPONSE_ONLY_VALIDATOR));
    }

    @Test
    void createOrderWithNoProductsMatchesSpec() throws Exception {
        Customer customer = saveCustomer("Zzq5 No Products Customer");

        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Zzq5\",\"customer\":{\"id\":" + customer.getId()
                                + "},\"products\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(openApi().isValid(RESPONSE_ONLY_VALIDATOR));
    }

    @Test
    void createOrderMissingProductMatchesSpec() throws Exception {
        Customer customer = saveCustomer("Zzq5 Missing Product Customer");

        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Zzq5\",\"customer\":{\"id\":" + customer.getId()
                                + "},\"products\":[{\"id\":999999999}]}"))
                .andExpect(status().isNotFound())
                .andExpect(openApi().isValid(SPEC));
    }

    @Test
    void createOrderMatchesSpec() throws Exception {
        Customer customer = saveCustomer("Zzq5 Create Order Customer");
        Product product = saveProduct("Zzq5 Create Order Product");

        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Zzq5\",\"customer\":{\"id\":" + customer.getId()
                                + "},\"products\":[{\"id\":" + product.getId() + "}]}"))
                .andExpect(status().isCreated())
                .andExpect(openApi().isValid(SPEC));
    }

    @Test
    void getProductsMatchesSpec() throws Exception {
        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(openApi().isValid(SPEC));
    }

    @Test
    void createProductBlankDescriptionMatchesSpec() throws Exception {
        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(openApi().isValid(SPEC));
    }

    @Test
    void createProductMatchesSpec() throws Exception {
        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Zzq5 Contract Product\"}"))
                .andExpect(status().isCreated())
                .andExpect(openApi().isValid(SPEC));
    }

    @Test
    void productNotFoundMatchesSpec() throws Exception {
        mockMvc.perform(get("/products/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(openApi().isValid(SPEC));
    }

    private Customer saveCustomer(String name) {
        Customer customer = new Customer();
        customer.setName(name);
        return customerRepository.save(customer);
    }

    private Product saveProduct(String description) {
        Product product = new Product();
        product.setDescription(description);
        return productRepository.save(product);
    }

    private Order saveOrder(Customer customer, Product... products) {
        Order order = new Order();
        order.setDescription("Order for " + customer.getName());
        order.setCustomer(customer);
        order.setProducts(Set.of(products));
        return orderRepository.save(order);
    }
}
