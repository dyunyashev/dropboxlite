package ru.myorg.server.protocol;

public enum MessageType {
    // Запросы от клиента
    ClientAuthRequest,
    ClientCatalogRequest,
    FileDeleteRequest,
    FileDownloadRequest,
    FileListRequest,
    FileModifiedRequest,
    FileRenameRequest,
    FileSharedRequest,
    FileUploadRequest,

    // Ответы от сервера
    FileDeleteResponse,
    FileDownloadResponse,
    FileListResponse,
    FileModifiedResponse,
    FileRenameResponse,
    FileSharedResponse,
    FileUploadResponse,
    ServerAuthResponse,
    ServerCatalogResponse
}
