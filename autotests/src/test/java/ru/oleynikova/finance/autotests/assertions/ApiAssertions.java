package ru.oleynikova.finance.autotests.assertions;

import static org.assertj.core.api.Assertions.assertThat;

import io.qameta.allure.Step;
import io.restassured.response.Response;

import ru.oleynikova.finance.autotests.api.model.ErrorResponse;

public final class ApiAssertions {

    private ApiAssertions() {
    }

    /** Проверяет статус и код ошибки, возвращает тело ошибки. */
    @Step("Проверить ошибку: HTTP {status}, code = {code}")
    public static ErrorResponse.ApiError assertError(Response response, int status, String code) {
        assertThat(response.statusCode()).as("HTTP-статус, тело: %s", response.asString()).isEqualTo(status);
        ErrorResponse body = response.as(ErrorResponse.class);
        assertThat(body.error()).as("объект error").isNotNull();
        assertThat(body.error().code()).as("error.code").isEqualTo(code);
        assertThat(body.error().message()).as("error.message").isNotBlank();
        return body.error();
    }
}
