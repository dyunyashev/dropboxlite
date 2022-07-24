package ru.myorg.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.json.JsonObjectDecoder;
import ru.myorg.client.commands_handler.CommandsHandler;
import ru.myorg.client.commands_handler.CommandsHandlerService;
import ru.myorg.client.config.ClientConfig;
import ru.myorg.client.file_handler.FilesHandler;
import ru.myorg.client.file_handler.FilesService;
import ru.myorg.client.controllers.ControllerGUI;
import ru.myorg.client.protocol.JacksonDecoder;
import ru.myorg.client.protocol.JacksonEncoder;

/**
 * Класс по запуску клиента Netty в режиме NIO
 */
public class ClientStorage {
    private final ClientConfig config = ClientConfig.getInstance();
    private EventLoopGroup group;
    public void connect(ControllerGUI controllerGUI) throws Exception {
        // Сервис по работе с файлами
        FilesService filesService = new FilesHandler();

        // Сервис по обработке команд
        CommandsHandlerService commandsHandlerService = new CommandsHandler();

        group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group).channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true).
                    handler(new ChannelInitializer<>() {
                        @Override
                        protected void initChannel(Channel ch) throws Exception {
                            ch.pipeline().addLast(new JacksonEncoder(),
                                                    new JsonObjectDecoder(Integer.MAX_VALUE),
                                                    new JacksonDecoder());
                            // подключаем наш обработчик команд
                            ch.pipeline().addLast(new ClientStorageHandler(controllerGUI, filesService, commandsHandlerService));
                        }
                    });
            ChannelFuture f = b.connect(config.getHost(), config.getPort()).sync();
            f.channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }
    }

    public void disconnect() {
        group.shutdownGracefully();
    }
}
