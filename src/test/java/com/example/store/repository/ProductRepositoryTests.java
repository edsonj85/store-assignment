package com.example.store.repository;

import com.example.store.config.ContainerConfig;
import com.example.store.entity.Customer;
import com.example.store.entity.Order;
import com.example.store.entity.Product;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

// Real Postgres via ContainerConfig - the query joins through order_product.
@Tag("integration")
@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@Import(ContainerConfig.class)
class ProductRepositoryTests {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    void findOrderIdsByProductIdsReturnsOnePairPerOrderProductAssociation() {
        Product widget = productRepository.save(newProduct("Widgetzzq"));
        Product gadget = productRepository.save(newProduct("Gadgetzzq"));

        Customer customer = customerRepository.save(newCustomer("Takudzwa Jengwa"));

        Order firstOrder = newOrder(customer, "First order", widget);
        Order secondOrder = newOrder(customer, "Second order", widget, gadget);
        orderRepository.save(firstOrder);
        orderRepository.save(secondOrder);

        List<ProductOrderIdProjection> result =
                productRepository.findOrderIdsByProductIds(List.of(widget.getId(), gadget.getId()));

        assertThat(result)
                .extracting(ProductOrderIdProjection::getProductId, ProductOrderIdProjection::getOrderId)
                .containsExactlyInAnyOrder(
                        tuple(widget.getId(), firstOrder.getId()),
                        tuple(widget.getId(), secondOrder.getId()),
                        tuple(gadget.getId(), secondOrder.getId()));
    }

    @Test
    void findOrderIdsByProductIdsReturnsEmptyForAProductWithNoOrders() {
        Product lonely = productRepository.save(newProduct("Lonelyzzq"));

        List<ProductOrderIdProjection> result = productRepository.findOrderIdsByProductIds(List.of(lonely.getId()));

        assertThat(result).isEmpty();
    }

    private static Product newProduct(String description) {
        Product product = new Product();
        product.setDescription(description);
        return product;
    }

    private static Customer newCustomer(String name) {
        Customer customer = new Customer();
        customer.setName(name);
        return customer;
    }

    private static Order newOrder(Customer customer, String description, Product... products) {
        Order order = new Order();
        order.setDescription(description);
        order.setCustomer(customer);
        Set<Product> productSet = new HashSet<>(List.of(products));
        order.setProducts(productSet);
        return order;
    }
}
