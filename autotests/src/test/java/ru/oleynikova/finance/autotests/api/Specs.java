package ru.oleynikova.finance.autotests.api;

import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.path.json.mapper.factory.Jackson2ObjectMapperFactory;
import io.restassured.specification.RequestSpecification;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import ru.oleynikova.finance.autotests.config.TestConfig;

public final class Specs {

    private static final Jackson2ObjectMapperFactory MAPPER = (type, charset) -> new ObjectMapper()
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private Specs() {
    }

    private static RequestSpecBuilder base() {
        return new RequestSpecBuilder()
                .setBaseUri(TestConfig.apiUrl())
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .setConfig(RestAssuredConfig.config()
                        .objectMapperConfig(ObjectMapperConfig.objectMapperConfig().jackson2ObjectMapperFactory(MAPPER)))
                .addFilter(new AllureRestAssured())
                .log(LogDetail.URI);
    }

    public static RequestSpecification anonymous() {
        return base().build();
    }

    // если token == null, запрос уходит без Authorization
    public static RequestSpecification auth(String token) {
        RequestSpecBuilder builder = base();
        if (token != null) {
            builder.addHeader("Authorization", "Bearer " + token);
        }
        return builder.build();
    }
}
