package ru.oleynikova.finance.autotests.api.client;

import static io.restassured.RestAssured.given;

import java.time.LocalDate;
import java.util.Map;

import io.qameta.allure.Step;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

import ru.oleynikova.finance.autotests.api.Specs;

/** Health-check, Swagger, сброс базы и время сервера. */
public class ServiceClient {

    @Step("GET /api/health")
    public Response health() {
        return given(Specs.anonymous()).get("/api/health");
    }

    @Step("GET /api/docs (Swagger UI)")
    public Response swaggerUi() {
        return given(Specs.anonymous()).accept(ContentType.HTML).get("/api/docs");
    }

    @Step("POST /api/test/reset — пересоздать тестовые данные")
    public Response reset() {
        return given(Specs.anonymous()).post("/api/test/reset");
    }

    @Step("Узнать «сегодня» по часам сервера")
    public LocalDate today() {
        return LocalDate.parse(given(Specs.anonymous()).get("/api/test/clock").then().statusCode(200)
                .extract().path("today"));
    }

    @Step("Сдвинуть часы сервера на {days} дн.")
    public LocalDate shiftClock(int days) {
        return LocalDate.parse(given(Specs.anonymous()).body(Map.of("shiftDays", days)).post("/api/test/clock")
                .then().statusCode(200).extract().path("today"));
    }

    @Step("Вернуть часы сервера к реальному времени")
    public void resetClock() {
        given(Specs.anonymous()).body(Map.of("reset", true)).post("/api/test/clock").then().statusCode(200);
    }
}
