package com.malichenko.yury.netty.server;

import com.malichenko.yury.netty.common.Command;
import com.malichenko.yury.netty.common.Sender;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * главный обработчик входящего потока конвейера сервера
 *  имеет состояния(пакеты протокола обмена данными):
 *  COMMAND - бездействуем ждем код
 *  NAME_LENGTH - получаем длину имени файла
 *  NAME - получаем имя файла
 *  FILE_LENGTH - получаем размер файл
 *  FILE - получаем файл
 */
public class MainHandler extends ChannelInboundHandlerAdapter {

    public enum State {
        COMMAND, NAME_LENGTH, NAME, FILE_LENGTH, FILE
    }

    private int fileNameLength; //длина имени файла
    private long fileLength; //длина файла
    private long receivedFileLength; //количество переданных байтов файла
    private byte command; //командный байт
    private Path filePath; //путь к файлу
    private final Path userPath; //путь к папке юзера
    private State currentState = State.COMMAND; //начальное состояние бездействие
    private BufferedOutputStream out; // поток для записи полученного файла

    public MainHandler(Path userPath) {
        this.userPath = userPath; //путь к директории пользователя
    }

    /**
     * метод ЧТЕНИЯ данных из канала
     * @param ctx -контекст передачи
     * @param msg - байты
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf buf = ((ByteBuf) msg); //то что пришло кастим и кладем в байтбуфер

        while (buf.readableBytes() > 0) { //работаем в цикле пока пока в буфере есть данные

            //обработка СОСТОЯНИЯ COMMAND:
            if (currentState == State.COMMAND) { //данные идут а состояние бездействие то
                command = buf.readByte(); //вычитываем из буфера байт в переменную command

                //обработка СОСТОЯНИЯ COMMAND и КОМАНДЫ SERVER_FILES_LIST_INFO:
                if (command == Command.SERVER_FILES_LIST_INFO.getByteValue()) { // если readed = запрос на список файлов
                    Sender.sendFilesList(userPath, ctx.channel(), Command.SERVER_FILES_LIST_INFO); //запускаем метод формирования и отправки списка файлов сервера

                //обработка СОСТОЯНИЯ COMMAND и КОМАНД TRANSFER_FILE + DELETE_FILE + DOWNLOAD_FILE:
                } else if (command == Command.TRANSFER_FILE.getByteValue() ||    //иначе если  прием файла или
                            command == Command.DELETE_FILE.getByteValue() ||     // удаление или
                        command == Command.DOWNLOAD_FILE.getByteValue()) {       // запрос на скачивание файла то
                    currentState = State.NAME_LENGTH; //меняем статус - ждем длину имени файла
                    receivedFileLength = 0L; //сбрасываем счетчик переданных байтов файла
                } else {
                    System.out.println("ERROR: Invalid first byte - " + command); //других сигналов не может быть - выдаем ошибку
                }
            }

            //обработка СОСТОЯНИЯ NAME_LENGTH:
            if (currentState == State.NAME_LENGTH) { //ЖДЁМ ДЛИНУ ИМЕНИ файла
                if (buf.readableBytes() >= 4) { // пока в буфер не придут все байты размера длинны имени файла - ждем данные в буфер
                    fileNameLength = buf.readInt(); //вычитываем из буфера байты длины имени передаваемого файла
                    System.out.println("STATE: Get filename length " + fileNameLength);
                    currentState = State.NAME; // длину имени получили меняем состояние на ожидание имени файла
                }
            }

            //обработка СОСТОЯНИЯ NAME
            if (currentState == State.NAME) { //ЖДЁМ ИМЯ ФАЙЛА

                if (buf.readableBytes() >= fileNameLength) { //количество байт в буфере = длине имени файла
                    byte[] fileName = new byte[fileNameLength]; //создаем байтовый массив дпо длине имени файла
                    buf.readBytes(fileName); //из буфера вычитываем байты имени файла в массив

                    getFilePath(userPath, fileName); //метод для склеивания полного пути из папки пользователя + имени файла

                    //обработка СОСТОЯНИЯ NAME и КОМАНДЫ TRANSFER_FILE:
                    if (command == Command.TRANSFER_FILE.getByteValue()) { //если команда = передача файла
                        deleteFileIfExist(filePath); //удаляем файл если такой уже есть
                        currentState = State.FILE_LENGTH; //меняем состояние на FILE_LENGTH - ожидаем длину файла

                    //обработка СОСТОЯНИЯ NAME и КОМАНД DELETE_FILE или DOWNLOAD_FILE:
                    } else if (!filePath.toFile().exists()) { //проверяем что файл не существует
                        Sender.sendCommand(ctx.channel(), Command.FILE_DOES_NOT_EXIST); // отправляем в ответ  ошибку-команду что файл не существует
                    } else {
                        currentState = State.COMMAND; //сбрасываем статус в IDLE(остальные команды не требуют дальнейшей обработки)

                        //обработка СОСТОЯНИЯ NAME>COMMAND и КОМАНДЫ DELETE_FILE:
                        if (command == Command.DELETE_FILE.getByteValue()) { // если команда удаление файла
                            if (filePath.toFile().delete()) { //удаляем файл, и если файл удалился без ошибок
                                Sender.sendFilesList(userPath, ctx.channel(), Command.SERVER_FILES_LIST_INFO); //то, запускаем метод формирования и отправки нового списка файлов сервера
                            } else {
                                Sender.sendCommand(ctx.channel(), Command.DELETE_FILE_ERR); //иначе отправляем колбэк-команду об ошибке удаления файла
                            }

                        //обработка СОСТОЯНИЯ NAME>COMMAND и КОМАНДЫ DOWNLOAD_FILE:
                        } else if (command == Command.DOWNLOAD_FILE.getByteValue()) { //если команда на запрос скачивания файла
                            Sender.sendFile(filePath, ctx.channel(), Command.TRANSFER_FILE, future -> { //то, запускаем метод отправляем файл: файл канал сигнал передачи и ждем фьюче
                                if (!future.isSuccess()) { //если файл не передали то печатаем ошибку
                                    future.cause().printStackTrace();
                                    Sender.sendCommand(ctx.channel(), Command.DOWNLOAD_FILE_ERR); //и отправляем колбек-команду ошибка закачивания файла
                                }
                            });
                        }
                    }
                }
            }

            //обработка СОСТОЯНИЯ FILE_LENGTH:
            if (currentState == State.FILE_LENGTH) { //текущее состояние = жду длину файла
                try {
                    if (buf.readableBytes() >= 8) { //количество байт в буфере = 8байтам
                        fileLength = buf.readLong(); //вычитываем из буфера байты длины файла
                        System.out.println("STATE: File length received - " + fileLength);
                        if (fileLength == 0) { //дина файла = 0
                            currentState = State.COMMAND; //меняем состояние на IDLE
                            Files.createFile(filePath); //создаем пустой файл
                        } else { //иначе создаем поток для создания файла:
                            out = new BufferedOutputStream(new FileOutputStream(filePath.toString()));
                            currentState = State.FILE; //меняем состояние на FILE
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            //обработка СОСТОЯНИЯ FILE:
            if (currentState == State.FILE) {
                try {
                    while (buf.readableBytes() > 0) { //пока буфер не пустой
                        out.write(buf.readByte()); //вычитываем данные из буфера в подготовленный поток к файлу
                        receivedFileLength++; //увеличиваем счетчик байтов файла
                        if (fileLength == receivedFileLength) { //если счетчик переданных байтов = длине файла
                            currentState = State.COMMAND; //меняем состояние на IDLE
                            out.close(); //закрываем поток создания файла
                            Sender.sendFilesList(userPath, ctx.channel(), Command.SERVER_FILES_LIST_INFO); //отправляем колбек-команду на обновление списка файлов сервера
                            break; //покидаем цикл собирания файла
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        if (buf.readableBytes() == 0) { //если буфер пустой
            buf.release(); //освобождаем буфер
        }
    }

    /**
     * метод удаления файла если он уже существует
     * @param filePath - путь к файлу
     */
    private void deleteFileIfExist(Path filePath) {
        try {
            if (Files.exists(filePath)) { // если файл есть
                Files.delete(filePath); //удаляем этот файл
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * метод получения полного пути(с именем файла) папка пользователя + имя файла
     * @param userPath - путь к файлу
     * @param fileName - имя файла
     */
    private void getFilePath(Path userPath, byte[] fileName) {
        try {
            System.out.println("STATE: Filename received - " + new String(fileName, "UTF-8"));
            filePath = userPath.resolve(new String(fileName, "UTF-8")); //создаем путь из папки пользователя и имени файла
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
