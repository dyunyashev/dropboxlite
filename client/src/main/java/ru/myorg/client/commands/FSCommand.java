package ru.myorg.client.commands;

import java.io.Serializable;

/**
 * Команды от файловой системы, которая следит за каталогом пользователя
 * на создание файла, удаление файла, переименование файла, модификацию файла
 */
public final class FSCommand extends ApplicationCommands implements Serializable {
    private final FSCommandStatus FSCommandStatus;
    private final String rootPath;
    private final String name;
    private String oldName;

    public FSCommand(FSCommandStatus FSCommandStatus, String rootPath, String name) {
        this.FSCommandStatus = FSCommandStatus;
        this.rootPath = rootPath;
        this.name = name;
    }

    public FSCommand(FSCommandStatus FSCommandStatus, String rootPath, String name, String oldName) {
        this.FSCommandStatus = FSCommandStatus;
        this.rootPath = rootPath;
        this.name = name;
        this.oldName = oldName;
    }

    public FSCommandStatus getFileStatus() {
        return FSCommandStatus;
    }

    public String getRootPath() {
        return rootPath;
    }

    public String getName() {
        return name;
    }

    public String getOldName() {
        return oldName;
    }

    @Override
    public String toString() {
        return "FileCommand{" +
                "fileStatus=" + FSCommandStatus +
                ", rootPath='" + rootPath + '\'' +
                ", name='" + name + '\'' +
                ", oldName='" + oldName + '\'' +
                '}';
    }
}
