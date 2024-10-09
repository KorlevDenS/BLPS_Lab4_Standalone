package org.korolev.dens.blps_lab4_standalone.exceptions;

import org.springframework.http.HttpStatus;

public class ForbiddenActionException extends ForumException {

    public ForbiddenActionException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }

}
