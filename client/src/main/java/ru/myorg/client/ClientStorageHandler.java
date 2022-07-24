package ru.myorg.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import ru.myorg.client.commands_handler.CommandsHandlerService;
import ru.myorg.client.file_handler.FilesService;
import ru.myorg.client.controllers.ControllerGUI;
import ru.myorg.client.commands.*;
import ru.myorg.client.protocol.Message;
import ru.myorg.client.protocol.MessageStatus;
import ru.myorg.client.protocol.MessageType;
import ru.myorg.client.protocol.UploadFile;

import java.io.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Класс, в котором происходит взаимодействие с сервером
 */
public class ClientStorageHandler extends ChannelInboundHandlerAdapter {
    // Контроллер пользовательского интерфейса
    private final ControllerGUI controllerGUI;

    // Очередь команд, поступающих от пользователя и файловой системы
    private final ConcurrentLinkedQueue<ApplicationCommands> commandQueue;

    // Обработчик команд, поступающих от пользователей и файловой системы
    private final CommandsHandlerService commandsHandler;

    // Сервис для работы с файлами (загрузка/выгрузка файлов)
    private final FilesService filesService;

    public ClientStorageHandler(ControllerGUI controllerGUI, FilesService filesService, CommandsHandlerService commandsHandler) {
        this.controllerGUI = controllerGUI;
        this.commandQueue = controllerGUI.getCommandQueue();
        this.filesService = filesService;
        this.commandsHandler = commandsHandler;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.commandsHandler.setOptions(ctx, controllerGUI, filesService);
        Thread fshT = new Thread(commandsHandler);
        fshT.setDaemon(true);
        fshT.start();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // Отправка файла на сервер
        int start;
        Message message = (Message) msg;
        if (message.getType() == MessageType.FileUploadResponse) {
            if (message.getStatus() == MessageStatus.UPLOAD) {
                start = message.getFile().getStarPos();
                UploadFile uploadFile = message.getFile();
                if (start != -1) {
                    UploadFile uf = filesService.readingFile(uploadFile.getFile(), start, controllerGUI.getLogin());
                    if (uf.getByteRead() != -1) {
                        uploadFile.setByteRead(uf.getByteRead());
                        uploadFile.setStarPos(start);
                        uploadFile.setBytes(uf.getBytes());
                        uploadFile.setFileLength(uploadFile.getFile().length());
                        try {
                            Message messageRes = new Message(MessageType.FileUploadRequest, MessageStatus.UPLOAD);
                            messageRes.setFile(uploadFile);
                            messageRes.setLogin(controllerGUI.getLogin());
                            ctx.writeAndFlush(messageRes);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        System.out.println("Файл прочитан --------");
                    }
                }
            }
            else if (message.getStatus() == MessageStatus.OK) {
                filesService.closeFileReading(controllerGUI.getLogin());
                commandsHandler.setProcessing(false);
                controllerGUI.printEvent(String.format(">>Finish uploading file to server: %s",
                        message.getFile().getFileName()));

            }
            else if (message.getStatus() == MessageStatus.ERROR) {
                filesService.closeFileReading(controllerGUI.getLogin());
                commandsHandler.setProcessing(false);
                controllerGUI.printEvent(String.format("File %s was not uploading to server, error occurred: %s",
                        message.getFile().getFileName(),
                        message.getErrorMessage()));
            }
        }
        // Обработка ответа сервера на переименование файла на сервере
        else if (message.getType() == MessageType.FileRenameResponse) {
            if (message.getStatus() == MessageStatus.OK) {
                commandsHandler.setProcessing(false);
                controllerGUI.printEvent(String.format(">>File %s was successfully renamed to %s",
                                        message.getOldName(),
                                        message.getName()));
            }
            else if (message.getStatus() == MessageStatus.ERROR) {
                commandsHandler.setProcessing(false);
                controllerGUI.printEvent(String.format(">>File %s was not renamed, error occurred: %s",
                                        message.getOldName(),
                                        message.getErrorMessage()));
            }
        }
        // Обработка ответа сервера на удаление файла на сервере
        else if (message.getType() == MessageType.FileDeleteResponse) {
            commandsHandler.setProcessing(false);
            if (message.getStatus() == MessageStatus.OK) {
                controllerGUI.printEvent(String.format(">>File %s was successfully deleted on server",
                                        message.getName()));
            }
            else if (message.getStatus() == MessageStatus.ERROR) {
                controllerGUI.printEvent(String.format(">>File %s was not deleted, error occured: %s",
                                        message.getName(),
                                        message.getErrorMessage()));
            }
        }
        // Обработка ответа сервера на запрос списка файлов пользователя
        else if (message.getType() == MessageType.FileListResponse) {
            commandsHandler.setProcessing(false);
            if (message.getStatus() == MessageStatus.GET_FILES_LIST) {
                controllerGUI.setListFiles(message.getFiles());
            }
            else if (message.getStatus() == MessageStatus.GET_SHARED_FILES_LIST) {
                controllerGUI.setSharedFiles(message.getFiles());
            }
            else if (message.getStatus() == MessageStatus.ERROR) {
                controllerGUI.printEvent(String.format(">>File list was not geted, error occured: %s",
                        message.getErrorMessage()));
            }
        }
        // Чтение файла с сервера в каталог пользователя
        else if (message.getType() == MessageType.FileDownloadResponse) {
            if (message.getStatus() == MessageStatus.UPLOAD) {
                UploadFile ef = message.getFile();
                start = ef.getStarPos();
                byte[] bytes = ef.getBytes();
                int byteRead = ef.getByteRead();
                File file = ef.getFile();

                //в начале работы открываем файл
                if (start == 0) {
                    filesService.openFileWriting(file, controllerGUI.getLogin());
                }

                filesService.writingFile(start, bytes, controllerGUI.getLogin());

                start = start + byteRead;

                ef.setBytes(new byte[1]);
                ef.setStarPos(start);

                Message messageRes = new Message(MessageType.FileDownloadRequest, MessageStatus.DOWNLOAD);
                messageRes.setFile(ef);
                messageRes.setLogin(controllerGUI.getLogin());
                ctx.writeAndFlush(messageRes);
            }
            else if (message.getStatus() == MessageStatus.OK) {
                filesService.closeFileWriting(controllerGUI.getLogin());
                commandsHandler.setProcessing(false);
                controllerGUI.printEvent(String.format(">>File %s was successfully download",
                        message.getFile().getFileName()));
            }
            else if (message.getStatus() == MessageStatus.ERROR) {
                filesService.closeFileWriting(controllerGUI.getLogin());
                commandsHandler.setProcessing(false);
                controllerGUI.printEvent(String.format(">>File %s was not download, error occured: %s",
                                                message.getFile().getFileName(),
                                                message.getErrorMessage()));
                // обновим список файлов, каких-то файлов уже не существует
                commandQueue.add(new UserCommand(UserCommandStatus.LIST_OF_FILES_ON_SERVER));
                commandQueue.add(new UserCommand(UserCommandStatus.LIST_OF_SHARED_FILES_ON_SERVER));
            }
        }
        // Обработка ответа сервера на запрос на модификацию файла на сервере
        else if (message.getType() == MessageType.FileModifiedResponse) {
            commandsHandler.setProcessing(false);
            if (message.getStatus() == MessageStatus.OK) {
                controllerGUI.printEvent(String.format(">>File %s was successfully modified",
                        message.getFile().getFileName()));
            }
            else if (message.getStatus() == MessageStatus.UPLOAD) {
                FSCommand FSCommand = new FSCommand(FSCommandStatus.CREATED,
                                                message.getFile().getFile().getParent(),
                                                message.getFile().getFileName());
                commandQueue.add(FSCommand);
            }
            else if (message.getStatus() == MessageStatus.ERROR) {
                controllerGUI.printEvent(String.format(">>File %s was not modified, error occured: %s",
                        message.getFile().getFileName(),
                        message.getErrorMessage()));
            }
        }
        // Обработка ответа сервера на запрос на авторизацию пользователя
        else if (message.getType() == MessageType.ServerAuthResponse) {
            commandsHandler.setProcessing(false);
            if (message.getStatus() == MessageStatus.AUTH_OK) {
                controllerGUI.openMainWindow();
                commandQueue.add(new UserCommand(UserCommandStatus.GET_CATALOG));
                commandQueue.add(new UserCommand(UserCommandStatus.LIST_OF_FILES_ON_SERVER));
                commandQueue.add(new UserCommand(UserCommandStatus.LIST_OF_SHARED_FILES_ON_SERVER));
            }
            else if (message.getStatus() == MessageStatus.AUTH_ERROR) {
                controllerGUI.printAuthError(message.getErrorMessage());
            }
        }
        // Обработка ответа сервера на запрос каталога пользователя, с которым происходит синхронизация файлов
        else if (message.getType() == MessageType.ServerCatalogResponse) {
            commandsHandler.setProcessing(false);
            if (message.getStatus() == MessageStatus.GET_CATALOG) {
                String catalog = message.getCatalog();
                if (catalog == null || catalog.isEmpty()) {
                    controllerGUI.openDirectorySelection();
                }
                // каталог указан в настройках
                else {
                    controllerGUI.setCatalog(catalog);
                }
            }
            else if (message.getStatus() == MessageStatus.OK_SET_CATALOG) {
                //пока ничего не делаем в случае установки каталога в настройках
            }
            else if (message.getStatus() == MessageStatus.ERROR) {
                controllerGUI.printEvent(String.format(">>Catalog was not getted, error occured: %s",
                        message.getErrorMessage()));
            }
        }
        // Обработка ответа сервера на запрос расшаривания файла
        else if (message.getType() == MessageType.FileSharedResponse) {
            commandsHandler.setProcessing(false);
            if (message.getStatus() == MessageStatus.OK) {
                controllerGUI.printEvent(String.format(">>File %s was successfully shared",
                                            message.getFileName()));
            }
            else if (message.getStatus() == MessageStatus.ERROR) {
                controllerGUI.printEvent(String.format(">>File %s was not shared, error occured: %s",
                        message.getFileName(),
                        message.getErrorMessage()));
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
