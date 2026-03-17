package com.taskmanager.app.specification;

import com.taskmanager.app.model.Task;
import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Pure unit tests for TaskSpecifications.
 *
 * Each specification is a lambda (root, query, criteriaBuilder) → Predicate.
 * We mock the JPA criteria API objects and verify the correct predicates are built.
 */
@SuppressWarnings("unchecked") // safe: JPA criteria API generics require raw mocks
class TaskSpecificationsTest {

    private final Root<Task> root = mock(Root.class);
    private final CriteriaQuery<?> query = mock(CriteriaQuery.class);
    private final CriteriaBuilder cb = mock(CriteriaBuilder.class);

    // =========================================================================
    // hasTitle(String title)
    // =========================================================================
    @Nested
    @DisplayName("hasTitle()")
    class HasTitle {

        @Test
        @DisplayName("returns null predicate when title is null — no filtering")
        void returnsNullWhenTitleIsNull() {
            Specification<Task> spec = TaskSpecifications.hasTitle(null);

            var predicate = spec.toPredicate(root, query, cb);

            assertThat(predicate).isNull();
            // CriteriaBuilder should never be called when title is null
            verifyNoInteractions(cb);
        }

        @Test
        @DisplayName("builds LIKE predicate with lowercase title when title is provided")
        void buildsLikePredicateWhenTitleProvided() {
            Path<String> headerPath = mock(Path.class);
            Expression<String> lowerExpr = mock(Expression.class);
            Predicate expectedPredicate = mock(Predicate.class);

            when(root.<String>get("header")).thenReturn(headerPath);
            when(cb.lower(headerPath)).thenReturn(lowerExpr);
            when(cb.like(lowerExpr, "%groceries%")).thenReturn(expectedPredicate);

            Specification<Task> spec = TaskSpecifications.hasTitle("Groceries");

            var predicate = spec.toPredicate(root, query, cb);

            assertThat(predicate).isEqualTo(expectedPredicate);
            verify(cb).lower(headerPath);
            verify(cb).like(lowerExpr, "%groceries%");
        }

        @Test
        @DisplayName("lowercases the search term for case-insensitive matching")
        void lowercasesSearchTerm() {
            Path<String> headerPath = mock(Path.class);
            Expression<String> lowerExpr = mock(Expression.class);
            Predicate expectedPredicate = mock(Predicate.class);

            when(root.<String>get("header")).thenReturn(headerPath);
            when(cb.lower(headerPath)).thenReturn(lowerExpr);
            when(cb.like(lowerExpr, "%buy milk%")).thenReturn(expectedPredicate);

            Specification<Task> spec = TaskSpecifications.hasTitle("BUY MILK");
            spec.toPredicate(root, query, cb);

            // Verify the pattern is lowercased
            verify(cb).like(lowerExpr, "%buy milk%");
        }
    }

    // =========================================================================
    // isCompleted(Boolean completed)
    // =========================================================================
    @Nested
    @DisplayName("isCompleted()")
    class IsCompleted {

        @Test
        @DisplayName("returns null predicate when completed is null — no filtering")
        void returnsNullWhenCompletedIsNull() {
            Specification<Task> spec = TaskSpecifications.isCompleted(null);

            var predicate = spec.toPredicate(root, query, cb);

            assertThat(predicate).isNull();
            verifyNoInteractions(cb);
        }

        @Test
        @DisplayName("builds equal predicate for completed=true")
        void buildsEqualPredicateForTrue() {
            Path<Boolean> completedPath = mock(Path.class);
            Predicate expectedPredicate = mock(Predicate.class);

            when(root.<Boolean>get("completed")).thenReturn(completedPath);
            when(cb.equal(completedPath, true)).thenReturn(expectedPredicate);

            Specification<Task> spec = TaskSpecifications.isCompleted(true);

            var predicate = spec.toPredicate(root, query, cb);

            assertThat(predicate).isEqualTo(expectedPredicate);
            verify(cb).equal(completedPath, true);
        }

        @Test
        @DisplayName("builds equal predicate for completed=false")
        void buildsEqualPredicateForFalse() {
            Path<Boolean> completedPath = mock(Path.class);
            Predicate expectedPredicate = mock(Predicate.class);

            when(root.<Boolean>get("completed")).thenReturn(completedPath);
            when(cb.equal(completedPath, false)).thenReturn(expectedPredicate);

            Specification<Task> spec = TaskSpecifications.isCompleted(false);

            var predicate = spec.toPredicate(root, query, cb);

            assertThat(predicate).isEqualTo(expectedPredicate);
            verify(cb).equal(completedPath, false);
        }
    }

    // =========================================================================
    // hasFuzzyTitle(String)
    // =========================================================================
    @Nested
    @DisplayName("hasFuzzyTitle()")
    class HasFuzzyTitle {

        @Test
        @DisplayName("returns null when search term is blank or null")
        void returnsNullWhenBlank() {
            Specification<Task> spec1 = TaskSpecifications.hasFuzzyTitle(null);
            Specification<Task> spec2 = TaskSpecifications.hasFuzzyTitle("   ");

            assertThat(spec1.toPredicate(root, query, cb)).isNull();
            assertThat(spec2.toPredicate(root, query, cb)).isNull();
        }

        @Test
        @DisplayName("builds combined like + similarity predicates when term provided")
        void buildsCombinedPredicates() {
            Path<String> headerPath = mock(Path.class);
            Expression<String> lowerExpr = mock(Expression.class);
            Predicate likePred = mock(Predicate.class);
            Predicate fuzzyPred = mock(Predicate.class);
            Expression<Double> simExpr = mock(Expression.class);

            when(root.<String>get("header")).thenReturn(headerPath);
            when(cb.lower(headerPath)).thenReturn(lowerExpr);
            when(cb.like(lowerExpr, "%grocery%")).thenReturn(likePred);
            when(cb.literal("grocery")).thenReturn(mock(Expression.class));
            when(cb.function(eq("similarity"), eq(Double.class), any(), any())).thenReturn(simExpr);
            when(cb.greaterThan(simExpr, 0.25)).thenReturn(fuzzyPred);
            when(cb.or(likePred, fuzzyPred)).thenReturn(mock(Predicate.class));

            Specification<Task> spec = TaskSpecifications.hasFuzzyTitle("grocery");
            Predicate p = spec.toPredicate(root, query, cb);

            assertThat(p).isNotNull();
            verify(cb).like(lowerExpr, "%grocery%");
            verify(cb).function(eq("similarity"), eq(Double.class), any(), any());
            verify(cb).greaterThan(simExpr, 0.25);
        }
    }



}


