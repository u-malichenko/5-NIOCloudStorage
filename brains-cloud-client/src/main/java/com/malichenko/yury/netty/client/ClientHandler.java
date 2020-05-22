package com.malichenko.yury.netty.client;

import com.malichenko.yury.netty.common.Command;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * главный обработчик входящего потока конвейера лкиента
 * имеет состояния(пакеты протокола обмена данными):
 * для чтения файлов:
 *  COMMAND - бездействуем ждем код команды
 *  NAME_LENGTH - получаем длину имени файла
 *  NAME - получаем имя файла
 *  FILE_LENGTH - получаем размер файл
 *  FILE - получаем файл
 * для чтения списка файлов:
 *  LIST_SIZE - размер списка файлов(строк в таблице)
 *  NAME_LENGTH_LIST - длинна имени файла в строке листа
 *  NAME_LIST - имя файла в листе
 *  FILE_LENGTH_LIST - размер файла в листе
 */
public class ClientHandler extends ChannelInboundHandlerAdapter {
    public enum State {
        COMMAND, NAME_LENGTH, NAME, FILE_LENGTH, FILE,                 // для чтения файлов
        LIST_SIZE, NAME_LENGTH_LIST, NAME_LIST, FILE_LENGTH_LIST    // для чтения списка файлов
    }

    private boolean fileReading = false; //читаем файл
    private boolean fileListReading = false; //читаем список файлов
    private int listSize; //размер списка файлов уменьшаем его вычитывая пофайлово весь список
    private int nameSize; //размер имени файла
    private long fileLength; //длинна файла
    private long receivedFileLength; //количество переданных байтов файла
    private String fileName; //имя файла
    private String userDirectory; //имя пользовательской директории

    private List<Callback> callbackList; //список колбеков
    private BufferedOutputStream out; //поток для записи файла в директорию
    private State currentState = State.COMMAND; //начальное состояние бездействие
    private Path filePath; //путь к файлу

    public ClientHandler(List<Callback> callbackList) {
        this.callbackList = callbackList; //список колбеков
    }

