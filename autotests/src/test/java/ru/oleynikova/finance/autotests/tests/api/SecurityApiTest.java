package ru.oleynikova.finance.autotests.tests.api;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.oleynikova.finance.autotests.assertions.ApiAssertions.assertError;
import static ru.oleynikova.finance.autotests.data.AccountBuilder.anAccount;

import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.restassured.response.Response;

import ru.oleynikova.finance.autotests.api.Specs;
import ru.oleynikova.finance.autotests.api.client.AccountsClient;
import ru.oleynikova.finance.autotests.api.client.AdminClient;
import ru.oleynikova.finance.autotests.api.client.AuthClient;
import ru.oleynikova.finance.autotests.api.client.AutopaymentsClient;
import ru.oleynikova.finance.autotests.api.client.CardsClient;
import ru.oleynikova.finance.autotests.api.client.OperationsClient;
import ru.oleynikova.finance.autotests.api.client.PaymentsClient;
import ru.oleynikova.finance.autotests.api.client.TransfersClient;
import ru.oleynikova.finance.autotests.api.model.Account;
import ru.oleynikova.finance.autotests.api.model.Autopayment;
import ru.oleynikova.finance.autotests.api.model.Card;
import ru.oleynikova.finance.autotests.api.model.CreateAutopaymentRequest;
import ru.oleynikova.finance.autotests.api.model.PaymentRequest;
import ru.oleynikova.finance.autotests.data.Session;
import ru.oleynikova.finance.autotests.data.Users;
import ru.oleynikova.finance.autotests.tests.BaseTest;

@Tag("api")
@Tag("security")
@Epic("Личные финансы")
@Feature("API: безопасность и разграничение доступа")
@DisplayName("API: безопасность и разграничение доступа")
class SecurityApiTest extends BaseTest {

    static Stream<Arguments> protectedEndpoints() {
        return Stream.of(
                Arguments.of("GET", "/api/auth/me"),
                Arguments.of("GET", "/api/accounts"),
                Arguments.of("POST", "/api/accounts"),
                Arguments.of("GET", "/api/operations"),
                Arguments.of("POST", "/api/transfers"),
                Arguments.of("GET", "/api/cards"),
                Arguments.of("POST", "/api/payments"),
                Arguments.of("GET", "/api/stats/spending"),
                Arguments.of("GET", "/api/autopayments"),
                Arguments.of("GET", "/api/admin/users"));
    }

    @ParameterizedTest(name = "AT-33 Без токена: {0} {1} — 401 UNAUTHORIZED")
    @MethodSource("protectedEndpoints")
    @Story("US-10 Защита API")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("AT-33 Запрос без токена")
    void noToken(String method, String path) {
        Response res = given(Specs.anonymous()).body("{}").request(method, path);
        assertError(res, 401, "UNAUTHORIZED");
    }

    @Test
    @Story("US-10 Защита API")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("AT-34 Мусорный токен и токен с подменённой подписью: 401 UNAUTHORIZED")
    void invalidTokens() {
        String token = demo.token();
        String[] parts = token.split("\\.");
        char last = parts[2].charAt(0);
        String tampered = parts[0] + "." + parts[1] + "." + (last == 'A' ? 'B' : 'A') + parts[2].substring(1);
        // payload подменён на чужой id при исходной подписи
        String forgedPayload = parts[0] + "." + parts[1].substring(0, parts[1].length() - 2) + "xx." + parts[2];

        for (String bad : new String[] {"garbage", tampered, forgedPayload, token + "x"}) {
            assertError(new AccountsClient(bad).listRaw(), 401, "UNAUTHORIZED");
        }
    }

    static Stream<Arguments> adminEndpoints() {
        return Stream.of(
                Arguments.of("GET", "/api/admin/users"),
                Arguments.of("POST", "/api/admin/users/2/block"),
                Arguments.of("POST", "/api/admin/users/2/unblock"),
                Arguments.of("GET", "/api/admin/operations"),
                Arguments.of("POST", "/api/admin/autopayments/run"));
    }

    @ParameterizedTest(name = "AT-35 Клиент: {0} {1} — 403 FORBIDDEN")
    @MethodSource("adminEndpoints")
    @Story("US-11 Роли")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("AT-35 Клиент не имеет доступа к админ-API")
    void clientCannotUseAdminApi(String method, String path) {
        Response res = given(Specs.auth(demo.token())).request(method, path);
        assertError(res, 403, "FORBIDDEN");
        assertThat(new AdminClient(Users.admin()).user(demo.userId()).blocked()).isFalse();
    }

