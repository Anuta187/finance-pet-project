package ru.oleynikova.finance.autotests.api.client;

import java.util.List;
import java.util.Map;

import io.qameta.allure.Step;
import io.restassured.common.mapper.TypeRef;
import io.restassured.response.Response;

import ru.oleynikova.finance.autotests.api.model.Card;
import ru.oleynikova.finance.autotests.data.Session;

public class CardsClient extends ApiClient {

    public CardsClient(Session session) {
        super(session);
    }

    @Step("GET /api/cards")
    public List<Card> list() {
        return req().get("/api/cards").then().statusCode(200).extract().as(new TypeRef<>() {
        });
    }

    @Step("POST /api/cards (счёт {accountId})")
    public Response issueRaw(Object accountId) {
        return req().body(accountId == null ? Map.of() : Map.of("accountId", accountId)).post("/api/cards");
    }

    public Card issue(long accountId) {
        return issueRaw(accountId).then().statusCode(201).extract().as(Card.class);
    }

    @Step("POST /api/cards/{id}/block")
    public Response blockRaw(Object id) {
        return req().post("/api/cards/{id}/block", id);
    }

    public Card block(long id) {
        return blockRaw(id).then().statusCode(200).extract().as(Card.class);
    }

    @Step("POST /api/cards/{id}/unblock")
    public Response unblockRaw(Object id) {
        return req().post("/api/cards/{id}/unblock", id);
    }

    public Card unblock(long id) {
        return unblockRaw(id).then().statusCode(200).extract().as(Card.class);
    }
}
