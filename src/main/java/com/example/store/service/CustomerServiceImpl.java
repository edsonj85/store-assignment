package com.example.store.service;

import com.example.store.dto.CustomerCreateDTO;
import com.example.store.dto.CustomerDTO;
import com.example.store.dto.PageResponse;
import com.example.store.entity.Customer;
import com.example.store.mapper.CustomerMapper;
import com.example.store.repository.CustomerRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

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
    @Transactional
    public CustomerDTO createCustomer(CustomerCreateDTO customerCreateDTO) {
        Customer customer = customerMapper.customerCreateDTOToCustomer(customerCreateDTO);
        return customerMapper.customerToCustomerDTO(customerRepository.save(customer));
    }
}
