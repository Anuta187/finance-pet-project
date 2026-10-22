package ru.oleynikova.finance.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonNode;

import ru.oleynikova.finance.config.MutableClock;

@DisplayName("Карты, платежи, аналитика и автоплатежи")
class ProductsIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MutableClock clock;

    @Test
    @DisplayName("Выпуск карты, блокировка: CARD_BLOCKED при оплате, разблокировка: платёж проходит")
    void cardLifecycle() throws Exception {
        JsonNode card = read(mvc.perform(jsonAs(demo, post("/api/cards"), "{\"accountId\":3}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.holderName").value("IVAN PETROV")));
        long id = card.get("id").asLong();
        assertThat(card.get("maskedNumber").asText()).matches("2200 \\*{4} \\*{4} \\d{4}");

        mvc.perform(as(demo, post("/api/cards/" + id + "/block"))).andExpect(jsonPath("$.status").value("BLOCKED"));
        mvc.perform(jsonAs(demo, post("/api/payments"), "{\"categoryId\":2,\"cardId\":" + id + ",\"amount\":100}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("CARD_BLOCKED"));
        mvc.perform(as(demo, get("/api/accounts/3"))).andExpect(jsonPath("$.balance").value(5000));

        mvc.perform(as(demo, post("/api/cards/" + id + "/unblock"))).andExpect(jsonPath("$.status").value("ACTIVE"));
        mvc.perform(jsonAs(demo, post("/api/payments"), "{\"categoryId\":2,\"cardId\":" + id + ",\"amount\":100}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.operation.type").value("PAYMENT"))
                .andExpect(jsonPath("$.operation.cardId").value(id))
                .andExpect(jsonPath("$.account.balance").value(4900));
    }

    @Test
    @DisplayName("Аналитика: платёж увеличивает сумму своей категории за текущий месяц")
    void spendingByCategory() throws Exception {
        mvc.perform(as(demo, get("/api/stats/spending")))
                .andExpect(jsonPath("$.total").value(7459.5))
                .andExpect(jsonPath("$.categories[0].code").value("UTILITIES"));

        mvc.perform(jsonAs(demo, post("/api/payments"), "{\"categoryId\":7,\"accountId\":2,\"amount\":10000}"))
                .andExpect(status().isCreated());

        mvc.perform(as(demo, get("/api/stats/spending")))
                .andExpect(jsonPath("$.total").value(17459.5))
                .andExpect(jsonPath("$.categories[0].code").value("HEALTH"))
                .andExpect(jsonPath("$.categories[0].amount").value(10000));
        mvc.perform(as(demo, get("/api/stats/spending")).param("month", "2020-01"))
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.categories.length()").value(0));
    }

    @Test
    @DisplayName("Автоплатёж: исполняется в дату старта, повторный запуск в тот же день ничего не делает, через месяц — снова")
    void autopaymentSchedule() throws Exception {
        long id = read(mvc.perform(jsonAs(demo, post("/api/autopayments"),
                        "{\"name\":\"Интернет\",\"categoryId\":2,\"accountId\":3,\"amount\":500,\"period\":\"MONTHLY\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.active").value(true))).get("id").asLong();

        JsonNode run = read(mvc.perform(as(admin, post("/api/admin/autopayments/run"))).andExpect(status().isOk()));
        assertThat(run.get("succeeded").asInt()).isEqualTo(1);
        assertThat(run.get("results").get(0).get("autopaymentId").asLong()).isEqualTo(id);
        mvc.perform(as(demo, get("/api/accounts/3"))).andExpect(jsonPath("$.balance").value(4500));

        assertThat(read(mvc.perform(as(admin, post("/api/admin/autopayments/run")))).get("processed").asInt()).isZero();

        clock.shift(Duration.ofDays(31));
        JsonNode second = read(mvc.perform(as(admin, post("/api/admin/autopayments/run"))));
        assertThat(second.findValues("autopaymentId")).extracting(JsonNode::asLong).contains(id);
        mvc.perform(as(demo, get("/api/accounts/3"))).andExpect(jsonPath("$.balance").value(4000));
        mvc.perform(as(demo, get("/api/operations")).param("accountId", "3").param("limit", "1"))
                .andExpect(jsonPath("$[0].autopaymentId").value(id))
                .andExpect(jsonPath("$[0].comment").value("Автоплатёж: Интернет"));
    }

    @Test
    @DisplayName("Отключённый автоплатёж не исполняется; при нехватке денег — статус FAILED и код ошибки")
    void autopaymentDisabledAndFailed() throws Exception {
        long disabled = read(mvc.perform(jsonAs(demo, post("/api/autopayments"),
                "{\"name\":\"Отключу\",\"categoryId\":1,\"accountId\":3,\"amount\":100,\"period\":\"DAILY\"}"))).get("id").asLong();
        mvc.perform(as(demo, post("/api/autopayments/" + disabled + "/disable")))
                .andExpect(jsonPath("$.active").value(false));
        long failing = read(mvc.perform(jsonAs(demo, post("/api/autopayments"),
                "{\"name\":\"Дорого\",\"categoryId\":3,\"accountId\":3,\"amount\":999999,\"period\":\"WEEKLY\"}"))).get("id").asLong();

        JsonNode run = read(mvc.perform(as(admin, post("/api/admin/autopayments/run"))));

        assertThat(run.findValues("autopaymentId")).extracting(JsonNode::asLong).containsExactly(failing);
        assertThat(run.get("results").get(0).get("errorCode").asText()).isEqualTo("INSUFFICIENT_FUNDS");
        mvc.perform(as(demo, get("/api/autopayments")))
                .andExpect(jsonPath("$[0].lastStatus").value("FAILED"))
                .andExpect(jsonPath("$[0].nextRunDate").exists());
        mvc.perform(as(demo, get("/api/accounts/3"))).andExpect(jsonPath("$.balance").value(5000));
    }

    @Test
    @DisplayName("Валидация автоплатежа: дата в прошлом, неверный период, чужой автоплатёж: 404")
    void autopaymentValidation() throws Exception {
        mvc.perform(jsonAs(demo, post("/api/autopayments"),
                        "{\"name\":\"X\",\"categoryId\":1,\"accountId\":1,\"amount\":1,\"period\":\"MONTHLY\",\"startDate\":\"2020-01-01\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details.field").value("startDate"));
        mvc.perform(jsonAs(demo, post("/api/autopayments"),
                        "{\"name\":\"X\",\"categoryId\":1,\"accountId\":1,\"amount\":1,\"period\":\"YEARLY\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details.field").value("period"));
        String other = register("other@test.ru");
        mvc.perform(as(other, post("/api/autopayments/1/disable")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("AUTOPAYMENT_NOT_FOUND"));
    }

    @Test
    @DisplayName("Админ видит операции всех пользователей с email владельца")
    void adminOperations() throws Exception {
        String other = register("other@test.ru");
        mvc.perform(jsonAs(other, post("/api/accounts"), "{\"name\":\"Мой\",\"initialBalance\":100}"))
                .andExpect(status().isCreated());

        JsonNode ops = read(mvc.perform(as(admin, get("/api/admin/operations"))).andExpect(status().isOk()));

        assertThat(ops.findValuesAsText("ownerEmail")).contains("other@test.ru", "demo@finance.local");
        assertThat(ops.get(0).get("ownerEmail").asText()).isEqualTo("other@test.ru");
    }

    private JsonNode read(org.springframework.test.web.servlet.ResultActions result) throws Exception {
        return mapper.readTree(result.andReturn().getResponse().getContentAsString());
    }
}
