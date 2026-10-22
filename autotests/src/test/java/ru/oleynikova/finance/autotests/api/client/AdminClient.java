package ru.oleynikova.finance.autotests.api.client;

import java.util.List;

import io.qameta.allure.Step;
import io.restassured.common.mapper.TypeRef;
import io.restassured.response.Response;

import ru.oleynikova.finance.autotests.api.model.AdminUser;
import ru.oleynikova.finance.autotests.api.model.AutopaymentRunResult;
import ru.oleynikova.finance.autotests.api.model.Operation;
import ru.oleynikova.finance.autotests.data.Session;

public class AdminClient extends ApiClient {

    public AdminClient(Session session) {
        super(session);
    }

    @Step("GET /api/admin/users")
    public Response usersRaw() {
        return req().get("/api/admin/users");
    }

    public List<AdminUser> users() {
        return usersRaw().then().statusCode(200).extract().as(new TypeRef<>() {
        });
    }

    public AdminUser user(long id) {
        return users().stream().filter(u -> u.id() == id).findFirst()
                .orElseThrow(() -> new AssertionError("Пользователь " + id + " не найден"));
    }

    @Step("POST /api/admin/users/{id}/block")
    public Response blockRaw(Object id) {
        return req().post("/api/admin/users/{id}/block", id);
    }

    public AdminUser block(long id) {
        return blockRaw(id).then().statusCode(200).extract().as(AdminUser.class);
    }

    @Step("POST /api/admin/users/{id}/unblock")
    public Response unblockRaw(Object id) {
        return req().post("/api/admin/users/{id}/unblock", id);
    }

    public AdminUser unblock(long id) {
        return unblockRaw(id).then().statusCode(200).extract().as(AdminUser.class);
    }

    @Step("GET /api/admin/operations")
    public Response operationsRaw() {
        return req().queryParam("limit", 500).get("/api/admin/operations");
    }

    public List<Operation> operations() {
        return operationsRaw().then().statusCode(200).extract().as(new TypeRef<>() {
        });
    }

    @Step("POST /api/admin/autopayments/run — исполнить автоплатежи на сегодня")
    public Response runAutopaymentsRaw() {
        return req().post("/api/admin/autopayments/run");
    }

    public AutopaymentRunResult runAutopayments() {
        return runAutopaymentsRaw().then().statusCode(200).extract().as(AutopaymentRunResult.class);
    }
}
