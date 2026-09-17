package edu.masters.ee;

import jakarta.ejb.ApplicationException;

/** Ожидаемая бизнес-ошибка откатывает транзакцию и превращается в HTTP 4xx. */
@ApplicationException(rollback = true)
public class LibraryException extends RuntimeException {
    public final int status;

    public LibraryException(int status, String message) {
        super(message);
        this.status = status;
    }
}
