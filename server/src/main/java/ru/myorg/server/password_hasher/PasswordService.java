package ru.myorg.server.password_hasher;

import java.security.spec.InvalidKeySpecException;

/**
 * Интерфейс позволяет работать с различными алгоритмами шифрования пароля пользователя для сохранения в базе данных
 */
public interface PasswordService {
    byte[] generateSalt();

    byte[] generateSecureHash(String word, byte[] salt) throws InvalidKeySpecException;

    boolean compareSecureHashes(String word, byte[] hash, byte[] salt)throws InvalidKeySpecException;
}
