package ru.myorg.client.controllers;

import ru.myorg.client.commands.ApplicationCommands;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

public interface ControllerGUI {
    void printEvent(String msg);

    ConcurrentLinkedQueue<ApplicationCommands> getCommandQueue();

    void setListFiles(ArrayList<String> files);

    void setSharedFiles(ArrayList<String> files);

    void printAuthError(String msg);

    void openMainWindow();

    String getLogin();

    String getCatalog();

    void setCatalog(String catalog);

    void openDirectorySelection();
}
