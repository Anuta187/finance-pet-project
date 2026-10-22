package ru.oleynikova.finance.autotests.api.client;

import static io.restassured.RestAssured.given;

import io.restassured.specification.RequestSpecification;

import ru.oleynikova.finance.autotests.api.Specs;
import ru.oleynikova.finance.autotests.data.Session;

public abstract class ApiClient {

    private final String token;

    protected ApiClient(Session session) {
        this(session == null ? null : session.token());
    }

    protected ApiClient(String token) {
        this.token = token;
    }

    protected RequestSpecification req() {
        return given(Specs.auth(token));
    }
}
