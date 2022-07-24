package ru.myorg.server.authentication;

/**
 * Возвращает результаты аутентификации пользователя и сообщение об ошибке, если в процессе аутентификации возникла ошибка
 */
public class VerificationResult {
    private final VerificationStatus status;
    private String errorMessage;

    public VerificationResult(VerificationStatus status) {
        this.status = status;
    }

    public VerificationResult(VerificationStatus status, String errorMessage) {
        this.status = status;
        this.errorMessage = errorMessage;
    }

    public VerificationStatus getStatus() {
        return status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
