package com.example.store.service;

import com.example.store.dto.CustomerReferenceDTO;
import com.example.store.dto.OrderCreateDTO;
import com.example.store.dto.OrderCustomerDTO;
import com.example.store.dto.OrderDTO;
import com.example.store.dto.PageResponse;
import com.example.store.entity.Customer;
import com.example.store.entity.Order;
import com.example.store.exception.ResourceNotFoundException;
import com.example.store.mapper.OrderMapper;
import com.example.store.repository.CustomerRepository;
import com.example.store.repository.OrderRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTests {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private CustomerRepository customerRepository;

    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderServiceImpl(orderRepository, orderMapper, customerRepository);
    }

    @Test
    void getAllOrdersReturnsPagedEnvelope() {
        Customer customer = new Customer();
        customer.setId(1L);
        customer.setName("Takudzwa Jengwa");

        Order order = new Order();
        order.setId(1L);
        order.setDescription("Test Order");
        order.setCustomer(customer);

        OrderCustomerDTO orderCustomerDTO = new OrderCustomerDTO();
        orderCustomerDTO.setId(1L);
        orderCustomerDTO.setName("Takudzwa Jengwa");

        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setId(1L);
        orderDTO.setDescription("Test Order");
        orderDTO.setCustomer(orderCustomerDTO);

        // page=2 -> Pageable offset 1
        Pageable pageable = PageRequest.of(1, 20);
        PageImpl<Order> orderPage = new PageImpl<>(List.of(order), pageable, 21);

        when(orderRepository.findAll(pageable)).thenReturn(orderPage);
        when(orderMapper.ordersToOrderDTOs(List.of(order))).thenReturn(List.of(orderDTO));

        PageResponse<OrderDTO> result = orderService.getAllOrders(2, 20);

        assertThat(result.getContent()).containsExactly(orderDTO);
        assertThat(result.getPage()).isEqualTo(2);
        assertThat(result.getTotalElements()).isEqualTo(21);
        assertThat(result.getTotalPages()).isEqualTo(2);
    }

    @Test
    void getOrderByIdReturnsDTO() {
        Customer customer = new Customer();
        customer.setId(1L);
        customer.setName("Takudzwa Jengwa");

        Order order = new Order();
        order.setId(1L);
        order.setDescription("Test Order");
        order.setCustomer(customer);

        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setId(1L);
        orderDTO.setDescription("Test Order");

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderMapper.orderToOrderDTO(order)).thenReturn(orderDTO);

        OrderDTO result = orderService.getOrderById(1L);

        assertThat(result).isEqualTo(orderDTO);
    }

    @Test
    void getOrderByIdThrowsWhenNotFound() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById(999L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createOrderLooksUpCustomerMapsSavesAndReturnsDTO() {
        CustomerReferenceDTO customerReference = new CustomerReferenceDTO();
        customerReference.setId(1L);

        OrderCreateDTO request = new OrderCreateDTO();
        request.setDescription("New Order");
        request.setCustomer(customerReference);

        Customer customer = new Customer();
        customer.setId(1L);
        customer.setName("Takudzwa Jengwa");

        Order mapped = new Order();
        mapped.setDescription("New Order");

        Order saved = new Order();
        saved.setId(2L);
        saved.setDescription("New Order");
        saved.setCustomer(customer);

        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setId(2L);
        orderDTO.setDescription("New Order");

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(orderMapper.orderCreateDTOToOrder(request)).thenReturn(mapped);
        when(orderRepository.save(mapped)).thenReturn(saved);
        when(orderMapper.orderToOrderDTO(saved)).thenReturn(orderDTO);

        OrderDTO result = orderService.createOrder(request);

        assertThat(result).isEqualTo(orderDTO);
        assertThat(mapped.getCustomer()).isEqualTo(customer);
    }

    @Test
    void createOrderThrowsWhenCustomerNotFound() {
        CustomerReferenceDTO customerReference = new CustomerReferenceDTO();
        customerReference.setId(999L);

        OrderCreateDTO request = new OrderCreateDTO();
        request.setDescription("New Order");
        request.setCustomer(customerReference);

        when(customerRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrder(request)).isInstanceOf(ResourceNotFoundException.class);
    }
}
