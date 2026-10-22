package ru.oleynikova.finance.error;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApi(ApiException e) {
        return build(e.getCode(), e.getMessage(), e.getDetails());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        Object target = e.getBindingResult().getTarget();
        List<String> order = target == null ? List.of()
                : Arrays.stream(target.getClass().getDeclaredFields()).map(Field::getName).toList();
        FieldError first = e.getBindingResult().getFieldErrors().stream()
                .min(Comparator.comparingInt(fe -> order.indexOf(fe.getField())))
                .orElse(null);
        if (first == null) {
            return build(ErrorCode.VALIDATION_ERROR, "Некорректный запрос", null);
        }
        return build(ErrorCode.VALIDATION_ERROR, first.getDefaultMessage(), Map.of("field", first.getField()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException e) {
        Throwable cause = e.getMostSpecificCause();
        if (cause instanceof JsonParseException) {
            return build(ErrorCode.INVALID_JSON, "Тело запроса не является корректным JSON", null);
        }
        if (cause instanceof JsonMappingException jme && !jme.getPath().isEmpty()) {
            String field = jme.getPath().get(jme.getPath().size() - 1).getFieldName();
            return build(ErrorCode.VALIDATION_ERROR, "Некорректный формат поля «" + field + "»", Map.of("field", field));
        }
        return build(ErrorCode.INVALID_JSON, "Тело запроса не является корректным JSON", null);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaType(HttpMediaTypeNotSupportedException e) {
        return build(ErrorCode.INVALID_JSON, "Тело запроса должно быть в формате JSON (Content-Type: application/json)", null);
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class,
            HttpRequestMethodNotSupportedException.class})
    public ResponseEntity<ErrorResponse> handleNotFound(Exception e, HttpServletRequest request) {
        String url = request.getRequestURI() + (request.getQueryString() != null ? "?" + request.getQueryString() : "");
        return build(ErrorCode.NOT_FOUND, "Маршрут " + request.getMethod() + " " + url + " не найден", null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleOther(Exception e) {
        log.error("Непредвиденная ошибка", e);
        return build(ErrorCode.INTERNAL_ERROR, "Внутренняя ошибка сервера", null);
    }

    private static ResponseEntity<ErrorResponse> build(ErrorCode code, String message, Map<String, Object> details) {
        return ResponseEntity.status(code.status()).body(ErrorResponse.of(code, message, details));
    }
}
