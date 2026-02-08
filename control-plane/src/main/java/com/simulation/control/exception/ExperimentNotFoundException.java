package com.simulation.control.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when an experiment is not found.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ExperimentNotFoundException extends RuntimeException {

    public ExperimentNotFoundException(String experimentId) {
        super("Experiment not found: " + experimentId);
    }
}
