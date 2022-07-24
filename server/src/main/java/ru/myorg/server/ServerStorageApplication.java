package ru.myorg.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.json.JsonObjectDecoder;
import ru.myorg.server.authentication.DBCPDataSource;
import ru.myorg.server.config.ServerConfig;
import ru.myorg.server.files_storage.FilesStorageService;
import ru.myorg.server.files_storage.MongoDbUtil;
import ru.myorg.server.password_hasher.SecureHasher;
import ru.myorg.server.authentication.UsersAuthHandler;
import ru.myorg.server.authentication.UsersStorageService;
import ru.myorg.server.protocol.JacksonDecoder;
import ru.myorg.server.protocol.JacksonEncoder;

/**
 * Инициализация сервера Netty для работы в режиме NIO
 */
public class ServerStorageApplication {
    private final ServerConfig config = ServerConfig.getInstance();

    public void bind() throws Exception {
        // Подключаем сервис для работы с хранилищем файлов
        FilesStorageService filesStorageService = new MongoDbUtil();

        // Подключаем сервис для работы с хранилищем пользователей
        DBCPDataSource dbPool = DBCPDataSource.getInstance();
        UsersStorageService usersStorageService = new UsersAuthHandler(new SecureHasher(), dbPool);

        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, config.getSo_backlog())
                    .childHandler(new ChannelInitializer<>() {
                        @Override
                        protected void initChannel(Channel ch) {
                            ch.pipeline().addLast(new JacksonEncoder(),
                                                    new JsonObjectDecoder(Integer.MAX_VALUE),
                                                    new JacksonDecoder());
                            // Подключаем наш обработчик аутентификации пользователей
                            ch.pipeline().addLast(new ServerStorageAuthHandler(usersStorageService));
                            //  Подключаем наш обработчик для работы с базами данных (загрузка/выгрузка файлов)
                            ch.pipeline().addLast(new ServerStorageHandler(filesStorageService, usersStorageService));
                        }
                    });
            ChannelFuture f = b.bind(config.getPort()).sync();
            f.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();

            //закрываем сервисы
            filesStorageService.close();
            dbPool.close();
        }
    }

    public static void main(String[] args) {
        try {
            new ServerStorageApplication().bind();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}