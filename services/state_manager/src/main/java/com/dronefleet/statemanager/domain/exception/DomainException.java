package com.dronefleet.statemanager.domain.exception;

/**
 * Base class for domain-specific exceptions that shouldn't necessarily trigger a retry if they are
 * business rejections.
 */
public abstract class DomainException extends RuntimeException {
    public DomainException(String message) {
        super(message);
    }
}
