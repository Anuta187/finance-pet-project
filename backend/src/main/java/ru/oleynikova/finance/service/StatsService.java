package ru.oleynikova.finance.service;

import java.time.Clock;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.oleynikova.finance.dto.SpendingDto;
import ru.oleynikova.finance.entity.Category;
import ru.oleynikova.finance.error.ApiException;
import ru.oleynikova.finance.repository.CategoryRepository;
import ru.oleynikova.finance.repository.OperationRepository;
import ru.oleynikova.finance.support.Money;

@Service
public class StatsService {

    private final OperationRepository operations;
    private final CategoryRepository categories;
    private final Clock clock;

    public StatsService(OperationRepository operations, CategoryRepository categories, Clock clock) {
        this.operations = operations;
        this.categories = categories;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public SpendingDto spending(long userId, String rawMonth) {
        YearMonth month;
        try {
            month = rawMonth == null || rawMonth.isBlank() ? YearMonth.now(clock) : YearMonth.parse(rawMonth.trim());
        } catch (DateTimeParseException e) {
            throw ApiException.validation("month", "Параметр «month» должен быть в формате ГГГГ-ММ");
        }
        ZoneId zone = clock.getZone();
        var from = month.atDay(1).atStartOfDay(zone).toInstant();
        var to = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant();

        Map<Long, Category> byId = categories.findAll().stream()
                .collect(Collectors.toMap(Category::getId, Function.identity()));
        List<SpendingDto.Item> items = operations.spendingByCategory(userId, from, to).stream()
                .map(row -> {
                    Category c = byId.get((Long) row[0]);
                    return new SpendingDto.Item(c.getId(), c.getCode(), c.getName(), c.getColor(),
                            Money.toRub(((Number) row[1]).longValue()), ((Number) row[2]).longValue());
                })
                .sorted(Comparator.comparing(SpendingDto.Item::amount).reversed())
                .toList();
        var total = items.stream().map(SpendingDto.Item::amount).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        return new SpendingDto(month.toString(), total.signum() == 0 ? java.math.BigDecimal.ZERO : total, items);
    }
}
