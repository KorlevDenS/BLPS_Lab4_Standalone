package org.korolev.dens.ratingservice.exceptions;

import org.springframework.http.HttpStatus;

public class RateObjectNotFoundException extends RatingException {

    public RateObjectNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }

}
