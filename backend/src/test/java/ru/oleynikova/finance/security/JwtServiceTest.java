package ru.oleynikova.finance.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.oleynikova.finance.entity.Role;
import ru.oleynikova.finance.entity.User;

@DisplayName("JwtService: выпуск и проверка токенов")
class JwtServiceTest {

    private static final String SECRET = "test-secret-test-secret-test-secret-42";
    private static final Instant T0 = Instant.parse("2026-10-22T00:00:00Z");

    private static User user() {
        User u = new User("demo@finance.local", "hash", "Иван Петров", Role.CLIENT, T0);
        u.setId(42L);
        return u;
    }

    @Test
    @DisplayName("Выданный токен разбирается в id пользователя, срок = ttl")
    void issueAndParse() {
        JwtService jwt = new JwtService(SECRET, Duration.ofHours(24), Clock.fixed(T0, ZoneOffset.UTC));

        JwtService.IssuedToken token = jwt.issue(user());

        assertThat(token.expiresAt()).isEqualTo(T0.plus(Duration.ofHours(24)));
        assertThat(jwt.parseUserId(token.token())).contains(42L);
    }

    @Test
    @DisplayName("Истёкший токен не принимается")
    void expiredToken() {
        String token = new JwtService(SECRET, Duration.ofHours(1), Clock.fixed(T0, ZoneOffset.UTC)).issue(user()).token();
        JwtService later = new JwtService(SECRET, Duration.ofHours(1), Clock.fixed(T0.plusSeconds(3601), ZoneOffset.UTC));

        assertThat(later.parseUserId(token)).isEmpty();
    }

    @Test
    @DisplayName("Токен, подписанный другим ключом, или изменённый токен не принимается")
    void wrongSignatureOrTampered() {
        Clock clock = Clock.fixed(T0, ZoneOffset.UTC);
        String token = new JwtService("another-secret-another-secret-another", Duration.ofHours(1), clock).issue(user()).token();
        JwtService jwt = new JwtService(SECRET, Duration.ofHours(1), clock);
        String own = jwt.issue(user()).token();
        String tampered = own.substring(0, own.length() - 2) + (own.endsWith("A") ? "BB" : "AA");

        assertThat(jwt.parseUserId(token)).isEmpty();
        assertThat(jwt.parseUserId(tampered)).isEmpty();
        assertThat(jwt.parseUserId("not-a-jwt")).isEmpty();
    }

    @Test
    @DisplayName("Секрет короче 32 байт не принимается")
    void shortSecretRejected() {
        assertThatThrownBy(() -> new JwtService("short", Duration.ofHours(1)))
                .isInstanceOf(IllegalStateException.class);
    }
}
