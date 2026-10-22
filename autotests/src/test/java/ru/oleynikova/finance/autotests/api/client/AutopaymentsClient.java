package ru.oleynikova.finance.autotests.api.client;

import java.util.List;

import io.qameta.allure.Step;
import io.restassured.common.mapper.TypeRef;
import io.restassured.response.Response;

import ru.oleynikova.finance.autotests.api.model.Autopayment;
import ru.oleynikova.finance.autotests.data.Session;

public class AutopaymentsClient extends ApiClient {

    public AutopaymentsClient(Session session) {
        super(session);
    }

    @Step("GET /api/autopayments")
    public List<Autopayment> list() {
        return req().get("/api/autopayments").then().statusCode(200).extract().as(new TypeRef<>() {
        });
    }

    public Autopayment get(long id) {
        return list().stream().filter(a -> a.id() == id).findFirst()
                .orElseThrow(() -> new AssertionError("Автоплатёж " + id + " не найден в списке"));
    }

    @Step("POST /api/autopayments")
    public Response createRaw(Object body) {
        return req().body(body).post("/api/autopayments");
    }

    public Autopayment create(Object body) {
        return createRaw(body).then().statusCode(201).extract().as(Autopayment.class);
    }

    @Step("POST /api/autopayments/{id}/disable")
    public Response disableRaw(Object id) {
        return req().post("/api/autopayments/{id}/disable", id);
    }
}
