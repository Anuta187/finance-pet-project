package ru.oleynikova.finance.autotests.api.client;

import java.util.List;
import java.util.Map;

import io.qameta.allure.Step;
import io.restassured.common.mapper.TypeRef;
import io.restassured.response.Response;

import ru.oleynikova.finance.autotests.api.model.Operation;
import ru.oleynikova.finance.autotests.data.Session;

public class OperationsClient extends ApiClient {

    public OperationsClient(Session session) {
        super(session);
    }

    @Step("GET /api/operations {query}")
    public Response listRaw(Map<String, ?> query) {
        return req().queryParams(query).get("/api/operations");
    }

    public List<Operation> list(Map<String, ?> query) {
        return listRaw(query).then().statusCode(200).extract().as(new TypeRef<>() {
        });
    }

    public List<Operation> byAccount(long accountId) {
        return list(Map.of("accountId", accountId));
    }
}
