package ru.oleynikova.finance.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

// Часы вынесены в бин, чтобы в тестах можно было подменить время
@Configuration
public class ClockConfig {

    @Bean
    @Profile("!test")
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    @Profile("test")
    public MutableClock testClock() {
        return new MutableClock(Clock.systemDefaultZone());
    }
}
