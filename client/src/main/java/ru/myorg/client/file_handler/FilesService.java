package ru.myorg.client.file_handler;

import ru.myorg.client.protocol.UploadFile;

import java.io.File;

/**
 * Общее описание метдов для работы с файлами (загрузка/выгрузка)
 */
public interface FilesService {
    UploadFile getUploadFile(File file);

    void openFileWriting(File file, String login);

    void writingFile(int start, byte[] bytes, String login);

    void closeFileWriting(String login);

    UploadFile readingFile(File file, int start, String login);

    void closeFileReading(String login);
}
