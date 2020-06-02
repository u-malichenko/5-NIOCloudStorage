package com.malichenko.yury.netty.server;

import java.sql.*;
import java.util.concurrent.*;

/**
 * Сервис авторизации клиента в БД
 */
public class ServerDBAuthService {
    private static Connection conn;
    private static Statement stmt;
    private static ExecutorService ex;

    public ServerDBAuthService() {
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
            directory = future.get(); //фюче возвращает директорию (из базы логин и пароль подошли)
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
        return directory;
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
        ex = Executors.newFixedThreadPool(2);
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
        String loginEntry;
        String passEntry;

        DirectoryOfUser(String loginEntry, String passEntry) {
            this.loginEntry = loginEntry;
            this.passEntry = passEntry;
        }

        @Override
        public String call() {
            String directory = null;
            try {
                ResultSet rs = stmt.executeQuery(
                        "SELECT * FROM users_info " + "WHERE login = '" + loginEntry + "' AND password = '" + passEntry + "' LIMIT 1");
                while (rs.next()) {
                    directory = rs.getString("login"); //директоря = значению в столбце логин
                    rs.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return directory;
        }
    }
}
