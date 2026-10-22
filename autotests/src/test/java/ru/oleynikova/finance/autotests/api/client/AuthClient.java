package ru.oleynikova.finance.autotests.api.client;

import java.util.Map;

import io.qameta.allure.Step;
import io.restassured.response.Response;

import ru.oleynikova.finance.autotests.api.model.AuthResponse;
import ru.oleynikova.finance.autotests.api.model.User;
import ru.oleynikova.finance.autotests.data.Session;

public class AuthClient extends ApiClient {

    public AuthClient() {
        super((String) null);
    }

    public AuthClient(Session session) {
        super(session);
    }

    public AuthClient(String rawToken) {
        super(rawToken);
    }

    @Step("POST /api/auth/register")
    public Response registerRaw(Object body) {
        return req().body(body).post("/api/auth/register");
    }

    public AuthResponse register(String email, String password, String fullName) {
        return registerRaw(Map.of("email", email, "password", password, "fullName", fullName))
                .then().statusCode(201).extract().as(AuthResponse.class);
    }

    @Step("POST /api/auth/login ({email})")
    public Response loginRaw(String email, String password) {
        return req().body(Map.of("email", email, "password", password)).post("/api/auth/login");
    }

    public Response loginRaw(Object body) {
        return req().body(body).post("/api/auth/login");
    }

    public AuthResponse login(String email, String password) {
        return loginRaw(email, password).then().statusCode(200).extract().as(AuthResponse.class);
    }

    @Step("GET /api/auth/me")
    public Response meRaw() {
        return req().get("/api/auth/me");
    }

    public User me() {
        return meRaw().then().statusCode(200).extract().as(User.class);
    }
}
