package ru.oleynikova.finance.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ru.oleynikova.finance.entity.Operation;

public interface OperationRepository extends JpaRepository<Operation, Long> {

    // Для дневного лимита
    @Query("""
            select coalesce(sum(o.amountKop), 0) from Operation o
            where o.type = ru.oleynikova.finance.entity.OperationType.TRANSFER
              and o.fromAccount.id = :accountId and o.createdAt >= :since""")
    long sumTransferredSince(@Param("accountId") Long accountId, @Param("since") Instant since);

    @Query("""
            select o from Operation o
              left join fetch o.fromAccount f left join fetch o.toAccount t
              left join fetch o.category left join fetch o.card
            where f.owner.id = :userId or t.owner.id = :userId
            order by o.id desc""")
    List<Operation> findLatestByOwner(@Param("userId") Long userId, Pageable pageable);

    @Query("""
            select o from Operation o
              left join fetch o.fromAccount f left join fetch o.toAccount t
              left join fetch o.category left join fetch o.card
            where f.id = :accountId or t.id = :accountId
            order by o.id desc""")
    List<Operation> findLatestByAccount(@Param("accountId") Long accountId, Pageable pageable);

    @Query("""
            select o from Operation o
              left join fetch o.fromAccount f left join fetch f.owner
              left join fetch o.toAccount t left join fetch t.owner
              left join fetch o.category left join fetch o.card
            order by o.id desc""")
    List<Operation> findLatestAll(Pageable pageable);

    // [categoryId, сумма в копейках, количество]
    @Query("""
            select o.category.id, sum(o.amountKop), count(o) from Operation o
            where o.type = ru.oleynikova.finance.entity.OperationType.PAYMENT
              and o.fromAccount.owner.id = :userId and o.createdAt >= :from and o.createdAt < :to
            group by o.category.id""")
    List<Object[]> spendingByCategory(@Param("userId") Long userId, @Param("from") Instant from,
                                      @Param("to") Instant to);
}
