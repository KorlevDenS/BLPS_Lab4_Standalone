package org.korolev.dens.blps_lab4_standalone.exceptions;

import org.springframework.http.HttpStatus;

public class ImageAccessException extends ForumException {
    public ImageAccessException(String message, HttpStatus possibleStatus) {
        super(message, possibleStatus);
    }
}
