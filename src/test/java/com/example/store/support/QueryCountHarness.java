package com.example.store.support;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;

/**
 * Counts the JDBC statements Hibernate prepares while running a block of code, so N+1 regressions
 * can be asserted on directly instead of eyeballed from SQL logs. Enables statistics collection at
 * runtime on the underlying {@link Statistics}, so the count is accurate regardless of whether
 * {@code hibernate.generate_statistics} is set for the active profile.
 */
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
