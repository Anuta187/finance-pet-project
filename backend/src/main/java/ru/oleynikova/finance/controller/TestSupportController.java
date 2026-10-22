package ru.oleynikova.finance.controller;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Hidden;

import ru.oleynikova.finance.config.MutableClock;
import ru.oleynikova.finance.error.ApiException;
import ru.oleynikova.finance.service.SeedService;

/** Сброс базы и управление временем для автотестов. Работает только в профиле test. */
@Hidden
@Profile("test")
@RestController
@RequestMapping("/api/test")
public class TestSupportController {

    private static final String[] TABLES = {"operations", "autopayments", "cards", "accounts", "users"};

    private final JdbcTemplate jdbc;
    private final SeedService seedService;
    private final MutableClock clock;

    public TestSupportController(JdbcTemplate jdbc, SeedService seedService, MutableClock clock) {
        this.jdbc = jdbc;
        this.seedService = seedService;
        this.clock = clock;
    }

    @PostMapping("/reset")
    public ResponseEntity<Void> reset() {
        clock.reset();
        for (String table : TABLES) {
            jdbc.execute("DELETE FROM " + table);
        }
        for (String table : TABLES) {
            jdbc.execute("ALTER TABLE " + table + " ALTER COLUMN id RESTART WITH 1");
        }
        seedService.seed();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/clock")
    public Map<String, Object> clock() {
        return Map.of("now", Instant.now(clock).toString(), "today", LocalDate.now(clock).toString(),
                "offsetSeconds", clock.offset().toSeconds());
    }

    // {"shiftDays": 31} или {"reset": true}
    @PostMapping("/clock")
    public Map<String, Object> moveClock(@RequestBody Map<String, Object> body) {
        if (Boolean.TRUE.equals(body.get("reset"))) {
            clock.reset();
        } else if (body.get("shiftDays") instanceof Number days) {
            clock.shift(Duration.ofDays(days.longValue()));
        } else {
            throw ApiException.validation("shiftDays", "Передайте shiftDays (число дней) или reset=true");
        }
        return clock();
    }
}
