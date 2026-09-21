package com.example.store.service;

import com.example.store.dto.CustomerCreateDTO;
import com.example.store.dto.CustomerDTO;
import com.example.store.dto.PageResponse;

public interface CustomerService {

    PageResponse<CustomerDTO> getAllCustomers(int page, int size);

    CustomerDTO createCustomer(CustomerCreateDTO customerCreateDTO);
}
