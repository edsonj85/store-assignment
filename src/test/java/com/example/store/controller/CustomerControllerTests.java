package com.example.store.controller;

import com.example.store.dto.CustomerCreateDTO;
import com.example.store.dto.CustomerDTO;
import com.example.store.dto.PageResponse;
import com.example.store.service.CustomerService;
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

@WebMvcTest(CustomerController.class)
class CustomerControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CustomerService customerService;

    private CustomerDTO customerDTO;

    @BeforeEach
    void setUp() {
        customerDTO = new CustomerDTO();
        customerDTO.setId(1L);
        customerDTO.setName("John Doe");
    }

    @Test
    void testCreateCustomer() throws Exception {
        CustomerCreateDTO request = new CustomerCreateDTO();
        request.setName("John Doe");

        when(customerService.createCustomer(request)).thenReturn(customerDTO);

        mockMvc.perform(post("/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("John Doe"));
    }

    @Test
    void testCreateCustomerBlankNameReturns400() throws Exception {
        CustomerCreateDTO request = new CustomerCreateDTO();
        request.setName(" ");

        mockMvc.perform(post("/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetAllCustomersUsesDefaultPageAndSize() throws Exception {
        // page=1 (1-indexed, first page) is Spring Pageable's zero-indexed page 0
        PageResponse<CustomerDTO> page = PageResponse.of(List.of(customerDTO), zeroIndexedPageOf(0, 20, 1));
        when(customerService.getAllCustomers(1, 20)).thenReturn(page);

        mockMvc.perform(get("/customer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("John Doe"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void testGetAllCustomersWithExplicitPageAndSize() throws Exception {
        // page=2 (1-indexed) is Spring Pageable's zero-indexed page 1
        PageResponse<CustomerDTO> page = PageResponse.of(List.of(customerDTO), zeroIndexedPageOf(1, 10, 25));
        when(customerService.getAllCustomers(2, 10)).thenReturn(page);

        mockMvc.perform(get("/customer").param("page", "2").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(10));
    }

    @Test
    void testGetAllCustomersSizeOverMaxReturns400() throws Exception {
        mockMvc.perform(get("/customer").param("size", "101")).andExpect(status().isBadRequest());
    }

    @Test
    void testGetAllCustomersSizeZeroReturns400() throws Exception {
        mockMvc.perform(get("/customer").param("size", "0")).andExpect(status().isBadRequest());
    }

    @Test
    void testGetAllCustomersPageZeroReturns400() throws Exception {
        mockMvc.perform(get("/customer").param("page", "0")).andExpect(status().isBadRequest());
    }

    @Test
    void testGetAllCustomersNegativePageReturns400() throws Exception {
        mockMvc.perform(get("/customer").param("page", "-1")).andExpect(status().isBadRequest());
    }

    private static Page<Object> zeroIndexedPageOf(int zeroIndexedPage, int size, long totalElements) {
        return new PageImpl<>(List.of(), PageRequest.of(zeroIndexedPage, size), totalElements);
    }
}
