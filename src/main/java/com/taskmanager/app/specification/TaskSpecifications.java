package com.taskmanager.app.specification;

import com.taskmanager.app.model.Task;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

public class TaskSpecifications {

    public static Specification<Task> hasTitle(String title) {
        return (root, query, cb) ->
                title == null ? null : cb.like(cb.lower(root.get("header")), "%" + title.toLowerCase() + "%");
    }

    public static Specification<Task> isCompleted(Boolean completed) {
        return (root, query, cb) ->
                completed == null ? null : cb.equal(root.get("completed"), completed);
    }

    /**
     * Fuzzy search on the task title using PostgreSQL pg_trgm similarity.
     *
     * Combines two predicates with OR so both strategies can match:
     *  1. LIKE '%term%'           — catches clean substrings  ("Groceries" in "Buy Groceries")
     *  2. similarity() > 0.25    — catches typos / word forms ("Grocery" ≈ "Groceries")
     *
     * The threshold 0.25 is intentionally permissive; raise it (e.g. 0.4) if results
     * are too broad, lower it if too few results come back.
     */
    public static Specification<Task> hasFuzzyTitle(String searchTerm) {
        return (root, query, cb) -> {
            if (searchTerm == null || searchTerm.isBlank()) return null;

            String term = searchTerm.toLowerCase().trim();

            // Predicate 1: simple case-insensitive substring match
            Predicate likePredicate = cb.like(
                    cb.lower(root.get("header")), "%" + term + "%");

            // Predicate 2: pg_trgm similarity — calls similarity(lower(title), :term) > 0.25
            Expression<Double> similarityScore = cb.function(
                    "similarity",
                    Double.class,
                    cb.lower(root.get("header")),
                    cb.literal(term));
            Predicate fuzzyPredicate = cb.greaterThan(similarityScore, 0.25);

            return cb.or(likePredicate, fuzzyPredicate);
        };
    }
}