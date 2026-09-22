package com.example.store.controller;

import com.example.store.dto.CustomerReferenceDTO;
import com.example.store.dto.OrderCreateDTO;
import com.example.store.dto.OrderCustomerDTO;
import com.example.store.dto.OrderDTO;
import com.example.store.dto.PageResponse;
import com.example.store.exception.ResourceNotFoundException;
import com.example.store.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class OrderControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    private OrderCreateDTO createRequest;
    private OrderDTO orderDTO;

    @BeforeEach
    void setUp() {
        CustomerReferenceDTO customerReference = new CustomerReferenceDTO();
        customerReference.setId(1L);

        createRequest = new OrderCreateDTO();
        createRequest.setDescription("Test Order");
        createRequest.setCustomer(customerReference);

        OrderCustomerDTO orderCustomerDTO = new OrderCustomerDTO();
        orderCustomerDTO.setId(1L);
        orderCustomerDTO.setName("Takudzwa Jengwa");

        orderDTO = new OrderDTO();
        orderDTO.setId(1L);
        orderDTO.setDescription("Test Order");
        orderDTO.setCustomer(orderCustomerDTO);
    }

    @Test
    void testCreateOrder() throws Exception {
        when(orderService.createOrder(createRequest)).thenReturn(orderDTO);

        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("Test Order"))
                .andExpect(jsonPath("$.customer.name").value("Takudzwa Jengwa"));
    }

    @Test
    void testCreateOrderCustomerNotFoundReturns404() throws Exception {
        when(orderService.createOrder(createRequest)).thenThrow(new ResourceNotFoundException("Customer", 1L));

        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    void testCreateOrderBlankDescriptionReturns400() throws Exception {
        createRequest.setDescription(" ");

        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetOrderByIdFound() throws Exception {
        when(orderService.getOrderById(1L)).thenReturn(orderDTO);

        mockMvc.perform(get("/order/{id}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Test Order"))
                .andExpect(jsonPath("$.customer.id").value(1))
                .andExpect(jsonPath("$.customer.name").value("Takudzwa Jengwa"));
    }

    @Test
    void testGetOrderByIdNotFound() throws Exception {
        when(orderService.getOrderById(999L)).thenThrow(new ResourceNotFoundException("Order", 999L));

        mockMvc.perform(get("/order/{id}", 999))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Order 999 not found"));
    }

    @Test
    void testGetOrderByIdNonNumericReturns400() throws Exception {
        mockMvc.perform(get("/order/{id}", "abc")).andExpect(status().isBadRequest());
    }

    @Test
    void testGetOrderUsesDefaultPageAndSize() throws Exception {
        // page=1 -> Pageable offset 0
        PageResponse<OrderDTO> page = PageResponse.of(List.of(orderDTO), zeroIndexedPageOf(0, 20, 1));
        when(orderService.getAllOrders(1, 20)).thenReturn(page);

        mockMvc.perform(get("/order"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].description").value("Test Order"))
                .andExpect(jsonPath("$.content[0].customer.name").value("Takudzwa Jengwa"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void testGetOrderSizeOverMaxReturns400() throws Exception {
        mockMvc.perform(get("/order").param("size", "101")).andExpect(status().isBadRequest());
    }

    @Test
    void testGetOrderPageZeroReturns400() throws Exception {
        mockMvc.perform(get("/order").param("page", "0")).andExpect(status().isBadRequest());
    }

    @Test
    void testGetOrderNegativePageReturns400() throws Exception {
        mockMvc.perform(get("/order").param("page", "-1")).andExpect(status().isBadRequest());
    }

    private static Page<Object> zeroIndexedPageOf(int zeroIndexedPage, int size, long totalElements) {
        return new PageImpl<>(List.of(), PageRequest.of(zeroIndexedPage, size), totalElements);
    }
}
