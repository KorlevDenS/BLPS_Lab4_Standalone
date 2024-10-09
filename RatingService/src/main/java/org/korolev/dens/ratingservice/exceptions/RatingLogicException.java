package org.korolev.dens.ratingservice.exceptions;

import org.springframework.http.HttpStatus;

public class RatingLogicException extends RatingException {

    public RatingLogicException(String message, HttpStatus possibleStatus) {
        super(message, possibleStatus);
    }

}
