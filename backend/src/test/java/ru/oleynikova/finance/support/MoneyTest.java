package ru.oleynikova.finance.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DecimalNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;

import ru.oleynikova.finance.error.ApiException;
import ru.oleynikova.finance.error.ErrorCode;

@DisplayName("Money: разбор и форматирование сумм")
class MoneyTest {

    @ParameterizedTest(name = "«{0}»: {1} коп.")
    @CsvSource({"0.01, 1", "1, 100", "10.5, 1050", "'10,50', 1050", "' 300.30 ', 30030", "100000, 10000000"})
    @DisplayName("Корректные суммы переводятся в копейки")
    void parsesValidAmounts(String raw, long expectedKop) {
        assertThat(Money.parseAmount(TextNode.valueOf(raw), "amount", false)).isEqualTo(expectedKop);
    }

    @ParameterizedTest(name = "«{0}»: INVALID_AMOUNT")
    @ValueSource(strings = {"0", "-1", "10.123", "abc", "1e3", "1.", ".5", "1 000"})
    @DisplayName("Некорректные суммы отклоняются с INVALID_AMOUNT")
    void rejectsInvalidAmounts(String raw) {
        assertThatThrownBy(() -> Money.parseAmount(TextNode.valueOf(raw), "amount", false))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getCode()).isEqualTo(ErrorCode.INVALID_AMOUNT);
    }

    @Test
    @DisplayName("Нет значения: VALIDATION_ERROR, булево: INVALID_AMOUNT, число из JSON без потерь")
    void handlesJsonTypes() {
        assertThatThrownBy(() -> Money.parseAmount(NullNode.getInstance(), "amount", false))
                .extracting(e -> ((ApiException) e).getCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
        assertThatThrownBy(() -> Money.parseAmount(BooleanNode.TRUE, "amount", false))
                .extracting(e -> ((ApiException) e).getCode()).isEqualTo(ErrorCode.INVALID_AMOUNT);
        assertThat(Money.parseAmount(DecimalNode.valueOf(new BigDecimal("0.1")), "amount", false)).isEqualTo(10);
        assertThat(Money.parseAmount(TextNode.valueOf("0"), "initialBalance", true)).isZero();
    }

    @Test
    @DisplayName("Больше 1 млрд ₽ за операцию: INVALID_AMOUNT")
    void rejectsTooLarge() {
        assertThatThrownBy(() -> Money.parseAmount(TextNode.valueOf("1000000000.01"), "amount", false))
                .extracting(e -> ((ApiException) e).getCode()).isEqualTo(ErrorCode.INVALID_AMOUNT);
    }

    @Test
    @DisplayName("Копейки переводятся в рубли без лишних нулей")
    void formatsRubles() {
        assertThat(Money.toRub(10_000_000).toString()).isEqualTo("100000");
        assertThat(Money.toRub(74_925).toString()).isEqualTo("749.25");
        assertThat(Money.toRub(30).toString()).isEqualTo("0.3");
        assertThat(Money.toRub(0).toString()).isEqualTo("0");
    }
}
