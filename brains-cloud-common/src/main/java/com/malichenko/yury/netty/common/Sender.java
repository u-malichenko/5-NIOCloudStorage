package com.malichenko.yury.netty.common;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

/**
 * TRANSFER_OBJECT
 */
public class Sender {
    /**
     * метод ОТПРАВКА ФАЙЛА (1sendCommand+ 2sendFileName(size+name)+ 3Files.size(path)+channel.writeAndFlush(region))
     * 1 отправляет команду - чтоб файл приняли на другой стороне
     * 2 передает размер имени файла
     * 3 передает имя файла
     * 4 передает размер файла
     * 5 передает файл
     * ждет результат для передачи отправителю addListener(finishListener)
     *
     * @param path           путь к файлу
     * @param channel        канал куда передавать файл
     * @param command        команда TRANSFER_FILE
     * @param finishListener возвращает отправителю результат отправки
     */
    public static void sendFile(Path path, Channel channel, Command command, ChannelFutureListener finishListener) {
        if (!path.toFile().exists()) {
            System.out.println("Данный файл не существует");
            return;
        }

        sendCommand(channel, command); //отправляем в канал сигнал о передаче файла TRANSFER_FILE
        sendFileName(path, channel); //отправляем в канал имя файла

        ByteBuf buf;
        try {
            FileRegion region = new DefaultFileRegion(path.toFile(), 0, Files.size(path)); // регион для передачи файла

            //отправляем РАЗМЕР ФАЙЛА:
            buf = ByteBufAllocator.DEFAULT.directBuffer(8); //выделяем буфер для размера файла
            buf.writeLong(Files.size(path)); //записываем в буфер длину файла
            channel.writeAndFlush(buf); //отправляем буфер в канал

            //отправляем ФАЙЛ:
            ChannelFuture transferOperationFuture = channel.writeAndFlush(region); //назначаем фьюче для обработки результата отправки файла
            if (finishListener != null) { //если переданное нам фьюче не было пустым
                transferOperationFuture.addListener(finishListener); //назначаем лиснер на фьюче-передачу файла и отдаем его в вызывающий метод
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * метод отправки ДЛИНЫ а следом И ИМЕНИ ФАЙЛА
     *
     * @param path    путь к файлу
     * @param channel канал куда отправлять данные
     */
    public static void sendFileName(Path path, Channel channel) {
        byte[] fileNameBytes = path.getFileName().toString().getBytes(StandardCharsets.UTF_8); // получаем имя файла из пути в байтах
        ByteBuf buf = ByteBufAllocator.DEFAULT.directBuffer(4 + fileNameBytes.length); //выделяем буфер для размера имени файла +имя
        buf.writeInt(fileNameBytes.length); //записываем в буфер длину имени файла
        buf.writeBytes(fileNameBytes); //записываем в буфер имя файла в байтах
        channel.writeAndFlush(buf); //отправляем буфер в канал
    }

    /**
     * метод для отправки КОМАНДЫ
     *
     * @param channel канал куда отправлять данные
     * @param command код команды для отправки
     */
    public static void sendCommand(Channel channel, Command command) {
        ByteBuf buf = ByteBufAllocator.DEFAULT.directBuffer(1);
        buf.writeByte(command.getByteValue()); //пишем в буфер код команды Command.TRANSFER_FILE
        channel.writeAndFlush(buf); //отправляем КОМАНДУ в канал
    }

    /**
     * метод отправки списка файлов и их размеров
     * @param path путь директории для запроса
     * @param channel вызывающий канал
     * @param command управляющая команда
     */
    public static void sendFilesList(Path path, Channel channel, Command command) {
        //ОТПРАВЛЯЕМ командный байт - подготовка к получению списка:
        sendCommand(channel, command);

        List<Path> list; //объявляем список путей
        ByteBuf buf; //объявим байт-буфер
        try {
            //ОТПРАВЛЯЕМ размер списка файлов = строк:
            list = Files.list(path).filter(p -> !Files.isDirectory(p)).collect(Collectors.toList());
            buf = ByteBufAllocator.DEFAULT.directBuffer(4);
            buf.writeInt(list.size());
            channel.writeAndFlush(buf);

            for (Path p : list) {
                //ОТПРАВЛЯЕМ строки с файлами
                byte[] filenameBytes = p.getFileName().toString().getBytes(StandardCharsets.UTF_8); //преобразуем имена в байт-массив
                buf = ByteBufAllocator.DEFAULT.directBuffer(4 + filenameBytes.length + 8); //выделим новый размер буфера (длинна имени+длинна имени+размер файла)
                buf.writeInt(filenameBytes.length); //вычитываем в буфер ДЛИНУ ИМЕНИ файла
                buf.writeBytes(filenameBytes); //вычитываем в буфер ИМЯ файла
                buf.writeLong(Files.size(p)); //вычитываем в буфер РАЗМЕР ФАЙЛА
                channel.writeAndFlush(buf); //отправляем буфер
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * метод отправки данных авторизации
     * @param channel канал куда отправлять данные
     * @param login логин
     * @param password пароль
     */
    public static void sendAuthInfo(Channel channel, String login, String password) {
        byte[] loginBytes = login.getBytes(); //преобразуем в байт-массив логин и пароль
        byte[] passwordBytes = password.getBytes();

        int bufLength = 4 + loginBytes.length + 4 + passwordBytes.length; //вычислим размер будущего байт-буфера
        ByteBuf buf = ByteBufAllocator.DEFAULT.directBuffer(bufLength); //выделим байт буфер вычисленного ранее размера
        buf.writeInt(login.length()); //вычитываем в буфер длину логина
        buf.writeBytes(loginBytes); //вычитываем в буфер сам логин
        buf.writeInt(password.length()); //вычитываем в буфер длину пароля
        buf.writeBytes(passwordBytes); //вычитываем в буфер сам пароль
        channel.writeAndFlush(buf); //отправляем данные в канал
    }
}