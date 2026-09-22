package com.example.store.repository;

import com.example.store.entity.Order;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // customer is @ManyToOne (single row, no multiplication); products is the
    // only to-many association fetched here, so there's no cartesian product
    // from combining two collections - safe to join-fetch both in one query.
    @EntityGraph(attributePaths = {"customer", "products"})
    Optional<Order> findById(Long id);
}
