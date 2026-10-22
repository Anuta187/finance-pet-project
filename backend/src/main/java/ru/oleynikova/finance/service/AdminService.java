package ru.oleynikova.finance.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.oleynikova.finance.dto.AdminUserDto;
import ru.oleynikova.finance.dto.OperationDto;
import ru.oleynikova.finance.entity.Role;
import ru.oleynikova.finance.entity.User;
import ru.oleynikova.finance.error.ApiException;
import ru.oleynikova.finance.error.ErrorCode;
import ru.oleynikova.finance.repository.AccountRepository;
import ru.oleynikova.finance.repository.OperationRepository;
import ru.oleynikova.finance.repository.UserRepository;
import ru.oleynikova.finance.support.Ids;
import ru.oleynikova.finance.support.Money;

@Service
public class AdminService {

    private final UserRepository users;
    private final AccountRepository accounts;
    private final OperationRepository operations;

    public AdminService(UserRepository users, AccountRepository accounts, OperationRepository operations) {
        this.users = users;
        this.accounts = accounts;
        this.operations = operations;
    }

    @Transactional(readOnly = true)
    public List<AdminUserDto> listUsers() {
        Map<Long, long[]> summary = new HashMap<>();
        for (Object[] row : accounts.summaryByOwner()) {
            summary.put((Long) row[0], new long[]{((Number) row[1]).longValue(), ((Number) row[2]).longValue()});
        }
        return users.findAllByOrderByIdAsc().stream().map(u -> {
            long[] s = summary.getOrDefault(u.getId(), new long[]{0, 0});
            return new AdminUserDto(u.getId(), u.getEmail(), u.getFullName(), u.getRole(), u.isBlocked(),
                    u.getCreatedAt(), s[0], Money.toRub(s[1]));
        }).toList();
    }

    @Transactional
    public AdminUserDto setBlocked(String rawId, boolean blocked) {
        long id = Ids.parse(rawId, "id");
        User user = users.findById(id).orElseThrow(() -> ApiException.userNotFound(id));
        if (blocked && user.getRole() == Role.ADMIN) {
            throw new ApiException(ErrorCode.CANNOT_BLOCK_ADMIN, "Нельзя заблокировать администратора");
        }
        user.setBlocked(blocked);
        return listUsers().stream().filter(u -> u.id() == id).findFirst().orElseThrow();
    }

    @Transactional(readOnly = true)
    public List<OperationDto> listOperations(String rawLimit) {
        return operations.findLatestAll(PageRequest.of(0, FinanceService.parseLimit(rawLimit))).stream()
                .map(Mappers::operationWithOwner).toList();
    }
}
