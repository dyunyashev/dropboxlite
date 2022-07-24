package ru.myorg.server.files_storage;

import ru.myorg.server.protocol.UploadFile;

import java.util.ArrayList;

/**
 * Интерфейс позволяет работать с различными хранилищами файлов
 */
public interface FilesStorageService {
    ArrayList<String> getFileList(String login);

    ArrayList<String> getFileListShared(String login);

    void deleteFile(String fileName, String login);

    void renameFile(String fileOldName, String fileName, String login);

    boolean modifiedFile(String fileName, long fileLength, String login);

    void openUploadStream(String fileName, String login, int byteRead);

    void write(byte[] bytes, int byteRead, String login);

    void closeUploadStream(String login);

    void openDownloadStream(String fileName, String login);

    UploadFile read(int start, String login);

    void closeDownloadStream(String login);

    void close();

    void addSharedToFile(String fileName, String login, String whom);

    void openDownloadStreamShared(String fileId, String login);
}
