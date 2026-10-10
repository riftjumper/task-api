package com.example.task_api.order;

import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(assignableTypes = OrderController.class)
public class OrderExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(OrderExceptionHandler.class);

    public record ErrorResponse(String code, String message, Map<String, String> fields) {
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> invalidFields(MethodArgumentNotValidException error) {
        Map<String, String> fields = error.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        field -> field.getField().equals("productId") ? "product_id" : field.getField(),
                        field -> field.getDefaultMessage() == null ? "Tidak valid" : field.getDefaultMessage(),
                        (first, second) -> first));
        return ResponseEntity.unprocessableEntity()
                .body(new ErrorResponse("INVALID_INPUT", "Input tidak valid", fields));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> malformedBody() {
        return ResponseEntity.unprocessableEntity()
                .body(new ErrorResponse("INVALID_INPUT", "Body JSON tidak valid", Map.of()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> invalidPath() {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("INVALID_PATH", "ID tidak valid", Map.of()));
    }

    @ExceptionHandler(OrderApiException.class)
    public ResponseEntity<ErrorResponse> expectedError(OrderApiException error) {
        HttpStatus status = switch (error.getReason()) {
            case PRODUCT_NOT_FOUND, ORDER_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case INSUFFICIENT_STOCK -> HttpStatus.CONFLICT;
        };
        return ResponseEntity.status(status)
                .body(new ErrorResponse(error.getReason().name(), error.getMessage(), Map.of()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> unexpectedError(Exception error) {
        log.error("Order API gagal", error);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_ERROR", "Terjadi gangguan sistem", Map.of()));
    }
}
