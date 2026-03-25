package com.university.attendance.exception;

/**
 * Thrown when a student scans a QR code that has already expired
 * or whose token is invalid. Handled by GlobalExceptionHandler
 * to return a 400 Bad Request with a clear error message.
 */
public class QrExpiredException extends RuntimeException {

    public QrExpiredException(String message) {
        super(message);
    }
}
