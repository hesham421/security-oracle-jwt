package com.example.security.advice;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.hibernate.LazyInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.example.security.dto.ErrorResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.example.security.common.LocalizationHelper;
import com.example.security.common.LocalizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;

@RestControllerAdvice
public class ApiErrors {

    private static final Logger log = LoggerFactory.getLogger(ApiErrors.class);
    @Autowired
    private LocalizationHelper i18n;

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
    List<Map<String, String>> details = ex.getBindingResult().getFieldErrors().stream()
        .map(fe -> Map.of("field", fe.getField(), "issue", i18n.msg(fe)))
                .collect(Collectors.toList());
        return buildResponse(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "VALIDATION_ERROR", Map.of("details", details));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
    List<Map<String, String>> details = ex.getConstraintViolations().stream()
        .map(cv -> Map.of("path", String.valueOf(getPropertyPath(cv)), "issue", i18n.msg(cv.getMessage())))
                .collect(Collectors.toList());
        return buildResponse(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "VALIDATION_ERROR", Map.of("details", details));
    }

    private String getPropertyPath(ConstraintViolation<?> cv) {
        return cv.getPropertyPath() == null ? "" : cv.getPropertyPath().toString();
    }

    @ExceptionHandler({IllegalArgumentException.class, MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class, HttpMediaTypeNotSupportedException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "BAD_REQUEST", Map.of("reason", ex.getMessage()));
    }

    @ExceptionHandler(LocalizedException.class)
    public ResponseEntity<ErrorResponse> handleLocalized(LocalizedException ex) {
        // pass the message key and args to the builder so it can resolve using MessageSource
        return buildResponse(ex.getStatus(), ex.getCode(), ex.getCode(), null, ex.getArgs());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "BAD_REQUEST", Map.of("reason", ex.getMessage()));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuth(AuthenticationException ex) {
        // Don't use the raw exception message as the message key (e.g. "Bad credentials").
        // Use a stable message key and include the original message as a detail so it can
        // still be inspected by clients or logs.
        return buildResponse(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "UNAUTHORIZED", Map.of("reason", ex.getMessage()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "INVALID_CREDENTIALS", Map.of("reason", ex.getMessage()));
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUsernameNotFound(UsernameNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "USER_NOT_FOUND", Map.of("reason", ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, "FORBIDDEN", "FORBIDDEN", Map.of("reason", ex.getMessage()));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoHandlerFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, "NOT_FOUND", "NOT_FOUND", null);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDataAccess(DataAccessException ex) {
        log.error("Database access error", ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "DB_ERROR", "DB_ERROR", null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.error("Data integrity violation", ex);
        return buildResponse(HttpStatus.BAD_REQUEST, "DB_CONSTRAINT_VIOLATION", "DB_CONSTRAINT_VIOLATION", Map.of("reason", ex.getMessage()));
    }

    @ExceptionHandler(LazyInitializationException.class)
    public ResponseEntity<ErrorResponse> handleLazyInit(LazyInitializationException ex) {
        // specific mapping useful during dev; it's usually a programming bug
        log.error("LazyInitializationException: possible missing transaction or fetch", ex);
    return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "INTERNAL_ERROR", null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleOther(Exception ex) {
        log.error("Unhandled exception caught by ApiErrors", ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "INTERNAL_ERROR", null);
    }
    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String code, String message, Map<String, Object> extra, Object... args) {
        String resolved = message == null ? "" : i18n.msg(message, args == null ? new Object[0] : args);
        ErrorResponse dto = new ErrorResponse(Instant.now().toString(), status.value(), code, resolved == null ? "" : resolved, extra);
        return ResponseEntity.status(status).body(dto);
    }
}
 