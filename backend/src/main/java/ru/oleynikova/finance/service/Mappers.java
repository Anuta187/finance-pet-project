package ru.oleynikova.finance.service;

import ru.oleynikova.finance.dto.AccountDto;
import ru.oleynikova.finance.dto.AutopaymentDto;
import ru.oleynikova.finance.dto.OperationDto;
import ru.oleynikova.finance.entity.Account;
import ru.oleynikova.finance.entity.Autopayment;
import ru.oleynikova.finance.entity.Card;
import ru.oleynikova.finance.entity.Category;
import ru.oleynikova.finance.entity.Operation;
import ru.oleynikova.finance.entity.User;
import ru.oleynikova.finance.support.Money;

public final class Mappers {

    private Mappers() {
    }

    public static AccountDto account(Account a) {
        return new AccountDto(a.getId(), a.getName(), a.getCurrency(), Money.toRub(a.getBalanceKop()), a.getCreatedAt());
    }

    public static OperationDto operation(Operation o) {
        return operation(o, null);
    }

    public static OperationDto operationWithOwner(Operation o) {
        Account acc = o.getFromAccount() != null ? o.getFromAccount() : o.getToAccount();
        return operation(o, acc == null ? null : acc.getOwner());
    }

    private static OperationDto operation(Operation o, User owner) {
        Account from = o.getFromAccount();
        Account to = o.getToAccount();
        Category cat = o.getCategory();
        Card card = o.getCard();
        return new OperationDto(o.getId(), o.getType(),
                from == null ? null : from.getId(), from == null ? null : from.getName(),
                to == null ? null : to.getId(), to == null ? null : to.getName(),
                Money.toRub(o.getAmountKop()), o.getComment(), o.getCreatedAt(),
                cat == null ? null : cat.getId(), cat == null ? null : cat.getName(),
                card == null ? null : card.getId(), card == null ? null : card.maskedNumber(),
                o.getAutopayment() == null ? null : o.getAutopayment().getId(),
                owner == null ? null : owner.getId(), owner == null ? null : owner.getEmail());
    }

    public static AutopaymentDto autopayment(Autopayment ap) {
        Card card = ap.getCard();
        return new AutopaymentDto(ap.getId(), ap.getName(), ap.getAccount().getId(), ap.getAccount().getName(),
                card == null ? null : card.getId(), card == null ? null : card.maskedNumber(),
                ap.getCategory().getId(), ap.getCategory().getName(), Money.toRub(ap.getAmountKop()), ap.getPeriod(),
                ap.getStartDate(), ap.getNextRunDate(), ap.isActive(), ap.getLastRunAt(), ap.getLastStatus(),
                ap.getLastError(), ap.getCreatedAt());
    }
}