    @Test
    @Story("US-12 Изоляция данных клиентов")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("AT-36 Новый клиент видит только свои данные: счета, операции, карты, автоплатежи")
    void newClientSeesOnlyOwnData() {
        Session alice = Users.newClient();

        assertThat(new AccountsClient(alice).list()).isEmpty();
        assertThat(new OperationsClient(alice).list(Map.of())).isEmpty();
        assertThat(new CardsClient(alice).list()).isEmpty();
        assertThat(new AutopaymentsClient(alice).list()).isEmpty();

        Account own = anAccount(alice).withBalance(10).create();
        assertThat(new AccountsClient(alice).list()).extracting(Account::id).containsExactly(own.id());
        assertThat(accounts.list()).extracting(Account::id).doesNotContain(own.id());
    }

    @Test
    @Story("US-12 Изоляция данных клиентов")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("AT-37 Действия с чужим счётом дают 404, деньги не двигаются")
    void foreignAccountIsInvisible() {
        Session alice = Users.newClient();
        Account foreign = anAccount(alice).withBalance(1000).create();
        Account mine = anAccountOfDemo().withBalance(1000).create();
        long utilities = new PaymentsClient(demo).category("UTILITIES").id();

        assertError(accounts.getRaw(foreign.id()), 404, "ACCOUNT_NOT_FOUND");
        assertError(operations.listRaw(Map.of("accountId", foreign.id())), 404, "ACCOUNT_NOT_FOUND");
        assertError(transfers.transferRaw(foreign.id(), mine.id(), 100), 404, "ACCOUNT_NOT_FOUND");
        assertError(transfers.transferRaw(mine.id(), foreign.id(), 100), 404, "ACCOUNT_NOT_FOUND");
        assertError(new PaymentsClient(demo).payRaw(PaymentRequest.fromAccount(utilities, foreign.id(), 100)),
                404, "ACCOUNT_NOT_FOUND");
        assertError(new CardsClient(demo).issueRaw(foreign.id()), 404, "ACCOUNT_NOT_FOUND");

        assertThat(new AccountsClient(alice).get(foreign.id()).balance()).isEqualByComparingTo("1000");
        assertThat(accounts.get(mine.id()).balance()).isEqualByComparingTo("1000");
    }

    @Test
    @Story("US-12 Изоляция данных клиентов")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("AT-38 Действия с чужой картой и чужим автоплатежом дают 404")
    void foreignCardAndAutopayment() {
        Session alice = Users.newClient();
        Account aliceAccount = anAccount(alice).withBalance(1000).create();
        Card aliceCard = new CardsClient(alice).issue(aliceAccount.id());
        long mobile = new PaymentsClient(alice).category("MOBILE").id();
        Autopayment aliceAp = new AutopaymentsClient(alice).create(CreateAutopaymentRequest.monthlyFromAccount(
                "Связь", mobile, aliceAccount.id(), 100, null));

        assertError(new CardsClient(demo).blockRaw(aliceCard.id()), 404, "CARD_NOT_FOUND");
        assertError(new PaymentsClient(demo).payRaw(PaymentRequest.byCard(mobile, aliceCard.id(), 1)),
                404, "CARD_NOT_FOUND");
        assertError(new AutopaymentsClient(demo).disableRaw(aliceAp.id()), 404, "AUTOPAYMENT_NOT_FOUND");

        assertThat(new CardsClient(alice).list().get(0).status()).isEqualTo("ACTIVE");
        assertThat(new AutopaymentsClient(alice).get(aliceAp.id()).active()).isTrue();
    }

    @Test
    @Story("US-13 Блокировка пользователя")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("AT-39 Заблокированный пользователь получает 403, после разблокировки доступ возвращается")
    void blockedUser() {
        Session bob = Users.newClient();
        AdminClient admin = new AdminClient(Users.admin());

        admin.block(bob.userId());

        assertError(new AccountsClient(bob).listRaw(), 403, "USER_BLOCKED");
        assertError(new AuthClient(bob).meRaw(), 403, "USER_BLOCKED");
        assertError(new AuthClient().loginRaw(bob.email(), Users.DEFAULT_PASSWORD), 403, "USER_BLOCKED");

        admin.unblock(bob.userId());

        assertThat(new AccountsClient(bob).listRaw().statusCode()).isEqualTo(200);
        assertThat(new AuthClient().login(bob.email(), Users.DEFAULT_PASSWORD).token()).isNotBlank();
    }
}
