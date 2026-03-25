package com.university.attendance.exception;

/**
 * Thrown when a student tries to scan a QR code for a session
 * they have already scanned. Handled by GlobalExceptionHandler
 * to return a 400 Bad Request with a clear error message.
 */
public class DuplicateScanException extends RuntimeException {

    public DuplicateScanException(String message) {
        super(message);
    }
}
