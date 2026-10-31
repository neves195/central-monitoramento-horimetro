package br.com.manutencao.central_manutencao.common;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Converte as exceções lançadas nos controllers em respostas HTTP com o código certo
 * e uma mensagem clara, em vez do 500 padrão do Spring.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Formato único de erro devolvido pela API
    public record ApiError(int status, String error, List<String> messages, LocalDateTime timestamp) {
    }

    // Equipamento não encontrado -> 404
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, List.of(ex.getMessage()));
    }

    // Regra de negócio violada (ex: código duplicado) -> 409
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiError> handleBusinessRule(BusinessRuleException ex) {
        return build(HttpStatus.CONFLICT, List.of(ex.getMessage()));
    }

    // Campo inválido no @Valid (ex: code em branco) -> 400, listando cada campo com problema
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        List<String> messages = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();
        return build(HttpStatus.BAD_REQUEST, messages);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, List<String> messages) {
        ApiError body = new ApiError(status.value(), status.getReasonPhrase(), messages, LocalDateTime.now());
        return ResponseEntity.status(status).body(body);
    }
}
