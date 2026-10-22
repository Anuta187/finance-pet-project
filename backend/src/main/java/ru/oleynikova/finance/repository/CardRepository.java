package ru.oleynikova.finance.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ru.oleynikova.finance.entity.Card;

public interface CardRepository extends JpaRepository<Card, Long> {

    @Query("select c from Card c join fetch c.account a where a.owner.id = :userId order by c.id asc")
    List<Card> findAllByOwner(@Param("userId") Long userId);
}
