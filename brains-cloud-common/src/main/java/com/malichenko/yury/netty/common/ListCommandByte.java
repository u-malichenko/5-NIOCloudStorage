package com.malichenko.yury.netty.common;

public enum ListCommandByte {

    AUTH_OK(10),
    AUTH_ERR(11),

    TRANSFER_FILE(20),
    TRANSFER_FILE_ERR(21),

    REQUEST_FILE(30),
    REQUEST_FILE_ERR(31),

    DELETE_FILE(40),
    DELETE_FILE_ERR(41),

    FILE_DOES_NOT_EXIST(51),

    SERVER_FILES_LIST_INFO(60);

    final int value;

    ListCommandByte(int value) {
        this.value = value;
    }

    public byte getByteValue() {
        return (byte) value;
    }
}
