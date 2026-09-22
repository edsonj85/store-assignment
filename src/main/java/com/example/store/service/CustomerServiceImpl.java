package com.example.store.service;

import com.example.store.dto.CustomerCreateDTO;
import com.example.store.dto.CustomerDTO;
import com.example.store.dto.PageResponse;
import com.example.store.entity.Customer;
import com.example.store.exception.InvalidSearchQueryException;
import com.example.store.mapper.CustomerMapper;
import com.example.store.repository.CustomerRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    // shorter patterns can't use the pg_trgm index
    private static final int MIN_TERM_LENGTH = 3;
    private static final int MAX_QUERY_LENGTH = 100;

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CustomerDTO> getAllCustomers(int page, int size) {
        Page<Customer> customerPage = customerRepository.findAll(PageRequest.of(page - 1, size));
        List<CustomerDTO> content = customerMapper.customersToCustomerDTOs(customerPage.getContent());
        return PageResponse.of(content, customerPage);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CustomerDTO> searchCustomers(String name, int page, int size) {
        String trimmed = name.trim();
        if (trimmed.length() > MAX_QUERY_LENGTH) {
            throw new InvalidSearchQueryException("Search query must be at most " + MAX_QUERY_LENGTH + " characters");
        }

        List<String> terms = splitIntoTerms(trimmed);
        boolean hasTermBelowMinLength =
                terms.isEmpty() || terms.stream().anyMatch(term -> term.length() < MIN_TERM_LENGTH);
        if (hasTermBelowMinLength) {
            throw new InvalidSearchQueryException(
                    "Search query must be at least " + MIN_TERM_LENGTH + " characters, and each word"
                            + " within it at least " + MIN_TERM_LENGTH + " characters");
        }

        Page<Customer> customerPage = customerRepository.searchByName(terms, PageRequest.of(page - 1, size));
        List<CustomerDTO> content = customerMapper.customersToCustomerDTOs(customerPage.getContent());
        return PageResponse.of(content, customerPage);
    }

    @Override
    @Transactional
    public CustomerDTO createCustomer(CustomerCreateDTO customerCreateDTO) {
        Customer customer = customerMapper.customerCreateDTOToCustomer(customerCreateDTO);
        return customerMapper.customerToCustomerDTO(customerRepository.save(customer));
    }

    private static List<String> splitIntoTerms(String name) {
        return Arrays.stream(name.trim().split("\\s+"))
                .filter(term -> !term.isEmpty())
                .toList();
    }
}