    /**
     * метод ЧТЕНИЯ данных из канала
     * @param ctx -контекст передачи
     * @param msg - байты
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf buf = ((ByteBuf) msg);//то что пришло кастим и кладем в байтбуфер

        while (buf.readableBytes() > 0) { //работаем в цикле пока в буфере есть данные

            //обработка СОСТОЯНИЯ COMMAND:
            if (currentState == State.COMMAND) { //данные идут а состояние ожидание COMMAND то
                byte command = buf.readByte(); //вычитываем из буфера байт в переменную command
                readCommand(command); //метод проверки Кода протокола
            }

            //СТАТУС = fileReading
            if (fileReading) { //обрабатываем получение файла
                readFile(buf); // запускаем метод получения файла
            }

            //СТАТУС = fileListReading
            if (fileListReading) { //обрабатываем получение списка файлов

                //обработка СОСТОЯНИЯ LIST_SIZE:
                if (currentState == State.LIST_SIZE) { //состояние ожидания размера списка файлов(итераций получений строк в лист)
                    if (buf.readableBytes() >= 4) { //читаем 4 байта из буфера
                        listSize = buf.readInt(); //вычитываем из буфера длину списка в переменную listSize далее ее убавляем
                        currentState = State.NAME_LENGTH_LIST; //меняем состояние на NAME_LENGTH_LIST
                    }
                }
                //обработка СОСТОЯНИЯ NAME_LENGTH_LIST:
                if (currentState != State.LIST_SIZE && listSize > 0) { //если мы уже знаем размер списка и его длинна больше 0
                    readServerFilesList(buf); //запускаем метод получения списка файлов в листе
                }
            }
        }
        if (buf.readableBytes() == 0) { //если буфер пустой
            buf.release(); //освобождаем буфер
        }
    }

    /**
     * метод проверки какая команда пришла на конвейер
     * @param command первый байт из принятого пакета
     */
    private void readCommand(byte command) {

        //обработка СОСТОЯНИЯ COMMAND и КОМАНДЫ AUTH_OK:
        if (command == Command.AUTH_OK.getByteValue()) { //получен колбек-команда об успешной авторизации
            System.out.println("Добро пожаловать!");
            userDirectory = GUIHelper.userDirectory; //получаем директорию пользователя
            callbackList.get(0).callback(); //вызываем обработку метода колбека - показать интерфейс this::showStoragePanel
        }

        //обработка СОСТОЯНИЯ COMMAND и КОМАНДЫ AUTH_ERR:
        else if (command == Command.AUTH_ERR.getByteValue()) { //получили в ответ команду-колбек ошибка авторизации
            GUIHelper.showError(new RuntimeException("Неверный логин или пароль")); //генерируем ошибку и отправляем ее  метод обработки ошибок
        }

        //обработка СОСТОЯНИЯ COMMAND и КОМАНДЫ SERVER_FILES_LIST_INFO:
        else if (command == Command.SERVER_FILES_LIST_INFO.getByteValue()) {
            GUIHelper.serverFilesList.clear(); //отчистить список файлов
            currentState = State.LIST_SIZE; //меняем состояние на LIST_SIZE
            fileListReading = true; //меняем СТАТУС на fileListReading
        }

        //обработка СОСТОЯНИЯ COMMAND и КОМАНДЫ TRANSFER_FILE:
        else if (command == Command.TRANSFER_FILE.getByteValue()) {
            currentState = State.NAME_LENGTH; //меняем состояние на NAME_LENGTH
            fileReading = true; //меняем СТАТУС на fileReading
            receivedFileLength = 0L; // количество принятых байтов сбрасываем в ноль
        }

        //обработка СОСТОЯНИЯ COMMAND и КОМАНДЫ FILE_DOES_NOT_EXIST:
        else if (command == Command.FILE_DOES_NOT_EXIST.getByteValue()) {
            GUIHelper.showError(new RuntimeException("Данный файл не существует")); //генерируем ошибку и отправляем ее  метод обработки ошибок
        }

        //обработка СОСТОЯНИЯ COMMAND и КОМАНДЫ TRANSFER_FILE_ERR:
        else if (command == Command.TRANSFER_FILE_ERR.getByteValue()) {
            GUIHelper.showError(new RuntimeException("Не удалось отправить файл")); //генерируем ошибку и отправляем ее  метод обработки ошибок
        }

        //обработка СОСТОЯНИЯ COMMAND и КОМАНДЫ DELETE_FILE_ERR:
        else if (command == Command.DELETE_FILE_ERR.getByteValue()) {
            GUIHelper.showError(new RuntimeException("Не удалось удалить файл")); //генерируем ошибку и отправляем ее  метод обработки ошибок
        }

        //обработка СОСТОЯНИЯ COMMAND и КОМАНДЫ DOWNLOAD_FILE_ERR:
        else if (command == Command.DOWNLOAD_FILE_ERR.getByteValue()) {
            GUIHelper.showError(new RuntimeException("Не удалось скачать файл")); //генерируем ошибку и отправляем ее  метод обработки ошибок
        } else {
            System.out.println("ERROR: Invalid first byte - " + command); //иных случаев быть не должно
        }
    }

    /**
     * метод получения списка файлов в листе
     * @param buf байты из входящего потока протокола
     */
    private void readServerFilesList(ByteBuf buf) {

        //обработка СОСТОЯНИЯ NAME_LENGTH_LIST:
        if (currentState == State.NAME_LENGTH_LIST) { //ждем длину имени файла
            if (buf.readableBytes() >= 4) { //читаем 4 байта из буфера
                nameSize = buf.readInt(); //вычитываем из буфера длину имени файла в переменную nameSize
                currentState = State.NAME_LIST; //меняем состояние на NAME_LIST
            }
        }

        //обработка СОСТОЯНИЯ NAME_LIST:
        if (currentState == State.NAME_LIST) { //ждем имя файла в листе
            if (buf.readableBytes() >= nameSize) { //читаем из буфера байты по размеру nameSize
                byte[] nextFileNameBytes = new byte[nameSize]; //создаем байтовый массив по размеру nameSize
                buf.readBytes(nextFileNameBytes); //вычитываем байты из буфера пишем их в байтовый массив
                getFileName(nextFileNameBytes); //запускаем метод преобразования байтового массива в стринг с присвоением переменной fileName
                currentState = State.FILE_LENGTH_LIST; //меняем состояние на FILE_LENGTH_LIST
            }
        }

        //обработка СОСТОЯНИЯ FILE_LENGTH_LIST:
        if (currentState == State.FILE_LENGTH_LIST) { //ждем получения размера файла в листе
            if (buf.readableBytes() >= 8) {  //читаем 8 байт из буфера
                long fileSize = buf.readLong(); //вычитываем из буфера размер файла в переменную fileSize
                filePath = Paths.get("client_storage", userDirectory, fileName); //получаем полный путь к файлу - собирая путь и имя
                FileInfo fileInfo = new FileInfo(filePath.toFile(), fileName, fileSize); //создаем файлинфо с объектом файл+имя+размер
                GUIHelper.serverFilesList.add(fileInfo); //добавляем в статическую переменную хелпера информацию о файле
                listSize--; //уменьшаем счетчик файлов(строк) в списке файлов
                currentState = State.NAME_LENGTH_LIST; //сбрасываем состояние NAME_LENGTH_LIST для следующей итерации получения файла
            }
        }
        if (listSize == 0) { //если все строки из листа прочитали
            callbackList.get(1).callback(); //вызываем обработку метода колбека - обновить список файлов сервера -refreshServerFilesTable
            fileListReading = false; //сбрасываем СТАТУС fileListReading
            currentState = State.COMMAND; //сбрасываем СОСТОЯНИЕ COMMAND
        }
    }

