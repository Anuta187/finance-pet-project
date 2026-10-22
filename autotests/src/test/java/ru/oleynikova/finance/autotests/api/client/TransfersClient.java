package ru.oleynikova.finance.autotests.api.client;

import io.qameta.allure.Step;
import io.restassured.response.Response;

import ru.oleynikova.finance.autotests.api.model.TransferRequest;
import ru.oleynikova.finance.autotests.api.model.TransferResult;
import ru.oleynikova.finance.autotests.data.Session;

public class TransfersClient extends ApiClient {

    public TransfersClient(Session session) {
        super(session);
    }

    @Step("POST /api/transfers")
    public Response transferRaw(Object body) {
        return req().body(body).post("/api/transfers");
    }

    public Response transferRaw(Object fromId, Object toId, Object amount) {
        return transferRaw(TransferRequest.of(fromId, toId, amount));
    }

    @Step("Перевести {amount} ₽ со счёта {fromId} на счёт {toId}")
    public TransferResult transfer(long fromId, long toId, Object amount) {
        return transferRaw(TransferRequest.of(fromId, toId, amount)).then().statusCode(201)
                .extract().as(TransferResult.class);
    }
}
