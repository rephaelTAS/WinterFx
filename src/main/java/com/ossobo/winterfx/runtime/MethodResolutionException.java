package com.ossobo.winterfx.runtime;

public class MethodResolutionException extends IllegalArgumentException {

    private final ResolutionStatus status;

    public MethodResolutionException(ResolutionStatus status, String message) {
        super(message);
        this.status = status;
    }

    public MethodResolutionException(ResolutionStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public ResolutionStatus getStatus() {
        return status;
    }

    public boolean isSignatureMismatch() {
        return status == ResolutionStatus.SIGNATURE_MISMATCH;
    }

    public boolean isNameNotFound() {
        return status == ResolutionStatus.NAME_NOT_FOUND;
    }

    public boolean isFound() {
        return status == ResolutionStatus.FOUND;
    }
}