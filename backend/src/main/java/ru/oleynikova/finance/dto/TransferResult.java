package ru.oleynikova.finance.dto;

public record TransferResult(OperationDto operation, AccountDto fromAccount, AccountDto toAccount) {
}
