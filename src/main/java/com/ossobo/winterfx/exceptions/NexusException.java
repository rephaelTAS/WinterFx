package com.ossobo.winterfx.exceptions;

// HIERARQUIA DE EXCEÇÕES DO FRAMEWORK
public class NexusException extends RuntimeException {
    public NexusException(String message) { super(message); }
    public NexusException(String message, Throwable cause) { super(message, cause); }
}

