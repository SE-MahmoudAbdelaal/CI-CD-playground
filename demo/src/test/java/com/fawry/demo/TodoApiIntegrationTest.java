package com.fawry.demo;

import com.fawry.demo.todo.TodoResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Integration tests: starts the whole app on a random port and calls it over real HTTP. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "app.version=1.2.3-test")
class TodoApiIntegrationTest {

    @LocalServerPort
    private int port;

    private RestTestClient client;

    @BeforeEach
    void setUp() {
        client = RestTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    @Test
    void healthEndpointIsUp() {
        client.get().uri("/actuator/health").exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.status").isEqualTo("UP");
    }

    @Test
    void apiHealthReportsUpAndTheConfiguredVersion() {
        client.get().uri("/api/health").exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP")
                .jsonPath("$.version").isEqualTo("1.2.3-test")
                .jsonPath("$.startedAt").isNotEmpty();
    }

    @Test
    void createReadAndDeleteTodo() {
        TodoResponse created = client.post().uri("/api/todos")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("title", "Ship it"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(TodoResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(created).isNotNull();
        assertThat(created.title()).isEqualTo("Ship it");

        client.get().uri("/api/todos/{id}", created.id()).exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.title").isEqualTo("Ship it");

        client.delete().uri("/api/todos/{id}", created.id()).exchange()
                .expectStatus().isNoContent();

        client.get().uri("/api/todos/{id}", created.id()).exchange()
                .expectStatus().isNotFound();
    }
}
