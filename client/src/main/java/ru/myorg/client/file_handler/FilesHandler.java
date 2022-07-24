package ru.myorg.client.file_handler;

import ru.myorg.client.config.ClientConfig;
import ru.myorg.client.protocol.UploadFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.ConcurrentHashMap;

public class FilesHandler implements FilesService {
    private final ClientConfig config = ClientConfig.getInstance();
    private final Integer bufferSize = config.getBufferSize();//16*1024*1024;

    ConcurrentHashMap<String, RandomAccessFile> userFiles;

    public FilesHandler() {
        this.userFiles = new ConcurrentHashMap<>();
    }

    /**
     * Получить параметры файла для загрузки на сервер
     * @param file - файл, который нужно загрузить на сервер
     * @return - в результате возвращается объект, содержащий следующие свойства:
     * имя файла, начальная позиция для загрузки, длина файла,
     * порция данных из файла и длина этой порции данных в байтах
     */
    @Override
    public UploadFile getUploadFile(File file) {
        UploadFile uploadFile = new UploadFile();
        uploadFile.setByteRead(-1);

        try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r")){
            int PARTS_TO_SPLIT = (int) Math.round((double) randomAccessFile.length() / bufferSize) + 1;
            int lastLength = (int) randomAccessFile.length() / PARTS_TO_SPLIT;
            byte[] bytes = new byte[lastLength];
            int byteRead;
            if ((byteRead = randomAccessFile.read(bytes)) != -1) {
                uploadFile.setFile(file);
                uploadFile.setFileName(file.getName());// имя файла
                uploadFile.setStarPos(0);// Начальная позиция файла
                uploadFile.setFileLength(file.length());
                uploadFile.setByteRead(byteRead);
                uploadFile.setBytes(bytes);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return uploadFile;
    }

    /**
     * Открытие файла на чтение
     * @param file - файл, который нужно прочитать
     * @param login - логин пользователя
     */
    @Override
    public void openFileWriting(File file, String login) {
        try {
            userFiles.put(login, new RandomAccessFile(file, "rw"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Записать порцию данных в файл
     * @param start - сколько байт уже записано в файл
     * @param bytes - порция данных в виде байтового массива
     * @param login - логин пользователя
     */
    @Override
    public void writingFile(int start, byte[] bytes, String login) {
        RandomAccessFile randomAccessFile = userFiles.get(login);
        if (randomAccessFile != null) {
            try {
                randomAccessFile.seek(start);
                randomAccessFile.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Закрытие файла, в который происходила запись
     * @param login - логин пользователя
     */
    @Override
    public void closeFileWriting(String login) {
        RandomAccessFile randomAccessFile = userFiles.get(login);
        if (randomAccessFile != null) {
            try {
                randomAccessFile.close();
                userFiles.remove(login);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Открытие файла на чтение данных
     * @param file - файл, который нужно прочитать
     * @param start - количество байт, которые нужно пропустить с начала файла
     * @param login - логин пользователя
     */
    @Override
    public UploadFile readingFile(File file, int start, String login) {
        UploadFile uf = new UploadFile();
        uf.setByteRead(-1);
        try {
            RandomAccessFile randomAccessFile = userFiles.get(login);
            if (randomAccessFile == null) {
                randomAccessFile = new RandomAccessFile(file, "r");
                userFiles.put(login, randomAccessFile);
            }

            randomAccessFile.seek(start);

            int PARTS_TO_SPLIT = (int) Math.round((double) randomAccessFile.length() / bufferSize) + 1;
            int lastLength = (int) randomAccessFile.length() / PARTS_TO_SPLIT;

            System.out.println("Длина блока:" + (randomAccessFile.length() / PARTS_TO_SPLIT));
            System.out.println("Длина:" + (randomAccessFile.length() - start));

            int a = (int) (randomAccessFile.length() - start);
            int b = (int) (randomAccessFile.length() / PARTS_TO_SPLIT);
            if (a < b) {
                lastLength = a;
            }

            byte[] bytes = new byte[lastLength];
            int byteRead;
            System.out.println("-----------------------------" + bytes.length);

            if ((byteRead = randomAccessFile.read(bytes)) != -1 && (randomAccessFile.length() - start) > 0) {
                System.out.println("длина байта:" + bytes.length);
                uf.setByteRead(byteRead);
                uf.setBytes(bytes);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return uf;
    }

    /**
     * Закрытие файла, чтение которого происходило
     * @param login - логин пользователя
     */
    @Override
    public void closeFileReading(String login) {
        RandomAccessFile randomAccessFile = userFiles.get(login);
        if (randomAccessFile != null) {
            try {
                randomAccessFile.close();
                userFiles.remove(login);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
