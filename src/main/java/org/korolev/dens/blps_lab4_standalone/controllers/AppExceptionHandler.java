package org.korolev.dens.blps_lab4_standalone.controllers;

import org.hibernate.exception.ConstraintViolationException;
import org.korolev.dens.blps_lab4_standalone.exceptions.ForumException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class AppExceptionHandler {

    @ExceptionHandler(ForumException.class)
    public ResponseEntity<?> handleForumException(ForumException e) {
        return new ResponseEntity<>(e.getMessage(), e.getPossibleStatus());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<?> handleAuthenticationException(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Неверное имя пользователя или пароль");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        if (ex.getMessage().contains("email")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Невалидный email");
        }
        if (ex.getMessage().contains("birthday")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Дата рождения в будущем");
        }
        if (ex.getMessage().contains("password")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Пароль должен содержать от 8 до 20 символов: латинских букв или цифр");
        }
        if (ex.getMessage().contains("rating")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Оценка должна быть от 1 до 10");
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Неизвестные невалидные данные");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        if (ex.getCause() instanceof ConstraintViolationException constraintViolationException) {
            if (constraintViolationException.getMessage().contains("chapter_title_key")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Раздел с таким названием уже существует");
            }
            if (constraintViolationException.getMessage().contains("client_login_key")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Пользователь с таким login уже существует");
            }
            if (constraintViolationException.getMessage().contains("client_email_key")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Пользователь с таким email уже существует");
            }
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Произошла ошибка (Случай не рассмотрен)");
    }

}
