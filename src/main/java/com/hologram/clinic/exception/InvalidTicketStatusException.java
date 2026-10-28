package com.hologram.clinic.exception;

public class InvalidTicketStatusException extends Exception {
    public InvalidTicketStatusException(String message) {
        super(message);
    }
}
