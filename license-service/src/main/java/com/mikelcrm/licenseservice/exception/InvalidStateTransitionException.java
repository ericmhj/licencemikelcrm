package com.mikelcrm.licenseservice.exception;

public class InvalidStateTransitionException extends RuntimeException {

    private final String currentState;
    private final String targetState;

    public InvalidStateTransitionException(String currentState, String targetState) {
        super(String.format("Invalid state transition from %s to %s", currentState, targetState));
        this.currentState = currentState;
        this.targetState = targetState;
    }

    public String getCurrentState() {
        return currentState;
    }

    public String getTargetState() {
        return targetState;
    }
}
