package ru.myorg.client.commands_handler;

import io.netty.channel.ChannelHandlerContext;
import ru.myorg.client.file_handler.FilesService;
import ru.myorg.client.controllers.ControllerGUI;

/**
 * Описание общих методов сервиса по обработке команд от файловой системы и пользовательских команд
 */
public interface CommandsHandlerService extends Runnable {
    void setProcessing(boolean processing);

    void setOptions(ChannelHandlerContext ctx, ControllerGUI controllerGUI, FilesService filesService);
}
