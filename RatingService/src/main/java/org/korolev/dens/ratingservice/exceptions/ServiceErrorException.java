package org.korolev.dens.ratingservice.exceptions;

import org.springframework.http.HttpStatus;

public class ServiceErrorException extends RatingException {

    public ServiceErrorException(String message) {
        super(message, HttpStatus.SERVICE_UNAVAILABLE);
    }

}
