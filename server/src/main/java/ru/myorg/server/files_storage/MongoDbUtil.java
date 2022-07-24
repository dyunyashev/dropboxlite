package ru.myorg.server.files_storage;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.GridFSDownloadStream;
import com.mongodb.client.gridfs.GridFSUploadStream;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.types.ObjectId;
import ru.myorg.server.config.ServerConfig;
import ru.myorg.server.protocol.UploadFile;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Сервис для работы с технологией GridFS в базе MongoDB
 */
public class MongoDbUtil implements FilesStorageService {
    private final ServerConfig config = ServerConfig.getInstance();

    private final MongoClient mongoClient;

    private final MongoDatabase database;

    private final GridFSBucket gridFSBucket;

    /**
     * Для каждого пользователя открывается поток для записи определенного файла в базу
     */
    private final ConcurrentHashMap<String, GridFSUploadStream> uploadStreams;

    /**
     * Для каждого пользователя открывается поток для чтения определенного файла из базы
     */
    private final ConcurrentHashMap<String, GridFSDownloadStream> downloadStreams;

    /**
     * В конструкторе инициализируется mongo-клиент для выполнения всех запросов к базе данных
     * Также идет подключение нашей базы и корзины, где будут храниться все файлы пользователей
     */
    public MongoDbUtil() {
        try {
            mongoClient = MongoClients.create(config.getMongodb_url());
            database = mongoClient.getDatabase("storagedb");
            gridFSBucket = GridFSBuckets.create(database, "userfiles");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        uploadStreams = new ConcurrentHashMap<>();
        downloadStreams = new ConcurrentHashMap<>();
    }

    /**
     * Метод открывает поток для записи файла в базу
     * @param fileName - имя файла
     * @param login - логин пользователя
     * @param byteRead - размер порции файла в базе
     */
    @Override
    public void openUploadStream(String fileName, String login, int byteRead) {
        //ищем новый файл на сервере, если он уже есть, то удаляем
        gridFSBucket.find(Filters.and(Filters.eq("filename", fileName),
                        Filters.eq("metadata.user", login)))
                        .forEach(gridFSFile -> gridFSBucket.delete(gridFSFile.getObjectId()));

        GridFSUploadOptions options = new GridFSUploadOptions()
                .chunkSizeBytes(byteRead)
                .metadata(new Document("user", login));

        uploadStreams.put(login, gridFSBucket.openUploadStream(fileName, options));
    }

    /**
     * Метод записывает очередную порцию файла в базу
     * @param bytes - данные файла
     * @param byteRead - размер порции файла в базе
     * @param login - логин пользователя
     */
    @Override
    public void write(byte[] bytes, int byteRead, String login) {
        GridFSUploadStream uploadStream = uploadStreams.get(login);
        if (uploadStream != null) {
            uploadStream.write(bytes, 0, byteRead);
            uploadStream.flush();
        }
    }

    /**
     * Метод закрывает открытый поток для записи файла в базу
     * @param login - логин пользователя
     */
    @Override
    public void closeUploadStream(String login) {
        GridFSUploadStream uploadStream = uploadStreams.get(login);
        if (uploadStream != null) uploadStream.close();
        uploadStreams.remove(login);
    }

    /**
     * Метод открывает поток чтения файла из базы данных
     * @param fileName - имя файла
     * @param login - логин пользователя
     */
    @Override
    public void openDownloadStream(String fileName, String login) {
        final ObjectId[] fileId = {null};
        gridFSBucket.find(Filters.and(Filters.eq("filename", fileName),
                        Filters.eq("metadata.user", login)))
                        .forEach(gridFSFile -> fileId[0] = gridFSFile.getObjectId());
        downloadStreams.put(login, gridFSBucket.openDownloadStream(fileId[0]));
    }

    /**
     * Метод открывает поток чтения файла из базы данных по указанному Id файла
     * @param fileId - id файла
     * @param login - логин пользователя
     */
    @Override
    public void openDownloadStreamShared(String fileId, String login) {
        downloadStreams.put(login, gridFSBucket.openDownloadStream(new ObjectId(fileId)));
    }

    /**
     * Метод читает из базы данных очередную порцию данных
     * @param start - размер в байтах, который уже прочитан
     * @param login - логин пользователя
     * @return - возвращает объект UploadFile, в котором хранится длина файла, размер считанной порции данных,
     * а также сами считанные данные в виде байтового массива
     */
    @Override
    public UploadFile read(int start, String login) {
        GridFSDownloadStream downloadStream = downloadStreams.get(login);
        UploadFile uploadFile = new UploadFile();
        if (downloadStream != null) {
            int lastLength = downloadStream.getGridFSFile().getChunkSize();
            int a = (int) (downloadStream.getGridFSFile().getLength() - start);
            if (a < lastLength) {
                lastLength = a;
            }
            byte[] data = new byte[lastLength];
            if (downloadStream.read(data) != -1 && (downloadStream.getGridFSFile().getLength() - start) > 0) {
                uploadFile.setFileLength(downloadStream.getGridFSFile().getLength());
                uploadFile.setByteRead(lastLength);
                uploadFile.setBytes(data);
                return uploadFile;
            }
        }
        return uploadFile;
    }

    /**
     * Метод закрывает поток чтения файла из базы данных
     * @param login - логин пользователя
     */
    @Override
    public void closeDownloadStream(String login) {
        GridFSDownloadStream downloadStream = downloadStreams.get(login);
        if (downloadStream != null) downloadStream.close();
        downloadStreams.remove(login);
    }

    /**
     * Метод получает из базы данных список файлов пользователя
     * @param login - логин пользователя
     * @return - возвращает список файлов пользователя в виде ArrayList
     */
    @Override
    public ArrayList<String> getFileList(String login) {
        ArrayList<String> files = new ArrayList<>();
        gridFSBucket.find(Filters.eq("metadata.user", login))
                .forEach(gridFSFile -> files.add(gridFSFile.getFilename()));
        return files;
    }

    /**
     * Метдо получает из базы данных список расшаренных файлов для указанного пользователя
     * @param login - логин пользователя
     * @return - возвращает список расшаренных файлов для пользователя в виде ArrayList
     */
    @Override
    public ArrayList<String> getFileListShared(String login) {
        ArrayList<String> files = new ArrayList<>();
        gridFSBucket.find(Filters.eq("metadata.shared", login))
                .forEach(gridFSFile -> files.add(gridFSFile.getFilename()+"#"+gridFSFile.getObjectId()));
        return files;
    }

    /**
     * Метод удаляет файл из базы данных
     * @param fileName - имя удаляемого файла
     * @param login - логин пользователя
     */
    @Override
    public void deleteFile(String fileName, String login) {
        gridFSBucket.find(Filters.and(Filters.eq("filename", fileName),
                        Filters.eq("metadata.user", login)))
                        .forEach(gridFSFile -> gridFSBucket.delete(gridFSFile.getObjectId()));
    }

    /**
     * Метод переименовывает файл в базе данных
     * @param fileOldName - текущее имя файла
     * @param fileName - новое имя файла
     * @param login - логин пользователя
     */
    @Override
    public void renameFile(String fileOldName, String fileName, String login) {
        gridFSBucket.find(Filters.and(Filters.eq("filename", fileOldName),
                        Filters.eq("metadata.user", login)))
                        .forEach(gridFSFile -> gridFSBucket.rename(gridFSFile.getObjectId(), fileName));
    }

    /**
     * Метод проверяет, модифицирован ли файл пользователем
     * @param fileName - имя файла
     * @param fileLength - длина файла
     * @param login - логин пользователя
     * @return - если файл модифицирован, возвращается true
     */
    @Override
    public boolean modifiedFile(String fileName, long fileLength, String login) {
        AtomicBoolean isModified = new AtomicBoolean(false);
        gridFSBucket.find(Filters.and(Filters.eq("filename", fileName),
                        Filters.eq("metadata.user", login)))
                        .forEach(gridFSFile -> isModified.set(fileLength != gridFSFile.getLength()));
        return isModified.get();
    }

    /**
     * Метод закрывает соединение с базой данных
     */
    public void close() {
        mongoClient.close();
    }

    /**
     * Помечает указанный файл нужного пользователя как расшаренный для другого пользователя
     * @param fileName - имя файла
     * @param login - логин пользователя, владельца файла
     * @param whom - кому пользователь расшаривает свой файл
     */
    @Override
    public void addSharedToFile(String fileName, String login, String whom) {
        gridFSBucket.find(Filters.and(Filters.eq("filename", fileName),
                        Filters.eq("metadata.user", login)))
                            .forEach(gridFSFile -> {
                                ArrayList<String> shareArray = (ArrayList<String>) gridFSFile.getMetadata().get("shared");
                                if (shareArray == null) shareArray = new ArrayList<>();

                                if (!shareArray.contains(whom)) {
                                    shareArray.add(whom);

                                    // обновляем метаданные
                                    MongoCollection<Document> collection = database.getCollection("userfiles.files");
                                    collection.updateOne(
                                            Filters.eq("_id", gridFSFile.getObjectId()),
                                            Updates.combine(Updates.set("metadata.shared", shareArray)));
                                }
                            });
    }
}
