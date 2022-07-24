package ru.myorg.client.protocol;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Message {
    private MessageType type;
    private MessageStatus status;
    private String errorMessage;
    private String login;
    private String password;
    private String catalog;
    private String name;
    private String oldName;
    private String fileName;
    private String sharedUser;
    private ArrayList<String> files;
    private UploadFile file;

    public Message() {
    }

    public Message(MessageType type, MessageStatus status) {
        this.type = type;
        this.status = status;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public MessageStatus getStatus() {
        return status;
    }

    public void setStatus(MessageStatus status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getCatalog() {
        return catalog;
    }

    public void setCatalog(String catalog) {
        this.catalog = catalog;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOldName() {
        return oldName;
    }

    public void setOldName(String oldName) {
        this.oldName = oldName;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getSharedUser() {
        return sharedUser;
    }

    public void setSharedUser(String sharedUser) {
        this.sharedUser = sharedUser;
    }

    public ArrayList<String> getFiles() {
        return files;
    }

    public void setFiles(ArrayList<String> files) {
        this.files = files;
    }

    public UploadFile getFile() {
        return file;
    }

    public void setFile(UploadFile file) {
        this.file = file;
    }
}
