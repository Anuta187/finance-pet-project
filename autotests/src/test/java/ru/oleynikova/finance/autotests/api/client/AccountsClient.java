package ru.oleynikova.finance.autotests.api.client;

import java.util.List;

import io.qameta.allure.Step;
import io.restassured.common.mapper.TypeRef;
import io.restassured.response.Response;

import ru.oleynikova.finance.autotests.api.model.Account;
import ru.oleynikova.finance.autotests.api.model.AccountDetails;
import ru.oleynikova.finance.autotests.api.model.CreateAccountRequest;
import ru.oleynikova.finance.autotests.data.Session;

// Методы *Raw возвращают ответ как есть, для негативных проверок
public class AccountsClient extends ApiClient {

    public AccountsClient(Session session) {
        super(session);
    }

    public AccountsClient(String rawToken) {
        super(rawToken);
    }

    @Step("GET /api/accounts")
    public Response listRaw() {
        return req().get("/api/accounts");
    }

    public List<Account> list() {
        return listRaw().then().statusCode(200).extract().as(new TypeRef<>() {
        });
    }

    @Step("POST /api/accounts")
    public Response createRaw(Object body) {
        return req().body(body).post("/api/accounts");
    }

    @Step("POST /api/accounts с телом-строкой")
    public Response createRawString(String body) {
        return req().body(body).post("/api/accounts");
    }

    public Account create(CreateAccountRequest request) {
        return createRaw(request).then().statusCode(201).extract().as(Account.class);
    }

    @Step("GET /api/accounts/{id}")
    public Response getRaw(Object id) {
        return req().get("/api/accounts/{id}", id);
    }

    public AccountDetails get(long id) {
        return getRaw(id).then().statusCode(200).extract().as(AccountDetails.class);
    }
}
