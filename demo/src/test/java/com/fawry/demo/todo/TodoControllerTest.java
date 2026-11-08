package com.fawry.demo.TodoResponse;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

/** Web layer tests: HTTP mapping, status codes and JSON, without starting a server. */
class TodoControllerTest {

    private final TodoService service = new TodoService();
    private final MockMvcTester mvc = MockMvcTester.of(new TodoController(service));

    @Test
    void listReturnsAllTodos() {
        service.create("Buy milk");

        assertThat(mvc.get().uri("/api/todos"))
                .hasStatusOk()
                .bodyJson().extractingPath("$[0].title").isEqualTo("Buy milk");
    }

    @Test
    void createReturns201WithLocationAndTrimmedTitle() {
        assertThat(mvc.post().uri("/api/todos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\": \"  Write tests  \"}"))
                .hasStatus(HttpStatus.CREATED)
                .hasHeader("Location", "/api/todos/1")
                .bodyJson().extractingPath("$.title").isEqualTo("Write tests");
    }

    @Test
    void createRejectsBlankTitle() {
        assertThat(mvc.post().uri("/api/todos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\": \" \"}"))
                .hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void getReturns404ForUnknownId() {
        assertThat(mvc.get().uri("/api/todos/99")).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void deleteReturns204AndRemovesTodo() {
        TodoResponse todo = service.create("temp");

        assertThat(mvc.delete().uri("/api/todos/{id}", todo.id())).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(mvc.get().uri("/api/todos/{id}", todo.id())).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(mvc.delete().uri("/api/todos/{id}", todo.id())).hasStatus(HttpStatus.NOT_FOUND);
    }
}
