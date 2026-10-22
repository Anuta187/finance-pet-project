package ru.oleynikova.finance.autotests.tests.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.restassured.response.Response;

import ru.oleynikova.finance.autotests.tests.BaseTest;

@Tag("api")
@Epic("Личные финансы")
@Feature("API: служебные эндпоинты")
@DisplayName("API: служебные эндпоинты")
class ServiceApiTest extends BaseTest {

    @Test
    @Tag("smoke")
    @DisplayName("Health-check /api/health отвечает UP")
    void health() {
        Response res = service.health();
        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(res.jsonPath().getString("status")).isEqualTo("UP");
    }

    @Test
    @DisplayName("Swagger UI доступен на /api/docs")
    void swaggerUi() {
        Response res = service.swaggerUi();
        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(res.asString()).contains("swagger-ui");
    }
}
