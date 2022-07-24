package ru.myorg.client.commands_handler;

import io.netty.channel.ChannelHandlerContext;
import net.contentobjects.jnotify.JNotify;
import net.contentobjects.jnotify.JNotifyException;
import net.contentobjects.jnotify.JNotifyListener;
import ru.myorg.client.file_handler.FilesService;
import ru.myorg.client.controllers.ControllerGUI;
import ru.myorg.client.commands.*;
import ru.myorg.client.protocol.Message;
import ru.myorg.client.protocol.MessageStatus;
import ru.myorg.client.protocol.MessageType;
import ru.myorg.client.protocol.UploadFile;

import java.io.File;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Обработчик команд, поступающих от файловой системы и пользовательских команд на работу с сервером
 */
public class CommandsHandler implements Runnable, CommandsHandlerService {
    private ControllerGUI controllerGUI;
    private ConcurrentLinkedQueue<ApplicationCommands> commandQueue;
    private ChannelHandlerContext ctx;
    private boolean processing = false;

    private FilesService filesService;

    private String TRACKED_DIRECTORY;

    public CommandsHandler() {
    }

    @Override
    public void run() {
        int mask = JNotify.FILE_CREATED |
                JNotify.FILE_DELETED |
                JNotify.FILE_MODIFIED |
                JNotify.FILE_RENAMED;

        //смотреть ли изменения во вложенных каталогах, не смотрим, сейчас вариант без использования подкаталогов
        boolean watchSubtree = false;

        int watchID = 0;

        boolean enableEventTracking = false;

        while (!Thread.currentThread().isInterrupted()) {
            if (commandQueue.isEmpty()) {
                if (!enableEventTracking) {
                    if (controllerGUI.getCatalog() != null && !controllerGUI.getCatalog().isEmpty()) {
                        TRACKED_DIRECTORY = controllerGUI.getCatalog();
                        try {
                            watchID = JNotify.addWatch(TRACKED_DIRECTORY, mask, watchSubtree, new FileSystemEventsListener());
                            enableEventTracking = true;
                        } catch (JNotifyException e) {
                            throw new RuntimeException(e);
                        }

                    }
                    else {
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
                else {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            else {
                if (!processing) {
                    ApplicationCommands applicationCommands = commandQueue.peek();
                    //обработка команд от файловой системы
                    if (applicationCommands.getClass() == FSCommand.class) {
                        FSCommand FSCommand = (FSCommand) applicationCommands;

                        // Обработка события добавления файла в каталог пользователя
                        if (FSCommand.getFileStatus() == FSCommandStatus.CREATED) {
                            File file = new File(FSCommand.getRootPath() + File.separator + FSCommand.getName());
                            if (file.exists() && file.isFile()) {
                                UploadFile uploadFile = filesService.getUploadFile(file);
                                if (uploadFile.getByteRead() != -1) {
                                    Message messageRes = new Message(MessageType.FileUploadRequest, MessageStatus.UPLOAD);
                                    messageRes.setFile(uploadFile);
                                    messageRes.setLogin(controllerGUI.getLogin());

                                    commandQueue.poll();//удаляем текущую команду
                                    processing = true;//признак запуска процесса
                                    controllerGUI.printEvent(String.format(">>Start uploading file to server: %s",
                                            FSCommand.getRootPath() + File.separator + FSCommand.getName()));

                                    ctx.writeAndFlush(messageRes);
                                } else {
                                    System.out.println("Файл прочитан");
                                }
                            }
                            else {
                                commandQueue.poll();
                            }
                        }
                        // Обработка события удаления файла в каталоге пользователя
                        else if (FSCommand.getFileStatus() == FSCommandStatus.DELETED) {
                            commandQueue.poll();//удаляем текущую команду
                            processing = true;//признак запуска процесса
                            controllerGUI.printEvent(String.format(">>Start deleting file on server: %s",
                                    FSCommand.getRootPath() + File.separator + FSCommand.getName()));

                            Message messageRes = new Message(MessageType.FileDeleteRequest, MessageStatus.DELETE);
                            messageRes.setName(FSCommand.getName());
                            messageRes.setLogin(controllerGUI.getLogin());
                            ctx.writeAndFlush(messageRes);
                        }
                        // Обработка события переименовая файла в каталоге пользователя
                        else if (FSCommand.getFileStatus() == FSCommandStatus.RENAMED) {
                            commandQueue.poll();//удаляем текущую команду
                            processing = true;//признак запуска процесса
                            controllerGUI.printEvent(String.format(">>Start renaming file %s to %s on server",
                                    FSCommand.getRootPath() + File.separator + FSCommand.getOldName(),
                                    FSCommand.getName()));

                            Message messageRes = new Message(MessageType.FileRenameRequest, MessageStatus.RENAME);
                            messageRes.setName(FSCommand.getName());
                            messageRes.setOldName(FSCommand.getOldName());
                            messageRes.setLogin(controllerGUI.getLogin());
                            ctx.writeAndFlush(messageRes);
                        }
                        // Обработка события по модификации файла в каталоге пользователя
                        else if (FSCommand.getFileStatus() == FSCommandStatus.MODIFIED) {
                            File file = new File(FSCommand.getRootPath() + File.separator + FSCommand.getName());
                            if (file.exists() && file.isFile()) {
                                    UploadFile uploadFile = new UploadFile();
                                    uploadFile.setFile(file);
                                    uploadFile.setFileName(FSCommand.getName());
                                    uploadFile.setFileLength(file.length());

                                    commandQueue.poll();//удаляем текущую команду
                                    processing = true;//признак запуска процесса
                                    controllerGUI.printEvent(String.format(">>Start modified file to server: %s",
                                            FSCommand.getRootPath() + File.separator + FSCommand.getName()));

                                    Message messageRes = new Message(MessageType.FileModifiedRequest, MessageStatus.MODIFIED);
                                    messageRes.setFile(uploadFile);
                                    messageRes.setLogin(controllerGUI.getLogin());
                                    ctx.writeAndFlush(messageRes);
                            }
                            else {
                                commandQueue.poll();
                            }

                        }
                        else {//если не известная команда - удаляем из очереди
                            commandQueue.poll();
                        }

                    }
                    //обработка команд от пользовательского интерфейса
                    else if (applicationCommands.getClass() == UserCommand.class) {
                        UserCommand userCommand = (UserCommand) applicationCommands;

                        // Обработка команды получения списка файлов пользователя на сервере
                        if (userCommand.getStatus() == UserCommandStatus.LIST_OF_FILES_ON_SERVER) {
                            commandQueue.poll();
                            processing = true;//признак запуска процесса
                            Message messageRes = new Message(MessageType.FileListRequest, MessageStatus.GET_FILES_LIST);
                            messageRes.setLogin(controllerGUI.getLogin());
                            ctx.writeAndFlush(messageRes);
                        }
                        // Обработка команды получения списка расшаренных файлов для пользователя на сервере
                        else if (userCommand.getStatus() == UserCommandStatus.LIST_OF_SHARED_FILES_ON_SERVER) {
                            commandQueue.poll();
                            processing = true;

                            Message messageRes = new Message(MessageType.FileListRequest, MessageStatus.GET_SHARED_FILES_LIST);
                            messageRes.setLogin(controllerGUI.getLogin());
                            ctx.writeAndFlush(messageRes);
                        }
                        // Обработка команды загрузки файла в каталог пользователя
                        else if (userCommand.getStatus() == UserCommandStatus.DOWNLOAD_FILE) {
                            commandQueue.poll();
                            processing = true;//признак запуска процесса
                            controllerGUI.printEvent(String.format(">>Start downloading file: %s", userCommand.getFileName()));

                            File downloadsDir = new File(TRACKED_DIRECTORY + File.separator + "downloads");
                            if (!downloadsDir.exists()) {
                                boolean makeDirs = downloadsDir.mkdirs();
                                if (!makeDirs) {
                                    throw new RuntimeException("failed to create a directory: " +
                                            TRACKED_DIRECTORY + File.separator + "downloads");
                                }
                            }

                            File file = new File(TRACKED_DIRECTORY + File.separator
                                                            + "downloads" + File.separator
                                                            + userCommand.getFileName());
                            UploadFile uploadFile = new UploadFile();
                            uploadFile.setFile(file);
                            uploadFile.setFileName(userCommand.getFileName());
                            uploadFile.setStarPos(0);

                            Message messageRes = new Message(MessageType.FileDownloadRequest, MessageStatus.DOWNLOAD);
                            messageRes.setFile(uploadFile);
                            messageRes.setLogin(controllerGUI.getLogin());
                            ctx.writeAndFlush(messageRes);

                        }
                        // Обработка команды загрузки расшаренных файлов в каталог пользователя
                        else if (userCommand.getStatus() == UserCommandStatus.DOWNLOAD_SHARED_FILE) {
                            commandQueue.poll();
                            processing = true;//признак запуска процесса
                            controllerGUI.printEvent(String.format(">>Start downloading file: %s", userCommand.getFileName()));

                            File downloadsDir = new File(TRACKED_DIRECTORY + File.separator + "downloads");
                            if (!downloadsDir.exists()) {
                                boolean makeDirs = downloadsDir.mkdirs();
                                if (!makeDirs) {
                                    throw new RuntimeException("failed to create a directory: " +
                                            TRACKED_DIRECTORY + File.separator + "downloads");
                                }

                            }

                            String[] fileParts = userCommand.getFileName().split("#");
                            File file = new File(TRACKED_DIRECTORY + File.separator
                                    + "downloads" + File.separator
                                    + fileParts[0]);
                            UploadFile uploadFile = new UploadFile();
                            uploadFile.setFile(file);
                            uploadFile.setFileName(fileParts[0]);
                            uploadFile.setFileId(fileParts[1]);
                            uploadFile.setStarPos(0);

                            Message messageRes = new Message(MessageType.FileDownloadRequest, MessageStatus.DOWNLOAD_SHARED);
                            messageRes.setFile(uploadFile);
                            messageRes.setLogin(controllerGUI.getLogin());
                            ctx.writeAndFlush(messageRes);

                        }
                        // Обработка команды авторизации на сервере
                        else if (userCommand.getStatus() == UserCommandStatus.AUTHORIZE) {
                            commandQueue.poll();
                            processing = true;//признак запуска процесса

                            Message messageRes = new Message(MessageType.ClientAuthRequest, MessageStatus.AUTHORIZATION);
                            messageRes.setLogin(userCommand.getLogin());
                            messageRes.setPassword(userCommand.getPassword());
                            ctx.writeAndFlush(messageRes);
                        }
                        // Обработка команды получения каталога пользователя из настроек на сервере
                        else if (userCommand.getStatus() == UserCommandStatus.GET_CATALOG) {
                            commandQueue.poll();
                            processing = true;//признак запуска процесса

                            Message messageRes = new Message(MessageType.ClientCatalogRequest, MessageStatus.GET_CATALOG);
                            messageRes.setLogin(controllerGUI.getLogin());
                            ctx.writeAndFlush(messageRes);
                        }
                        // Обработка команды по сохранению каталога пользователя в настройках на сервере
                        else if (userCommand.getStatus() == UserCommandStatus.SET_CATALOG) {
                            commandQueue.poll();
                            processing = true;//признак запуска процесса

                            Message messageRes = new Message(MessageType.ClientCatalogRequest, MessageStatus.SET_CATALOG);
                            messageRes.setCatalog(userCommand.getFileName());
                            messageRes.setLogin(controllerGUI.getLogin());
                            ctx.writeAndFlush(messageRes);
                        }
                        // Обработка команды по расшариванию файла для другого пользователя
                        else if (userCommand.getStatus() == UserCommandStatus.SHARE_FILE) {
                            commandQueue.poll();
                            processing = true;

                            Message messageRes = new Message(MessageType.FileSharedRequest, MessageStatus.SHARED_FILE);
                            messageRes.setFileName(userCommand.getFileName());
                            messageRes.setLogin(controllerGUI.getLogin());
                            messageRes.setSharedUser(userCommand.getLogin());
                            ctx.writeAndFlush(messageRes);
                        }
                        else {//если не известная команда - удаляем из очереди
                            commandQueue.poll();
                        }
                    }
                }
            }
        }

        if (enableEventTracking) {
            // to remove watch the watch
            try {
                JNotify.removeWatch(watchID);
            } catch (JNotifyException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Слушатель событий файловой системы в каталоге пользователя
     * создание файла
     * удаление файла
     * переименование файла
     * модификация файла
     */
    class FileSystemEventsListener implements JNotifyListener {
        @Override
        public void fileRenamed(int wd, String rootPath, String oldName,
                                String newName) {
            commandQueue.add(new FSCommand(FSCommandStatus.RENAMED, rootPath, newName, oldName));
            commandQueue.add(new UserCommand(UserCommandStatus.LIST_OF_FILES_ON_SERVER));
        }

        @Override
        public void fileModified(int wd, String rootPath, String name) {
            commandQueue.add(new FSCommand(FSCommandStatus.MODIFIED, rootPath, name));
        }

        @Override
        public void fileDeleted(int wd, String rootPath, String name) {
            commandQueue.add(new FSCommand(FSCommandStatus.DELETED, rootPath, name));
            commandQueue.add(new UserCommand(UserCommandStatus.LIST_OF_FILES_ON_SERVER));
        }

        @Override
        public void fileCreated(int wd, String rootPath, String name) {
            commandQueue.add(new FSCommand(FSCommandStatus.CREATED, rootPath, name));
            commandQueue.add(new UserCommand(UserCommandStatus.LIST_OF_FILES_ON_SERVER));
        }
    }

    @Override
    public void setProcessing(boolean processing) {
        this.processing = processing;
    }

    @Override
    public void setOptions(ChannelHandlerContext ctx, ControllerGUI controllerGUI, FilesService filesService) {
        this.commandQueue = controllerGUI.getCommandQueue();
        this.ctx = ctx;
        this.controllerGUI = controllerGUI;
        this.filesService = filesService;
    }
}
