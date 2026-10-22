package ru.oleynikova.finance.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import ru.oleynikova.finance.dto.CardDto;
import ru.oleynikova.finance.dto.IssueCardRequest;
import ru.oleynikova.finance.entity.CardStatus;
import ru.oleynikova.finance.security.AuthUser;
import ru.oleynikova.finance.service.CardService;

@RestController
@RequestMapping("/api/cards")
@Tag(name = "Cards", description = "Карты, привязанные к счетам")
public class CardController {

    private final CardService service;

    public CardController(CardService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Мои карты")
    public List<CardDto> list(@AuthenticationPrincipal AuthUser me) {
        return service.list(me.id());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Выпустить виртуальную карту к счёту")
    public CardDto issue(@AuthenticationPrincipal AuthUser me, @RequestBody(required = false) IssueCardRequest request) {
        return service.issue(me.id(), request);
    }

    @PostMapping("/{id}/block")
    @Operation(summary = "Заблокировать карту")
    public CardDto block(@AuthenticationPrincipal AuthUser me, @PathVariable("id") String id) {
        return service.setStatus(me.id(), id, CardStatus.BLOCKED);
    }

    @PostMapping("/{id}/unblock")
    @Operation(summary = "Разблокировать карту")
    public CardDto unblock(@AuthenticationPrincipal AuthUser me, @PathVariable("id") String id) {
        return service.setStatus(me.id(), id, CardStatus.ACTIVE);
    }
}
