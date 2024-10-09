package org.korolev.dens.blps_lab4_standalone.exceptions;

import org.springframework.http.HttpStatus;

public class ForumLogicException extends ForumException {
    public ForumLogicException(String message, HttpStatus possibleStatus) {
        super(message, possibleStatus);
    }
}
