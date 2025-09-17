package ru.practicum.stat.exception;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ErrorResponse {
    private final String error;
    private final String message;
    private LocalDateTime timestamp;
}