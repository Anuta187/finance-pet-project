package ru.oleynikova.finance.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.node.DecimalNode;

import ru.oleynikova.finance.dto.CreateAccountRequest;
import ru.oleynikova.finance.entity.Account;
import ru.oleynikova.finance.entity.Autopayment;
import ru.oleynikova.finance.entity.AutopaymentPeriod;
import ru.oleynikova.finance.entity.Card;
import ru.oleynikova.finance.entity.Operation;
import ru.oleynikova.finance.entity.OperationType;
import ru.oleynikova.finance.entity.Role;
import ru.oleynikova.finance.entity.User;
import ru.oleynikova.finance.repository.AccountRepository;
import ru.oleynikova.finance.repository.AutopaymentRepository;
import ru.oleynikova.finance.repository.CardRepository;
import ru.oleynikova.finance.repository.CategoryRepository;
import ru.oleynikova.finance.repository.OperationRepository;
import ru.oleynikova.finance.repository.UserRepository;

/** Демо-данные. Счета из старой версии без владельца отдаются демо-клиенту. */
@Service
public class SeedService {

    private static final Logger log = LoggerFactory.getLogger(SeedService.class);

    public static final String ADMIN_EMAIL = "admin@finance.local";
    public static final String DEMO_EMAIL = "demo@finance.local";

    static final List<CreateAccountRequest> DEMO_ACCOUNTS = List.of(
            new CreateAccountRequest("Основной", DecimalNode.valueOf(new BigDecimal("50000"))),
            new CreateAccountRequest("Накопительный", DecimalNode.valueOf(new BigDecimal("150000"))),
            new CreateAccountRequest("Наличные", DecimalNode.valueOf(new BigDecimal("5000"))));

    private record DemoPayment(String category, long kop, String comment, boolean byCard) {
    }

    private static final List<DemoPayment> DEMO_PAYMENTS = List.of(
            new DemoPayment("MOBILE", 65_000, "Мобильная связь, +7 914 ***-**-12", false),
            new DemoPayment("INTERNET", 89_000, "Домашний интернет", false),
            new DemoPayment("UTILITIES", 432_050, "Квартплата за октябрь", false),
            new DemoPayment("TRANSPORT", 120_000, "Пополнение транспортной карты", true),
            new DemoPayment("ENTERTAINMENT", 39_900, "Подписка на музыку", true));

    private final UserRepository users;
    private final AccountRepository accounts;
    private final OperationRepository operations;
    private final CategoryRepository categories;
    private final CardRepository cards;
    private final AutopaymentRepository autopayments;
    private final FinanceService financeService;
    private final AuthService authService;
    private final CardService cardService;
    private final Clock clock;
    private final String adminPassword;
    private final String demoPassword;

    public SeedService(UserRepository users, AccountRepository accounts, OperationRepository operations,
                       CategoryRepository categories, CardRepository cards, AutopaymentRepository autopayments,
                       FinanceService financeService, AuthService authService, CardService cardService, Clock clock,
                       @Value("${app.seed.admin-password:admin12345}") String adminPassword,
                       @Value("${app.seed.demo-password:demo12345}") String demoPassword) {
        this.users = users;
        this.accounts = accounts;
        this.operations = operations;
        this.categories = categories;
        this.cards = cards;
        this.autopayments = autopayments;
        this.financeService = financeService;
        this.authService = authService;
        this.cardService = cardService;
        this.clock = clock;
        this.adminPassword = adminPassword;
        this.demoPassword = demoPassword;
    }

    @Transactional
    public void seed() {
        seed(true);
    }

    @Transactional
    public void seed(boolean withDemo) {
        if (!users.existsByEmail(ADMIN_EMAIL)) {
            users.save(authService.createUser(ADMIN_EMAIL, adminPassword, "Администратор", Role.ADMIN));
            log.info("Создан администратор {}", ADMIN_EMAIL);
        }
        if (!withDemo || users.existsByEmail(DEMO_EMAIL)) {
            return;
        }
        User demo = users.save(authService.createUser(DEMO_EMAIL, demoPassword, "Иван Петров", Role.CLIENT));
        var now = clock.instant().truncatedTo(ChronoUnit.MILLIS);
        int adopted = accounts.adoptOrphans(demo);
        Account main;
        if (adopted > 0) {
            // Старая база: деньги не трогаем, только добавляем карту и автоплатёж
            log.info("Счета из v1 ({} шт.) переданы демо-клиенту {}", adopted, DEMO_EMAIL);
            main = accounts.findAllByOwnerIdOrderByIdAsc(demo.getId()).get(0);
            cards.save(cardService.newCard(main, "IVAN PETROV"));
        } else {
            List<Account> demoAccounts = DEMO_ACCOUNTS.stream().map(r -> financeService.openAccount(demo, r)).toList();
            main = demoAccounts.get(0);
            Account savings = demoAccounts.get(1);
            main.debit(500_000);
            savings.credit(500_000);
            operations.save(new Operation(OperationType.TRANSFER, main, savings, 500_000, "Отложить на отпуск", now));

            Card card = cards.save(cardService.newCard(main, "IVAN PETROV"));
            for (DemoPayment p : DEMO_PAYMENTS) {
                main.debit(p.kop());
                operations.save(Operation.payment(main, p.kop(), p.comment(), now,
                        categories.findByCode(p.category()).orElseThrow(), p.byCard() ? card : null, null));
            }
        }
        autopayments.save(new Autopayment(main, null, categories.findByCode("MOBILE").orElseThrow(),
                "Мобильная связь", 65_000, AutopaymentPeriod.MONTHLY,
                LocalDate.now(clock).withDayOfMonth(1).plusMonths(1), now));
        log.info("Добавлен демо-клиент {} с демо-данными", DEMO_EMAIL);
    }
}
