package com.example.store.service;

import com.example.store.dto.CustomerCreateDTO;
import com.example.store.dto.CustomerDTO;
import com.example.store.dto.PageResponse;
import com.example.store.entity.Customer;
import com.example.store.mapper.CustomerMapper;
import com.example.store.repository.CustomerRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceImplTests {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CustomerMapper customerMapper;

    private CustomerServiceImpl customerService;

    @BeforeEach
    void setUp() {
        customerService = new CustomerServiceImpl(customerRepository, customerMapper);
    }

    @Test
    void getAllCustomersReturnsPagedEnvelope() {
        Customer customer = new Customer();
        customer.setId(1L);
        customer.setName("John Doe");

        CustomerDTO customerDTO = new CustomerDTO();
        customerDTO.setId(1L);
        customerDTO.setName("John Doe");

        // page=1 (1-indexed, first page) translates to Pageable offset 0
        Pageable pageable = PageRequest.of(0, 20);
        PageImpl<Customer> customerPage = new PageImpl<>(List.of(customer), pageable, 42);

        when(customerRepository.findAll(pageable)).thenReturn(customerPage);
        when(customerMapper.customersToCustomerDTOs(List.of(customer))).thenReturn(List.of(customerDTO));

        PageResponse<CustomerDTO> result = customerService.getAllCustomers(1, 20);

        assertThat(result.getContent()).containsExactly(customerDTO);
        assertThat(result.getPage()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(20);
        assertThat(result.getTotalElements()).isEqualTo(42);
        assertThat(result.getTotalPages()).isEqualTo(3);
    }

    @Test
    void createCustomerMapsSavesAndReturnsDTO() {
        CustomerCreateDTO request = new CustomerCreateDTO();
        request.setName("Jane Doe");

        Customer mapped = new Customer();
        mapped.setName("Jane Doe");

        Customer saved = new Customer();
        saved.setId(2L);
        saved.setName("Jane Doe");

        CustomerDTO customerDTO = new CustomerDTO();
        customerDTO.setId(2L);
        customerDTO.setName("Jane Doe");

        when(customerMapper.customerCreateDTOToCustomer(request)).thenReturn(mapped);
        when(customerRepository.save(mapped)).thenReturn(saved);
        when(customerMapper.customerToCustomerDTO(saved)).thenReturn(customerDTO);

        CustomerDTO result = customerService.createCustomer(request);

        assertThat(result).isEqualTo(customerDTO);
    }
}
