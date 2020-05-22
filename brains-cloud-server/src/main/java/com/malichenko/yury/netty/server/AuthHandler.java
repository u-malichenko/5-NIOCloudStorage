package com.malichenko.yury.netty.server;

import com.malichenko.yury.netty.common.Command;
import com.malichenko.yury.netty.common.Sender;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Аервичный обработчик входящих данных осуществляет авторизацию клиента с запросом к БД
 * включает состояния (пакеты протокола авторизации):
 * LOGIN_LENGTH - длина логина
 * LOGIN - логин
 * PASSWORD_LENGTH - длина пароля
 * PASSWORD - пароль
 */
public class AuthHandler extends ChannelInboundHandlerAdapter {
    private DataBaseAuthService authService; // подключение к БД

    public enum State {
        LOGIN_LENGTH, LOGIN, PASSWORD_LENGTH, PASSWORD
    }

    private int loginLength; //длина логина
    private int passwordLength; //длина пароля
    private String login; //логин
    private String password; //пароль
    private AuthHandler.State currentState = State.LOGIN_LENGTH; // первичное состояние = LOGIN_LENGTH
    private Logger logger = LogManager.getLogger(); //логер

    public AuthHandler(DataBaseAuthService authService) {
        this.authService = authService; // пробрасываем ссылку из конвеера нетти
    }

    /**
     * метод ЧТЕНИЯ данных из канала
     *
     * @param ctx контекст подключения
     * @param msg полученные байты
     */
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf buf = ((ByteBuf) msg); //то что пришло кастим и кладем в байтбуфер

        try {
            getLoginAndPassword(buf); //запускаем метод получения логина и пароля
        } catch (UnsupportedEncodingException e) {
            logger.error(e); //ошибка в лог
        }

        String userDirectory = authService.getDirectoryByLoginPass(login, password); //запускаем метод проверки директории по логину паролю за БД

        if (userDirectory == null) { //если директории нет
            Sender.sendCommand(ctx.channel(), Command.AUTH_ERR); //запуск метода отправки команды-колбек сервера клиенту ошибка авторизации
            logger.debug("Неверный логин: " + login + " или пароль: " + password);
            return; //завершение чтения
        } else {
            if (!Files.exists(Paths.get("server_storage", userDirectory))) { //если папка еще не создана
                try {
                    Files.createDirectory(Paths.get("server_storage", userDirectory)); //создаем новый каталог
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Sender.sendCommand(ctx.channel(), Command.AUTH_OK); //запуск метода отправки команды-колбека авторицация пройдена
            ctx.pipeline().addLast(new MainHandler(Paths.get("server_storage", userDirectory))); //добавляем в конвейер главный обработчик входящих данных, пробрасываем в его путь к директории пользователя
            ctx.pipeline().remove(this); //удаляем из конвейра обработчик авторизации
            logger.debug("Пользователь : " + login + " успешно прошел авторизацию.");
        }
    }

    /**
     * метод получения лигина и пароля из потока
     * @param buf
     * @throws UnsupportedEncodingException
     */
    private void getLoginAndPassword(ByteBuf buf) throws UnsupportedEncodingException {

        while (buf.readableBytes() > 0) { //работаем в цикле пока в буфере есть данные

            //обработка СОСТОЯНИЯ LOGIN_LENGTH:
            if (currentState == State.LOGIN_LENGTH) { //ждем длину логина
                if (buf.readableBytes() >= 4) { //пока в буфер не придут все байты размера длинны логина - ждем данные в буфер
                    loginLength = buf.readInt(); //вычитываем из буфера байты в переменную loginLength
                    logger.debug("STATE: Get login length - " + loginLength); //запись в лог
                    currentState = State.LOGIN; //меняем состояние на LOGIN
                }
            }

            //обработка СОСТОЯНИЯ LOGIN:
            if (currentState == State.LOGIN) { //теперь ждем логин
                if (buf.readableBytes() >= loginLength) { //данные в буфере= длинна логина
                    byte[] loginBytes = new byte[loginLength]; //создаем байтовый массив длинною = длинна логина
                    buf.readBytes(loginBytes); //вычитываем данные из буфера в массив
                    login = new String(loginBytes, "UTF-8"); //логин = стринг из байт из байтбуфера
                    logger.debug("STATE: login received - " + login); //запись в лог
                    currentState = State.PASSWORD_LENGTH; //меняем статус на PASSWORD_LENGTH
                }
            }

            //обработка СОСТОЯНИЯ PASSWORD_LENGTH:
            if (currentState == State.PASSWORD_LENGTH) { //ждем длину пароля
                if (buf.readableBytes() >= 4) { ///пока в буфер не придут все байты размера длинны пароля - ждем данные в буфер
                    passwordLength = buf.readInt(); //вычитываем данные из буфера в переменную passwordLength
                    logger.debug("STATE: Get password length - " + passwordLength); //запись в лог
                    currentState = State.PASSWORD; //меняем статус на PASSWORD
                }
            }

            //обработка СОСТОЯНИЯ PASSWORD:
            if (currentState == State.PASSWORD) { //ждем получение пароля
                if (buf.readableBytes() >= passwordLength) { //когда размер буфера = размеру пароля
                    byte[] passwordBytes = new byte[passwordLength]; //создаем буфер размером с длину пароля
                    buf.readBytes(passwordBytes); //вычитываем из буфера данные в массив
                    password = new String(passwordBytes, "UTF-8"); //из массива создаем строку
                    logger.debug("STATE: password received - " + password); //запись в лог
                }
                currentState = State.LOGIN_LENGTH; //сбрасываем статус в LOGIN_LENGTH
                break; //прерываем цикл
            }
        }
        if (buf.readableBytes() == 0) { //если буфер пустой то освобождаем буфер
            buf.release();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        logger.error(cause);
        ctx.close();
    }
}
