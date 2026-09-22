package com.example.store.repository;

import com.example.store.entity.Customer;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class CustomerSearchRepositoryImpl implements CustomerSearchRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @SuppressWarnings("unchecked")
    public Page<Customer> searchByName(List<String> terms, Pageable pageable) {
        String predicate = buildPredicate(terms);

        Query contentQuery = entityManager
                .createNativeQuery("SELECT * FROM customer WHERE " + predicate + " ORDER BY id", Customer.class)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize());
        Query countQuery = entityManager.createNativeQuery("SELECT COUNT(*) FROM customer WHERE " + predicate);

        bindTerms(contentQuery, terms);
        bindTerms(countQuery, terms);

        List<Customer> content = contentQuery.getResultList();
        long total = ((Number) countQuery.getSingleResult()).longValue();

        return new PageImpl<>(content, pageable, total);
    }

    // one ILIKE clause per term, ANDed; values are bound in bindTerms, never concatenated
    private static String buildPredicate(List<String> terms) {
        return IntStream.range(0, terms.size())
                .mapToObj(i -> "name ILIKE :term" + i + " ESCAPE '\\'")
                .collect(Collectors.joining(" AND "));
    }

    private static void bindTerms(Query query, List<String> terms) {
        for (int i = 0; i < terms.size(); i++) {
            query.setParameter("term" + i, "%" + escapeLikeMetacharacters(terms.get(i)) + "%");
        }
    }

    private static String escapeLikeMetacharacters(String term) {
        return term.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
