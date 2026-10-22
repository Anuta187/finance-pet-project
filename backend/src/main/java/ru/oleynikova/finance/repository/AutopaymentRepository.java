package ru.oleynikova.finance.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ru.oleynikova.finance.entity.Autopayment;

public interface AutopaymentRepository extends JpaRepository<Autopayment, Long> {

    @Query("""
            select ap from Autopayment ap join fetch ap.account a join fetch ap.category left join fetch ap.card
            where a.owner.id = :userId order by ap.id desc""")
    List<Autopayment> findAllByOwner(@Param("userId") Long userId);

    @Query("select ap.id from Autopayment ap where ap.active = true and ap.nextRunDate <= :today order by ap.id")
    List<Long> findDueIds(@Param("today") LocalDate today);
}
