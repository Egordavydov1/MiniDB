package com.minidb;

/** Общее исключение для всех ошибок СУБД (парсинг, I/O, нарушение ограничений). */
public class DbException extends RuntimeException {
    public DbException(String message) { super(message); }
    public DbException(String message, Throwable cause) { super(message, cause); }
}