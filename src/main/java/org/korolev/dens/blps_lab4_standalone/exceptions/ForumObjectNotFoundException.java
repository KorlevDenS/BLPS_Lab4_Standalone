package org.korolev.dens.blps_lab4_standalone.exceptions;

import org.springframework.http.HttpStatus;

public class ForumObjectNotFoundException extends ForumException {
    public ForumObjectNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
