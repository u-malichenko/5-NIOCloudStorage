package com.malichenko.yury.netty.client;

import java.io.File;
import java.nio.file.Path;

public class FileInfo {

    private File file;
    private String name;
    private long size;
    //private LocalDateTime lastModified;

    /**
     * конструктор для создания объектов файлинфо в методе контроллера - refreshClientFilesTable()  .map(FileInfo::new)
     */
    public FileInfo(Path path) {
        //this.file = file;
        this.file = path.toFile();
        this.name = file.getName();
        this.size = file.length();
        //this.lastModified = LocalDateTime.ofInstant(Files.getLastModifiedTime(path).toInstant(), ZoneOffset.ofHours(3));
    }
    //TODO дата обновления

    /**
     * этим конструктором клиент добавляет файлинфо =строки в таблицу
     * FileInfo fileInfo = new FileInfo(filePath.toFile(), fileName, fileSize);
     * GUIHelper.serverFilesList.add(fileInfo);
     */
    public FileInfo(File file, String name, long size) {
        this.file = file;
        this.name = name;
        this.size = size;
    }

    public File getFile() {
        return file;
    }

    /**
     * нужен для сортировки файлов по имени в контроллере refreshServerFilesTable()
     */
    public String getName() {
        return name;
    }

    public long getSize() {
        return size;
    }



}
