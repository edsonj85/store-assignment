package com.example.store.service;

import com.example.store.dto.OrderCreateDTO;
import com.example.store.dto.OrderDTO;
import com.example.store.dto.PageResponse;
import com.example.store.dto.ProductReferenceDTO;
import com.example.store.entity.Customer;
import com.example.store.entity.Order;
import com.example.store.entity.Product;
import com.example.store.exception.InvalidOrderException;
import com.example.store.exception.ResourceNotFoundException;
import com.example.store.mapper.OrderMapper;
import com.example.store.repository.CustomerRepository;
import com.example.store.repository.OrderRepository;
import com.example.store.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderDTO> getAllOrders(int page, int size) {
        Page<Order> orderPage = orderRepository.findAll(PageRequest.of(page - 1, size));
        List<OrderDTO> content = orderMapper.ordersToOrderDTOs(orderPage.getContent());
        return PageResponse.of(content, orderPage);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDTO getOrderById(Long id) {
        Order order = orderRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Order", id));
        return orderMapper.orderToOrderDTO(order);
    }

    @Override
    @Transactional
    public OrderDTO createOrder(OrderCreateDTO orderCreateDTO) {
        List<ProductReferenceDTO> productReferences = orderCreateDTO.getProducts();
        if (productReferences == null || productReferences.isEmpty()) {
            throw new InvalidOrderException("Order must contain at least one product");
        }

        Long customerId = orderCreateDTO.getCustomer().getId();
        Customer customer = customerRepository
                .findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId));

        Order order = orderMapper.orderCreateDTOToOrder(orderCreateDTO);
        order.setCustomer(customer);
        order.setProducts(resolveProducts(productReferences));

        return orderMapper.orderToOrderDTO(orderRepository.save(order));
    }

    private Set<Product> resolveProducts(List<ProductReferenceDTO> productReferences) {
        Set<Long> productIds = new LinkedHashSet<>();
        for (ProductReferenceDTO reference : productReferences) {
            productIds.add(reference.getId());
        }

        Map<Long, Product> productsById = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        Set<Product> products = new HashSet<>();
        for (Long productId : productIds) {
            Product product = productsById.get(productId);
            if (product == null) {
                throw new ResourceNotFoundException("Product", productId);
            }
            products.add(product);
        }
        return products;
    }
}
