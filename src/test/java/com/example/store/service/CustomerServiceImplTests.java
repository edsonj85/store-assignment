package com.example.store.service;

import com.example.store.dto.CustomerCreateDTO;
import com.example.store.dto.CustomerDTO;
import com.example.store.dto.PageResponse;
import com.example.store.entity.Customer;
import com.example.store.exception.InvalidSearchQueryException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
        customer.setName("Takudzwa Jengwa");

        CustomerDTO customerDTO = new CustomerDTO();
        customerDTO.setId(1L);
        customerDTO.setName("Takudzwa Jengwa");

        // page=1 -> Pageable offset 0
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
    void searchCustomersReturnsPagedEnvelope() {
        Customer customer = new Customer();
        customer.setId(1L);
        customer.setName("Johan van Zyl");

        CustomerDTO customerDTO = new CustomerDTO();
        customerDTO.setId(1L);
        customerDTO.setName("Johan van Zyl");

        Pageable pageable = PageRequest.of(0, 20);
        PageImpl<Customer> customerPage = new PageImpl<>(List.of(customer), pageable, 1);

        when(customerRepository.searchByName(List.of("van", "zyl"), pageable)).thenReturn(customerPage);
        when(customerMapper.customersToCustomerDTOs(List.of(customer))).thenReturn(List.of(customerDTO));

        PageResponse<CustomerDTO> result = customerService.searchCustomers("van zyl", 1, 20);

        assertThat(result.getContent()).containsExactly(customerDTO);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void searchCustomersTrimsAndCollapsesWhitespaceBetweenTerms() {
        Pageable pageable = PageRequest.of(0, 20);
        PageImpl<Customer> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(customerRepository.searchByName(List.of("van", "zyl"), pageable)).thenReturn(emptyPage);
        when(customerMapper.customersToCustomerDTOs(List.of())).thenReturn(List.of());

        customerService.searchCustomers("  van   zyl  ", 1, 20);
    }

    @Test
    void searchCustomersThrowsWhenAWordIsShorterThanMinLength() {
        assertThatThrownBy(() -> customerService.searchCustomers("va zyl", 1, 20))
                .isInstanceOf(InvalidSearchQueryException.class);
    }

    @Test
    void searchCustomersThrowsWhenBlank() {
        assertThatThrownBy(() -> customerService.searchCustomers("   ", 1, 20))
                .isInstanceOf(InvalidSearchQueryException.class);
    }

    @Test
    void searchCustomersThrowsWhenQueryExceedsMaxLength() {
        String tooLong = "a".repeat(101);

        assertThatThrownBy(() -> customerService.searchCustomers(tooLong, 1, 20))
                .isInstanceOf(InvalidSearchQueryException.class);
    }

    @Test
    void createCustomerMapsSavesAndReturnsDTO() {
        CustomerCreateDTO request = new CustomerCreateDTO();
        request.setName("Rutendo Moyo");

        Customer mapped = new Customer();
        mapped.setName("Rutendo Moyo");

        Customer saved = new Customer();
        saved.setId(2L);
        saved.setName("Rutendo Moyo");

        CustomerDTO customerDTO = new CustomerDTO();
        customerDTO.setId(2L);
        customerDTO.setName("Rutendo Moyo");

        when(customerMapper.customerCreateDTOToCustomer(request)).thenReturn(mapped);
        when(customerRepository.save(mapped)).thenReturn(saved);
        when(customerMapper.customerToCustomerDTO(saved)).thenReturn(customerDTO);

        CustomerDTO result = customerService.createCustomer(request);

        assertThat(result).isEqualTo(customerDTO);
    }
}
