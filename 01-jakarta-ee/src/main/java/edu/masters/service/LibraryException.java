package edu.masters.service;

import jakarta.ejb.ApplicationException;

// При ошибке ввода контейнер отменяет изменения в текущей транзакции.
@ApplicationException(rollback = true)
public class LibraryException extends RuntimeException {
    public LibraryException(String message) {
        super(message);
    }
}
