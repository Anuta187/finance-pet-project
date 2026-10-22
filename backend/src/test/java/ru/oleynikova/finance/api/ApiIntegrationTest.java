package ru.oleynikova.finance.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.yaml.snakeyaml.Yaml;

import com.fasterxml.jackson.databind.JsonNode;

@DisplayName("API: счета и переводы")
class ApiIntegrationTest extends IntegrationTestBase {

    @Test
    @DisplayName("Flyway создал схему, при старте демо-клиенту добавлены 3 счёта с операциями")
    void seedAccounts() throws Exception {
        mvc.perform(as(demo, get("/api/accounts")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].name").value("Основной"))
                .andExpect(jsonPath("$[0].balance").value(37540.5))
                .andExpect(jsonPath("$[1].balance").value(155000));
    }

    @Test
    @DisplayName("Перевод проводится в БД: остатки, журнал и дневной лимит обновляются")
    void transferIsPersisted() throws Exception {
        mvc.perform(jsonAs(demo, post("/api/transfers"),
                        "{\"fromAccountId\":1,\"toAccountId\":2,\"amount\":\"1500,50\",\"comment\":\"Тест\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.operation.type").value("TRANSFER"))
                .andExpect(jsonPath("$.operation.amount").value(1500.5))
                .andExpect(jsonPath("$.fromAccount.balance").value(36040));

        mvc.perform(as(demo, get("/api/accounts/1")))
                .andExpect(jsonPath("$.dailyTransferred").value(6500.5))     // 5 000 из демо-данных + 1 500,50
                .andExpect(jsonPath("$.dailyLimitRemaining").value(93499.5));
        mvc.perform(as(demo, get("/api/operations")).param("accountId", "2").param("limit", "1"))
                .andExpect(jsonPath("$[0].comment").value("Тест"))
                .andExpect(jsonPath("$[0].fromAccountName").value("Основной"));
    }

    @Test
    @DisplayName("Отказ в переводе откатывает транзакцию: остаток и журнал без изменений")
    void failedTransferRollsBack() throws Exception {
        int before = mapper.readTree(mvc.perform(as(demo, get("/api/operations"))).andReturn()
                .getResponse().getContentAsString()).size();
        mvc.perform(jsonAs(demo, post("/api/transfers"), "{\"fromAccountId\":3,\"toAccountId\":1,\"amount\":5000.01}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("INSUFFICIENT_FUNDS"))
                .andExpect(jsonPath("$.error.details.balance").value(5000));
        mvc.perform(as(demo, get("/api/accounts/3"))).andExpect(jsonPath("$.balance").value(5000));
        mvc.perform(as(demo, get("/api/operations"))).andExpect(jsonPath("$.length()").value(before));
    }

    @Test
    @DisplayName("Единый формат ошибок: INVALID_JSON, VALIDATION_ERROR, NOT_FOUND")
    void errorFormat() throws Exception {
        mvc.perform(jsonAs(demo, post("/api/accounts"), "{\"name\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_JSON"));
        mvc.perform(jsonAs(demo, post("/api/accounts"), "{\"name\":\"" + "a".repeat(51) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details.field").value("name"));
        mvc.perform(as(demo, get("/api/unknown")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("Health-check и Swagger доступны без токена")
    void publicEndpoints() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
        mvc.perform(get("/api/openapi.yaml")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Контракт openapi.yaml описывает все эндпоинты, реализованные в коде")
    @SuppressWarnings("unchecked")
    void openApiContractInSync() throws Exception {
        String generated = mvc.perform(get("/api/openapi-generated")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode generatedPaths = mapper.readTree(generated).get("paths");

        Map<String, Object> contract;
        try (InputStream in = getClass().getResourceAsStream("/static/api/openapi.yaml")) {
            contract = new Yaml().load(in);
        }
        Map<String, Map<String, Object>> contractPaths = (Map<String, Map<String, Object>>) contract.get("paths");

        assertThat(generatedPaths.size()).as("в коде есть эндпоинты").isGreaterThan(15);
        generatedPaths.fieldNames().forEachRemaining(path -> {
            assertThat(contractPaths).as("путь %s есть в openapi.yaml", path).containsKey(path);
            Set<String> contractMethods = contractPaths.get(path).keySet();
            generatedPaths.get(path).fieldNames().forEachRemaining(method ->
                    assertThat(contractMethods).as("%s %s есть в openapi.yaml", method, path).contains(method));
        });
    }
}
