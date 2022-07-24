package ru.myorg.server.authentication;

/**
 * Список возможных вариантов аутентификации пользователя: ошибка, авторизован, не авторизован
 */
public enum VerificationStatus {
    ERROR,
    AUTHORIZED,
    NOT_AUTHORIZED
}
