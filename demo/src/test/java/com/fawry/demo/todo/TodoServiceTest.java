package com.fawry.demo.todo;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Unit tests: plain Java, no Spring context. */
class TodoServiceTest {

    private final TodoService service = new TodoService();

    @Test
    void createAssignsIncreasingIds() {
        Todo first = service.create("first");
        Todo second = service.create("second");

        assertThat(first).isEqualTo(new Todo(1, "first"));
        assertThat(second.id()).isEqualTo(2);
    }

    @Test
    void findAllReturnsTodosOrderedById() {
        service.create("a");
        service.create("b");

        assertThat(service.findAll()).extracting(Todo::title).containsExactly("a", "b");
    }

    @Test
    void findByIdReturnsEmptyForUnknownId() {
        assertThat(service.findById(42)).isEmpty();
    }

    @Test
    void deleteRemovesTodo() {
        Todo todo = service.create("temp");

        assertThat(service.delete(todo.id())).isTrue();
        assertThat(service.findById(todo.id())).isEmpty();
        assertThat(service.delete(todo.id())).isFalse();
    }
}
