package ru.oleynikova.finance.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import ru.oleynikova.finance.service.SeedService;

/** При старте создаёт администратора и демо-клиента. */
@Component
public class SeedRunner implements ApplicationRunner {

    private final SeedService seedService;
    private final boolean demo;

    public SeedRunner(SeedService seedService, @Value("${app.seed.enabled:true}") boolean demo) {
        this.seedService = seedService;
        this.demo = demo;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedService.seed(demo);
    }
}
