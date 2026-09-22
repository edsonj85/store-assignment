package com.example.store.controller;

import com.example.store.dto.CustomerCreateDTO;
import com.example.store.dto.CustomerDTO;
import com.example.store.dto.PageResponse;
import com.example.store.exception.InvalidSearchQueryException;
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
        customerDTO.setName("Takudzwa Jengwa");
    }

    @Test
    void testCreateCustomer() throws Exception {
        CustomerCreateDTO request = new CustomerCreateDTO();
        request.setName("Takudzwa Jengwa");

        when(customerService.createCustomer(request)).thenReturn(customerDTO);

        mockMvc.perform(post("/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Takudzwa Jengwa"));
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
        // page=1 -> Pageable offset 0
        PageResponse<CustomerDTO> page = PageResponse.of(List.of(customerDTO), zeroIndexedPageOf(0, 20, 1));
        when(customerService.getAllCustomers(1, 20)).thenReturn(page);

        mockMvc.perform(get("/customer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Takudzwa Jengwa"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void testGetAllCustomersWithExplicitPageAndSize() throws Exception {
        // page=2 -> Pageable offset 1
        PageResponse<CustomerDTO> page = PageResponse.of(List.of(customerDTO), zeroIndexedPageOf(1, 10, 25));
        when(customerService.getAllCustomers(2, 10)).thenReturn(page);

        mockMvc.perform(get("/customer").param("page", "2").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(10));
    }

    @Test
    void testGetCustomersWithNameCallsSearch() throws Exception {
        PageResponse<CustomerDTO> page = PageResponse.of(List.of(customerDTO), zeroIndexedPageOf(0, 20, 1));
        when(customerService.searchCustomers("van zyl", 1, 20)).thenReturn(page);

        mockMvc.perform(get("/customer").param("name", "van zyl"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Takudzwa Jengwa"));
    }

    @Test
    void testGetCustomersWithBlankNameFallsBackToListAll() throws Exception {
        PageResponse<CustomerDTO> page = PageResponse.of(List.of(customerDTO), zeroIndexedPageOf(0, 20, 1));
        when(customerService.getAllCustomers(1, 20)).thenReturn(page);

        mockMvc.perform(get("/customer").param("name", "   ")).andExpect(status().isOk());
    }

    @Test
    void testGetCustomersInvalidSearchQueryReturns400() throws Exception {
        when(customerService.searchCustomers("ab", 1, 20))
                .thenThrow(new InvalidSearchQueryException("Search query must be at least 3 characters"));

        mockMvc.perform(get("/customer").param("name", "ab"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
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
