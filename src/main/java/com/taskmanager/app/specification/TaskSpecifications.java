package com.taskmanager.app.specification;

import com.taskmanager.app.model.Task;
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
}