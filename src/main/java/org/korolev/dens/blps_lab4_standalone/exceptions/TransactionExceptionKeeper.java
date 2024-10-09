package org.korolev.dens.blps_lab4_standalone.exceptions;

import lombok.Getter;

@Getter
public class TransactionExceptionKeeper {

    private ForumException forumException;

    public <T extends ForumException> void setEx(T forumException) {
        this.forumException = forumException;
    }

    public void throwIfSet() throws ForumException {
        if (this.forumException != null) {
            throw this.forumException;
        }
    }

}
