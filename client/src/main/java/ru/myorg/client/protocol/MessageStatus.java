package ru.myorg.client.protocol;

public enum MessageStatus {
    OK,
    UPLOAD,
    ERROR,
    AUTH_OK,
    AUTH_ERROR,
    GET_CATALOG,
    OK_SET_CATALOG,
    GET_FILES_LIST,
    GET_SHARED_FILES_LIST,
    DOWNLOAD,
    DOWNLOAD_SHARED,
    AUTHORIZATION,
    SET_CATALOG,
    SHARED_FILE,
    DELETE,
    RENAME,
    MODIFIED
}
