package com.dronefleet.statemanager.domain.exception;

/**
 * Thrown when a business rule prevents an operation (e.g. drone not available).
 * These are generally not retryable at the infrastructure level.
 */
public class BusinessRejectionException extends DomainException {
    public BusinessRejectionException(String message) {
        super(message);
    }
}
