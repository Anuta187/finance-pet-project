package ru.oleynikova.finance.autotests.data;

import io.qameta.allure.Allure;

import ru.oleynikova.finance.autotests.api.client.AccountsClient;
import ru.oleynikova.finance.autotests.api.model.Account;
import ru.oleynikova.finance.autotests.api.model.CreateAccountRequest;

/** Пример: anAccount(session).withBalance(1000).create() */
public final class AccountBuilder {

    private final Session owner;
    private String name = TestData.uniqueName();
    private Object initialBalance;

    private AccountBuilder(Session owner) {
        this.owner = owner;
    }

    public static AccountBuilder anAccount(Session owner) {
        return new AccountBuilder(owner);
    }

    public AccountBuilder named(String name) {
        this.name = name;
        return this;
    }

    public AccountBuilder withNamePrefix(String prefix) {
        this.name = TestData.uniqueName(prefix);
        return this;
    }

    public AccountBuilder withBalance(Object initialBalance) {
        this.initialBalance = initialBalance;
        return this;
    }

    public CreateAccountRequest build() {
        return new CreateAccountRequest(name, initialBalance);
    }

    public Account create() {
        return Allure.step("Предусловие: " + owner.email() + " открывает счёт «" + name + "» с остатком "
                        + (initialBalance == null ? 0 : initialBalance),
                () -> new AccountsClient(owner).create(build()));
    }
}
