package ru.oleynikova.finance.security;

import java.io.IOException;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import ru.oleynikova.finance.error.ErrorCode;
import ru.oleynikova.finance.error.ErrorResponse;

/** Ошибки 401 и 403 в том же формате, что и остальные. */
@Component
public class SecurityErrorWriter {

    private final ObjectMapper mapper;

    public SecurityErrorWriter(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public void write(HttpServletResponse response, ErrorCode code, String message) throws IOException {
        response.setStatus(code.status().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        mapper.writeValue(response.getOutputStream(), ErrorResponse.of(code, message, null));
    }
}
