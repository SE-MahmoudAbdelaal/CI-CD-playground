package com.fawry.demo.TodoResponse;

import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/** Keeps todos in memory, so they are lost when the app restarts. */
@Service
public class TodoService {

    private final Map<Long, TodoResponse> todos = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);

    public List<TodoResponse> findAll() {
        return todos.values().stream()
                .sorted(Comparator.comparingLong(TodoResponse::id))
                .toList();
    }

    public Optional<TodoResponse> findById(long id) {
        return Optional.ofNullable(todos.get(id));
    }

    public TodoResponse create(String title) {
        TodoResponse todo = new TodoResponse(nextId.getAndIncrement(), title);
        todos.put(todo.id(), todo);
        return todo;
    }

    public boolean delete(long id) {
        return todos.remove(id) != null;
    }
}
