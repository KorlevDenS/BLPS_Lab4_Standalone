package org.korolev.dens.ratingservice.exceptions;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@Setter
@Getter
public class RatingException extends Exception {

    private HttpStatus possibleStatus = HttpStatus.INTERNAL_SERVER_ERROR;

    public RatingException(String message, HttpStatus possibleStatus) {
        super(message);
        this.possibleStatus = possibleStatus;
    }

    public RatingException(String message) {
        super(message);
    }
}
