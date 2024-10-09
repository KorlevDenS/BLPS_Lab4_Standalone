package org.korolev.dens.ratingservice.controllers;

import org.korolev.dens.ratingservice.exceptions.RatingException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class RateExceptionHandler {

    @ExceptionHandler(RatingException.class)
    public ResponseEntity<?> handleRatingException(RatingException e) {
        return new ResponseEntity<>(e.getMessage(), e.getPossibleStatus());
    }

}
