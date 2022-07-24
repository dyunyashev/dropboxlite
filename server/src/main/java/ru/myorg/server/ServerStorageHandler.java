package ru.myorg.server;

import com.mongodb.MongoGridFSException;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import ru.myorg.server.authentication.UsersStorageService;
import ru.myorg.server.files_storage.FilesStorageService;
import ru.myorg.server.protocol.Message;
import ru.myorg.server.protocol.MessageStatus;
import ru.myorg.server.protocol.MessageType;
import ru.myorg.server.protocol.UploadFile;

import java.awt.*;

public class ServerStorageHandler extends ChannelInboundHandlerAdapter {

    // Сервис по работе с хранилищем файлов
    private final FilesStorageService filesStorageService;

    // Сервис по работе с хранилищем настроек пользователей
    private final UsersStorageService usersStorageService;

    public ServerStorageHandler(FilesStorageService filesStorageService, UsersStorageService usersStorageService) {
        this.filesStorageService = filesStorageService;
        this.usersStorageService = usersStorageService;
    }

    /**
     * Основной обработчик команд от клиентов
     * @param ctx - текущее соединение с клиентом
     * @param msg - объект, полученный от клиента
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // Обработка запроса по загрузке файла на сервер
        int start;
        Message message = (Message) msg;
        if (message.getType() == MessageType.FileUploadRequest) {
            UploadFile ef = message.getFile();
            byte[] bytes = ef.getBytes();
            int byteRead = ef.getByteRead();
            start = ef.getStarPos();
            String fileName = ef.getFileName();//имя файла

            if (start == 0) {//задаем настройки
                try {
                    filesStorageService.openUploadStream(fileName, message.getLogin(), byteRead);
                }
                catch (MongoGridFSException e) {
                    UploadFile uf = new UploadFile();
                    uf.setFileName(fileName);

                    Message messageRes = new Message(MessageType.FileUploadResponse, MessageStatus.ERROR);
                    messageRes.setFile(uf);
                    messageRes.setErrorMessage(e.getMessage());
                    ctx.writeAndFlush(messageRes);
                    return;
                }
            }

            try {
                filesStorageService.write(bytes, byteRead, message.getLogin());
                start = start + byteRead;

                if (byteRead > 0) {
                    UploadFile answerFile = new UploadFile();
                    answerFile.setFile(ef.getFile());
                    answerFile.setStarPos(start);
                    answerFile.setFileName(ef.getFileName());

                    Message messageRes = new Message(MessageType.FileUploadResponse, MessageStatus.UPLOAD);
                    messageRes.setFile(answerFile);
                    ctx.writeAndFlush(messageRes);
                }

                if (ef.getFileLength() == start) {
                    filesStorageService.closeUploadStream(message.getLogin());

                    UploadFile answerFile = new UploadFile();
                    answerFile.setFileName(ef.getFileName());

                    Message messageRes = new Message(MessageType.FileUploadResponse, MessageStatus.OK);
                    messageRes.setFile(answerFile);
                    ctx.writeAndFlush(messageRes);
                }
            }
            catch (MongoGridFSException e) {
                UploadFile uf = new UploadFile();
                uf.setFileName(fileName);

                Message messageRes = new Message(MessageType.FileUploadResponse, MessageStatus.ERROR);
                messageRes.setFile(uf);
                messageRes.setErrorMessage(e.getMessage());

                ctx.writeAndFlush(messageRes);
            }

        }
        // Обработка запроса по переименованию файла на сервере
        else if (message.getType() == MessageType.FileRenameRequest) {
            try {
                filesStorageService.renameFile(message.getOldName(),
                        message.getName(),
                        message.getLogin());

                Message messageRes = new Message(MessageType.FileRenameResponse, MessageStatus.OK);
                messageRes.setName(message.getName());
                messageRes.setOldName(message.getOldName());

                ctx.writeAndFlush(messageRes);
            }
            catch (MongoGridFSException e) {
                Message messageRes = new Message(MessageType.FileRenameResponse, MessageStatus.ERROR);
                messageRes.setName(message.getName());
                messageRes.setOldName(message.getOldName());
                messageRes.setErrorMessage(e.getMessage());

                ctx.writeAndFlush(messageRes);
            }
        }
        // Обработка запроса по удалению файла на сервере
        else if (message.getType() == MessageType.FileDeleteRequest) {
            try {
                filesStorageService.deleteFile(message.getName(), message.getLogin());

                Message messageRes = new Message(MessageType.FileDeleteResponse, MessageStatus.OK);
                messageRes.setName(message.getName());

                ctx.writeAndFlush(messageRes);
            }
            catch (MongoGridFSException e) {
                Message messageRes = new Message(MessageType.FileDeleteResponse, MessageStatus.ERROR);
                messageRes.setName(message.getName());
                messageRes.setErrorMessage(e.getMessage());

                ctx.writeAndFlush(messageRes);
            }
        }
        // Обработка запроса по получению списка файлов пользователя
        else if (message.getType() == MessageType.FileListRequest) {
            try {
                if (message.getStatus() == MessageStatus.GET_FILES_LIST) {
                    Message messageRes = new Message(MessageType.FileListResponse, MessageStatus.GET_FILES_LIST);
                    messageRes.setFiles(filesStorageService.getFileList(message.getLogin()));

                    ctx.writeAndFlush(messageRes);

                } else if (message.getStatus() == MessageStatus.GET_SHARED_FILES_LIST) {
                    Message messageRes = new Message(MessageType.FileListResponse, MessageStatus.GET_SHARED_FILES_LIST);
                    messageRes.setFiles(filesStorageService.getFileListShared(message.getLogin()));

                    ctx.writeAndFlush(messageRes);
                }
            }
            catch (MongoGridFSException e) {
                Message messageRes = new Message(MessageType.FileListResponse, MessageStatus.ERROR);
                messageRes.setErrorMessage(e.getMessage());

                ctx.writeAndFlush(messageRes);
            }
        }
        // Обработка запроса по выгрузке файла из базы данных на диск клиента
        else if (message.getType() == MessageType.FileDownloadRequest) {
            UploadFile ef = message.getFile();
            start = ef.getStarPos();
            String fileName = ef.getFileName();

            if (start == 0) {//задаем настройки
                if (message.getStatus() == MessageStatus.DOWNLOAD) {
                    try {
                        filesStorageService.openDownloadStream(fileName, message.getLogin());
                    }
                    catch (MongoGridFSException e) {
                        UploadFile uf = new UploadFile();
                        uf.setFileName(fileName);

                        Message messageRes = new Message(MessageType.FileDownloadResponse, MessageStatus.ERROR);
                        messageRes.setFile(uf);
                        messageRes.setErrorMessage(e.getMessage());

                        ctx.writeAndFlush(messageRes);
                        return;
                    }
                }
                else if (message.getStatus() == MessageStatus.DOWNLOAD_SHARED) {
                    try {
                        filesStorageService.openDownloadStreamShared(message.getFile().getFileId(),
                                message.getLogin());
                    }
                    catch (MongoGridFSException e) {
                        UploadFile uf = new UploadFile();
                        uf.setFileName(fileName);

                        Message messageRes = new Message(MessageType.FileDownloadResponse, MessageStatus.ERROR);
                        messageRes.setFile(uf);
                        messageRes.setErrorMessage(e.getMessage());

                        ctx.writeAndFlush(messageRes);
                        return;
                    }
                }
            }

            try {
                UploadFile uf = filesStorageService.read(start, message.getLogin());
                if (uf.getByteRead() != 0) {
                    ef.setStarPos(start);
                    ef.setFileLength(uf.getFileLength());
                    ef.setByteRead(uf.getByteRead());
                    ef.setBytes(uf.getBytes());

                    Message messageRes = new Message(MessageType.FileDownloadResponse, MessageStatus.UPLOAD);
                    messageRes.setFile(ef);

                    ctx.writeAndFlush(messageRes);

                } else {//файл прочитан
                    filesStorageService.closeDownloadStream(message.getLogin());
                    UploadFile answerFile = new UploadFile();
                    answerFile.setFileName(fileName);

                    Message messageRes = new Message(MessageType.FileDownloadResponse, MessageStatus.OK);
                    messageRes.setFile(answerFile);

                    ctx.writeAndFlush(messageRes);
                }
            }
            catch (MongoGridFSException e) {
                UploadFile uf = new UploadFile();
                uf.setFileName(fileName);

                Message messageRes = new Message(MessageType.FileDownloadResponse, MessageStatus.ERROR);
                messageRes.setFile(uf);
                messageRes.setErrorMessage(e.getMessage());

                ctx.writeAndFlush(messageRes);
            }
        }
        // Обработка запроса по модификации файла в базе данных
        else if (message.getType() == MessageType.FileModifiedRequest) {
            try {
                boolean isModified = filesStorageService.modifiedFile(message.getFile().getFileName(),
                        message.getFile().getFileLength(),
                        message.getLogin());
                if (!isModified) {
                    Message messageRes = new Message(MessageType.FileModifiedResponse, MessageStatus.OK);
                    messageRes.setFile(message.getFile());

                    ctx.writeAndFlush(messageRes);

                } else {
                    Message messageRes = new Message(MessageType.FileModifiedResponse, MessageStatus.UPLOAD);
                    messageRes.setFile(message.getFile());

                    ctx.writeAndFlush(messageRes);
                }
            }
            catch (MongoGridFSException e) {
                UploadFile uf = new UploadFile();
                uf.setFileName(message.getFile().getFileName());

                Message messageRes = new Message(MessageType.FileModifiedResponse, MessageStatus.ERROR);
                messageRes.setFile(uf);
                messageRes.setErrorMessage(e.getMessage());

                ctx.writeAndFlush(messageRes);
            }
        }
        // Обработка запроса на получение / сохранения каталога пользователя, с которым синхронизируются файлы на сервере
        else if (message.getType() == MessageType.ClientCatalogRequest) {
            try {
                if (message.getStatus() == MessageStatus.GET_CATALOG) {
                    Message messageRes = new Message(MessageType.ServerCatalogResponse, MessageStatus.GET_CATALOG);
                    messageRes.setCatalog(usersStorageService.getCatalog(message.getLogin()));

                    ctx.writeAndFlush(messageRes);

                } else if (message.getStatus() == MessageStatus.SET_CATALOG) {
                    usersStorageService.setCatalog(message.getCatalog(), message.getLogin());

                    Message messageRes = new Message(MessageType.ServerCatalogResponse, MessageStatus.OK_SET_CATALOG);

                    ctx.writeAndFlush(messageRes);
                }
            }
            catch (MongoGridFSException e) {
                Message messageRes = new Message(MessageType.ServerCatalogResponse, MessageStatus.ERROR);
                messageRes.setErrorMessage(e.getMessage());

                ctx.writeAndFlush(messageRes);
            }
        }
        // Обработка запроса на расшаривание каталога
        else if (message.getType() == MessageType.FileSharedRequest) {
            if (message.getStatus() == MessageStatus.SHARED_FILE) {
                try {
                    filesStorageService.addSharedToFile(message.getFileName(),
                            message.getLogin(),
                            message.getSharedUser());

                    Message messageRes = new Message(MessageType.FileSharedResponse, MessageStatus.OK);
                    messageRes.setFileName(message.getFileName());

                    ctx.writeAndFlush(messageRes);
                }
                catch (MongoGridFSException e) {
                    Message messageRes = new Message(MessageType.FileSharedResponse, MessageStatus.ERROR);
                    messageRes.setFileName(message.getFileName());
                    messageRes.setErrorMessage(e.getMessage());

                    ctx.writeAndFlush(messageRes);
                }
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
