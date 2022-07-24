package ru.myorg.server.password_hasher;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;

public class SecureHasher implements PasswordService {
    //инициируем объект генератора безопасного случайного числа
    private final SecureRandom secureRandom = new SecureRandom();
    //инициируем константу длины hash в байтах
    private final int capacity = 16;
    //инициируем константу количества итераций для создания хэш
    // фактически является параметром прочности
    private final int iterationCount = capacity * capacity;
    //инициируем константу длины ключа в алгоритме
    private final int keyLength = 128;
    //инициируем константу названия алгоритма
    private final String secureAlgorithm = "PBKDF2WithHmacSHA1";
    //объявляем объект SecretKeyFactory
    private SecretKeyFactory keyFactory;

    public SecureHasher(){
        try {
            keyFactory = SecretKeyFactory.getInstance(secureAlgorithm);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    /**
     * Метод генерирует "соль" - случайный байтовый массив размером capacity.
     * @return - случайный байтовый массив
     */
    @Override
    public byte[] generateSalt() {
        byte[] salt = new byte[capacity];
        //наполняем байтовый массив случайным набором байтов
        secureRandom.nextBytes(salt);
        return salt;
    }

    /**
     * Метод генерирует безопасный хэш с "солью" для заданной строки(например, пароля).
     * @param word - заданная строка(например, пароль)
     * @param salt - байтовый массив со случайным набором байт
     * @return - байтовый массив - безопасный хэш с "солью"
     */
    @Override
    public byte[] generateSecureHash(String word, byte[] salt) throws InvalidKeySpecException {
        //инициируем объект спецификации ключа
        KeySpec keySpec = new PBEKeySpec(word.toCharArray(), salt, iterationCount, keyLength);
        //генерируем массив случайных байтов
        return keyFactory.generateSecret(keySpec).getEncoded();
    }

    /**
     * Метод сравнивает заданную строку с заданным безопасным хэш.
     * @param word - заданная строка(например, пароль)
     * @param hash - заданный байтовый массив - безопасный хэш с "солью"
     * @param salt - заданный байтовый массив - "соль"
     * @return - результат сравнения
     */
    @Override
    public boolean compareSecureHashes(String word, byte[] hash, byte[] salt)throws InvalidKeySpecException {
        //инициируем объект спецификации ключа
        KeySpec keySpec = new PBEKeySpec(word.toCharArray(), salt, iterationCount, keyLength);
        //инициируем и наполняем контрольный байтовый массив
        byte[] checkHash = keyFactory.generateSecret(keySpec).getEncoded();
        //возвращаем результат сравнения байтовых массивов
        return Arrays.equals(checkHash, hash);
    }

}