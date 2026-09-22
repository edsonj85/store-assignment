package com.example.store.repository;

import com.example.store.entity.Product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // Reads the join table directly rather than navigating Product.orders, which
    // would materialise full Order entities just to read their id.
    @Query(
            """
            select p.id as productId, o.id as orderId
            from Order o join o.products p
            where p.id in :productIds
            """)
    List<ProductOrderIdProjection> findOrderIdsByProductIds(@Param("productIds") Collection<Long> productIds);
}
