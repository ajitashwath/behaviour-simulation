package com.simulation.control.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a simulation operation fails.
 */
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class SimulationException extends RuntimeException {

    public SimulationException(String message) {
        super(message);
    }

    public SimulationException(String message, Throwable cause) {
        super(message, cause);
    }
}
