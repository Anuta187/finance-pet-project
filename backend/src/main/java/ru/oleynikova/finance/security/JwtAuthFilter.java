package ru.oleynikova.finance.security;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import ru.oleynikova.finance.entity.User;
import ru.oleynikova.finance.error.ErrorCode;
import ru.oleynikova.finance.repository.UserRepository;

// Пользователь читается из базы на каждом запросе, поэтому блокировка срабатывает сразу
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String PREFIX = "Bearer ";

    private final JwtService jwt;
    private final UserRepository users;
    private final SecurityErrorWriter errors;

    public JwtAuthFilter(JwtService jwt, UserRepository users, SecurityErrorWriter errors) {
        this.jwt = jwt;
        this.users = users;
        this.errors = errors;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || header.isBlank()) {
            chain.doFilter(request, response);   // анонимный запрос: решит SecurityConfig (401 или permitAll)
            return;
        }
        if (!header.startsWith(PREFIX)) {
            errors.write(response, ErrorCode.UNAUTHORIZED, "Ожидается заголовок Authorization: Bearer <токен>");
            return;
        }
        Optional<User> user = jwt.parseUserId(header.substring(PREFIX.length()).trim()).flatMap(users::findById);
        if (user.isEmpty()) {
            errors.write(response, ErrorCode.UNAUTHORIZED, "Токен недействителен или истёк. Войдите заново");
            return;
        }
        User u = user.get();
        if (u.isBlocked()) {
            errors.write(response, ErrorCode.USER_BLOCKED, "Пользователь заблокирован. Обратитесь в поддержку банка");
            return;
        }
        var principal = new AuthUser(u.getId(), u.getEmail(), u.getRole());
        var auth = new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole().name())));
        SecurityContextHolder.getContext().setAuthentication(auth);
        chain.doFilter(request, response);
    }
}
