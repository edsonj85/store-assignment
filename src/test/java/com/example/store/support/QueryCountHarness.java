package com.example.store.support;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;

// Counts JDBC statements Hibernate issues while running a block of code.
public class QueryCountHarness {

    private final Statistics statistics;

    public QueryCountHarness(EntityManagerFactory entityManagerFactory) {
        this.statistics =
                entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        this.statistics.setStatisticsEnabled(true);
    }

    public long countStatements(Action action) throws Exception {
        statistics.clear();
        action.run();
        return statistics.getPrepareStatementCount();
    }

    @FunctionalInterface
    public interface Action {
        void run() throws Exception;
    }
}
