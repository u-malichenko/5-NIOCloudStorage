package com.malichenko.yury.netty.client;

import java.io.File;

public class FileInfo {

    private File file;
    private String name;
    private long size;

    /**
     * конструктор для создания объектов файлинфо в методе контроллера - refreshClientFilesTable()  .map(FileInfo::new)
     * @param file
     */
    public FileInfo(File file) {
        this.file = file;
        this.name = file.getName();
        this.size = file.length();
    }

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
     * @return
     */
    public String getName() {
        return name;
    }

    public long getSize() {
        return size;
    }



}
