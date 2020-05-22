package com.malichenko.yury.netty.server;

import java.sql.*;
import java.util.concurrent.*;

/**
 * Сервис авторизации клиента в БД
 */
public class DataBaseAuthService {
    private static Connection conn; //конекшен к базе 1 на всех
    private static Statement stmt; //стейтмен тож 1
    private static ExecutorService ex; //треды для выполнения запростов

    public DataBaseAuthService() {
        try {
            connection(); // подключаемся к базе при старте сервера
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * проверка директории пользователя по логину паролю
     * @param loginEntry логин
     * @param passEntry пароль
     * @return имя папки пользователя
     */
    public String getDirectoryByLoginPass(String loginEntry, String passEntry)  {
        String directory = null;
        Future<String> future = ex.submit(new DirectoryOfUser(loginEntry, passEntry)); //запускаем метод проверки
        try {
            directory = future.get(); //фюче возвращает директорию
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
        return directory;//возвращает переменную директория
    }

    /**
     * подключение к БД
     * @throws ClassNotFoundException
     * @throws SQLException
     */
    private static void connection() throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        conn = DriverManager.getConnection("jdbc:sqlite:db.db");
        stmt = conn.createStatement();
        ex = Executors.newFixedThreadPool(10);
    }

    /**
     * закрываем подключение к базе и потоки
     */
    public void disconnect() {
        ex.shutdown(); //завершаем пул потоков
        try {
            conn.close(); //закрываем соединение при завершении работы сервера
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

    }

    /**
     * запрос к БД
     */
    private static class DirectoryOfUser implements Callable<String> {
        String loginEntry; //логин
        String passEntry; //пароль

        DirectoryOfUser(String loginEntry, String passEntry) {
            this.loginEntry = loginEntry;
            this.passEntry = passEntry;
        }

        @Override
        public String call() {
            String directory = null;
            try {
                ResultSet rs = stmt.executeQuery(
                        "SELECT * FROM users_info " + "WHERE login = '" + loginEntry + "' AND password = '" + passEntry + "' LIMIT 1"); //запрос в БД
                while (rs.next()) { //пока есть что читать в ответе из запроса
                    directory = rs.getString("login"); //директоря = значению в столбце логин
                    rs.close(); //закрыть результат
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return directory;
        }
    }
}