    private void readFile(ByteBuf buf) {

        //обработка СОСТОЯНИЯ NAME_LENGTH:
        if (currentState == State.NAME_LENGTH) { //ждем длину имени
            if (buf.readableBytes() >= 4) { //читаем 4 байта из буфера
                System.out.println("STATE: Get filename length");
                nameSize = buf.readInt(); //вычитываем из буфера длину имени файла в переменную nameSize
                currentState = State.NAME; //меняем состояние на NAME
            }
        }

        //обработка СОСТОЯНИЯ NAME
        if (currentState == State.NAME) { //ждем имя файла
            if (buf.readableBytes() >= nameSize) { //читаем из буфера байты длинной nameSize
                byte[] fileNameBytes = new byte[nameSize]; //создаем байтовый массив по размеру nameSize
                buf.readBytes(fileNameBytes); //вычитываем байты из буфера пишем их в байтовый массив
                getFileName(fileNameBytes); //запускаем метод преобразования байтового массива в стринг с присвоением переменной fileName
                System.out.println("STATE: Filename received - " + fileName);
                filePath = Paths.get("client_storage", userDirectory, fileName); //получаем полный путь к файлу - собирая путь и имя
                deleteFileIfExist(filePath); //запускаем метод удаления файла если он уже там есть
                currentState = State.FILE_LENGTH; //меняем состояние на FILE_LENGTH
            }
        }

        //обработка СОСТОЯНИЯ FILE_LENGTH:
        if (currentState == State.FILE_LENGTH) {// ждем длину файла
            try {
                if (buf.readableBytes() >= 8) { //читаем 8 байт из буфера
                    fileLength = buf.readLong(); //вычитываем из буфера длину файла в переменную fileLength
                    System.out.println("STATE: File length received - " + fileLength);
                    if (fileLength == 0) { // если длина файла =0
                        currentState = State.COMMAND; //сбрасываем СОСТОЯНИЕ в COMMAND
                        Files.createFile(filePath); //создаем файл
                        callbackList.get(2).callback(); //вызываем обработку метода колбека - обновить список файлов клиента -refreshClientFilesTable
                    } else {
                        out = new BufferedOutputStream(new FileOutputStream(filePath.toString())); //иначе создаем поток для создания файла:
                        currentState = State.FILE; //меняем состояние FILE
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //обработка СОСТОЯНИЯ FILE
        if (currentState == State.FILE) { //ждем получения файла
            try {
                while (buf.readableBytes() > 0) { //пока буфер не пустой
                    out.write(buf.readByte()); //вычитываем данные из буфера в подготовленный поток к файлу
                    receivedFileLength++; //увеличиваем счетчик байтов файла
                    if (fileLength == receivedFileLength) { //если счетчик переданных байтов = длине файла
                        currentState = State.COMMAND; //сбрасываем состояние в исходное COMMAND
                        out.close(); //закрываем поток создания файла
                        fileReading = false; //сбрасываем СТАТУС fileReading
                        System.out.println("File received");
                        callbackList.get(2).callback(); //вызываем обработку метода колбека - обновить список файлов клиента -refreshClientFilesTable
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * метод получения имени файла из байтбуфера
     * @param nextFileNameBytes - байтбуфер с данными для парсинга
     */
    private void getFileName(byte[] nextFileNameBytes) {
        try {
            fileName = new String(nextFileNameBytes, "UTF-8"); //узнаем имя файла из байтов
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    /**
     * метод удаления файла в пути если он там уже есть
     * @param filePath путь к файлу
     */
    private void deleteFileIfExist(Path filePath) {
        try {
            if (Files.exists(filePath)) { //если файл существует
                Files.delete(filePath); //удалить файл
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
