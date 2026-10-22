package ru.oleynikova.finance.autotests.api.client;

import java.util.List;

import io.qameta.allure.Step;
import io.restassured.common.mapper.TypeRef;
import io.restassured.response.Response;

import ru.oleynikova.finance.autotests.api.model.Category;
import ru.oleynikova.finance.autotests.api.model.PaymentRequest;
import ru.oleynikova.finance.autotests.api.model.PaymentResult;
import ru.oleynikova.finance.autotests.api.model.Spending;
import ru.oleynikova.finance.autotests.data.Session;

public class PaymentsClient extends ApiClient {

    public PaymentsClient(Session session) {
        super(session);
    }

    @Step("GET /api/categories")
    public List<Category> categories() {
        return req().get("/api/categories").then().statusCode(200).extract().as(new TypeRef<>() {
        });
    }

    public Category category(String code) {
        return categories().stream().filter(c -> c.code().equals(code)).findFirst()
                .orElseThrow(() -> new IllegalStateException("Нет категории " + code));
    }

    @Step("POST /api/payments")
    public Response payRaw(Object body) {
        return req().body(body).post("/api/payments");
    }

    public PaymentResult pay(PaymentRequest request) {
        return payRaw(request).then().statusCode(201).extract().as(PaymentResult.class);
    }

    @Step("GET /api/stats/spending")
    public Spending spending() {
        return req().get("/api/stats/spending").then().statusCode(200).extract().as(Spending.class);
    }
}
