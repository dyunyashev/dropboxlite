package ru.myorg.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import ru.myorg.server.authentication.VerificationStatus;
import ru.myorg.server.authentication.UsersStorageService;
import ru.myorg.server.authentication.VerificationResult;
import ru.myorg.server.protocol.Message;
import ru.myorg.server.protocol.MessageStatus;
import ru.myorg.server.protocol.MessageType;

public class ServerStorageAuthHandler extends ChannelInboundHandlerAdapter {
    private String login;

    // Сервис по аутентификации пользователей
    UsersStorageService usersStorageService;

    public ServerStorageAuthHandler(UsersStorageService usersStorageService) {
        this.usersStorageService = usersStorageService;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception{
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // если запрос на авторизацию, авторизуем/регистрируем пользователя
        Message message = (Message) msg;
        if (message.getType() == MessageType.ClientAuthRequest) {
            VerificationResult result = usersStorageService.authorizeUser(message.getLogin(),
                    message.getPassword(), ctx);
            if (result.getStatus() == VerificationStatus.ERROR) {
                Message messageRes = new Message(MessageType.ServerAuthResponse, MessageStatus.AUTH_ERROR);
                messageRes.setErrorMessage(result.getErrorMessage());
                ctx.writeAndFlush(messageRes);

            } else if (result.getStatus() == VerificationStatus.AUTHORIZED) {
                this.login = message.getLogin();
                Message messageRes = new Message(MessageType.ServerAuthResponse, MessageStatus.AUTH_OK);
                ctx.writeAndFlush(messageRes);

            }
        }
        // если другой запрос, то просто проверяем авторизован ли пользователь
        else {
            VerificationResult result = usersStorageService.isUserAuthorized(message.getLogin(), ctx);
            if (result.getStatus() != VerificationStatus.AUTHORIZED) {
                Message messageRes = new Message(MessageType.ServerAuthResponse, MessageStatus.AUTH_ERROR);
                messageRes.setErrorMessage(result.getErrorMessage());
                ctx.writeAndFlush(messageRes);
            }
            else {
                ctx.fireChannelRead(msg);
            }
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // нужно удалить пользователя из списка подключенных пользователей по ctx
        if (login != null) {
            usersStorageService.deAuthorizeUser(login);
        }
    }
}
