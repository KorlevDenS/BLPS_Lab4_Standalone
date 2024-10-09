package org.korolev.dens.blps_lab4_standalone.exceptions;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@Setter
@Getter
public class ForumException extends Exception {

    private HttpStatus possibleStatus = HttpStatus.INTERNAL_SERVER_ERROR;

    public ForumException(String message, HttpStatus possibleStatus) {
        super(message);
        this.possibleStatus = possibleStatus;
    }

    public ForumException(String message) {
        super(message);
    }
}
