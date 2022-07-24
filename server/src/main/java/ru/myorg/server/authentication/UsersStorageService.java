package ru.myorg.server.authentication;

import io.netty.channel.ChannelHandlerContext;

/**
 * Интерфейс позволяет авторизовать пользователей в приложении
 */
public interface UsersStorageService {
    void deAuthorizeUser(String login);

    VerificationResult isUserAuthorized(String login, ChannelHandlerContext ctx);

    VerificationResult authorizeUser(String login, String password, ChannelHandlerContext ctx);

    String getCatalog(String login);

    void setCatalog(String catalog, String login);
}
