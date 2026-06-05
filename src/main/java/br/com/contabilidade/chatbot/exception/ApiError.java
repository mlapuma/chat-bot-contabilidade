package br.com.contabilidade.chatbot.exception;

import java.time.LocalDateTime;
import java.util.Map;

public class ApiError {

    private LocalDateTime timestamp;
    private int status;
    private String message;
    private Map<String, String> fields;

    public ApiError(LocalDateTime timestamp, int status, String message, Map<String, String> fields) {
        this.timestamp = timestamp;
        this.status = status;
        this.message = message;
        this.fields = fields;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public int getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public Map<String, String> getFields() {
        return fields;
    }
}
