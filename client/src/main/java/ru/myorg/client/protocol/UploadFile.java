package ru.myorg.client.protocol;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.File;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UploadFile {
    private File file;// файл
    private String fileName;// имя файла
    private int starPos;// начальная позиция
    private byte[] bytes;// байтовый массив файла
    private int byteRead;// Конечная позиция
    private long fileLength;// Длина файла
    private String fileId;// id файла на сервере

    public UploadFile() {
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public int getStarPos() {
        return starPos;
    }

    public void setStarPos(int starPos) {
        this.starPos = starPos;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    public int getByteRead() {
        return byteRead;
    }

    public void setByteRead(int byteRead) {
        this.byteRead = byteRead;
    }

    public long getFileLength() {
        return fileLength;
    }

    public void setFileLength(long fileLength) {
        this.fileLength = fileLength;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }
}
