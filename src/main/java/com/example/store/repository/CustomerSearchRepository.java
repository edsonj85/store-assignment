package com.example.store.repository;

import com.example.store.entity.Customer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CustomerSearchRepository {

    Page<Customer> searchByName(List<String> terms, Pageable pageable);
}
