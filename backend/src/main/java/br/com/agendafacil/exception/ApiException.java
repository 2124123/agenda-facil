package br.com.agendafacil.exception;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {
    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public static ApiException naoEncontrado(String msg) { return new ApiException(HttpStatus.NOT_FOUND, msg); }
    public static ApiException invalido(String msg) { return new ApiException(HttpStatus.BAD_REQUEST, msg); }
    public static ApiException conflito(String msg) { return new ApiException(HttpStatus.CONFLICT, msg); }
    public static ApiException proibido(String msg) { return new ApiException(HttpStatus.FORBIDDEN, msg); }
}
