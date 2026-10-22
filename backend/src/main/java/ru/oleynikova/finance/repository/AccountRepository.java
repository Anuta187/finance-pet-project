package ru.oleynikova.finance.repository;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ru.oleynikova.finance.entity.Account;
import ru.oleynikova.finance.entity.User;

public interface AccountRepository extends JpaRepository<Account, Long> {

    List<Account> findAllByOwnerIdOrderByIdAsc(Long ownerId);

    boolean existsByIdAndOwnerId(Long id, Long ownerId);

    long countByOwnerId(Long ownerId);

    // Блокирует строку счёта до конца транзакции
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.id = :id")
    Optional<Account> findByIdForUpdate(@Param("id") Long id);

    // [userId, количество счетов, сумма остатков в копейках]
    @Query("select a.owner.id, count(a), coalesce(sum(a.balanceKop), 0) from Account a where a.owner is not null group by a.owner.id")
    List<Object[]> summaryByOwner();

    @Modifying
    @Query("update Account a set a.owner = :owner where a.owner is null")
    int adoptOrphans(@Param("owner") User owner);
}
