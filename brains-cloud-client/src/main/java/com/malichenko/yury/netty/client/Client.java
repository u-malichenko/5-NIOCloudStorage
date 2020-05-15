package com.malichenko.yury.netty.client;

import com.malichenko.yury.netty.common.FileSender;

import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;

public class Client {
    public static void main(String[] args) throws Exception {
        CountDownLatch networkStarter = new CountDownLatch(1);
        new Thread(() -> Network.getInstance().start(networkStarter)).start(); // start net connect to server
        networkStarter.await(); //await while not connect to server

        AuthSender.sendAuth("/auth user1 1", Network.getInstance().getCurrentChannel(), future -> {

            if (!future.isSuccess()) {
                System.out.println("Авторизация не пройдена");
                future.cause().printStackTrace();
                //TODO JOptionPane.showConfirmDialog(авторизация не пройдена);
//                Network.getInstance().stop();
            }
            if (future.isSuccess()) { //файл передан печатаем ок
                System.out.println("Авторизация успешно пройдена");
                //TODO JOptionPane.showConfirmDialog("Авторизация успешно пройдена");
//                Network.getInstance().stop();
            }
        });

        //TODO transfer class in conveyor

        FileSender.sendFile(Paths.get("client_storage/11.txt"), Network.getInstance().getCurrentChannel(), future -> {

            //добавляем обработчик события отправки файла - флаг
            if (!future.isSuccess()) { //файл не передан вернем ошибку
                future.cause().printStackTrace();
                //TODO JOptionPane.showConfirmDialog(файл не передан);
//                Network.getInstance().stop();
            }
            if (future.isSuccess()) { //файл передан печатаем ок
                System.out.println("Файл успешно передан");
                //TODO JOptionPane.showConfirmDialog("Файл успешно передан");
//                Network.getInstance().stop();
            }
        });
    }
}
