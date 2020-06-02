package com.malichenko.yury.netty.client;

import com.malichenko.yury.netty.common.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;


public class Controller implements Initializable {
    public @FXML VBox storagePanel;

    public @FXML TableView<FileInfo> clientFilesTable;
    public @FXML TableColumn<FileInfo, String> clientFilesName;
    public @FXML TableColumn<FileInfo, Long> clientFilesSize;

    public @FXML TableView<FileInfo> serverFilesTable;
    public @FXML TableColumn<FileInfo, String> serverFilesName;
    public @FXML TableColumn<FileInfo, Long> serverFilesSize;

    private ObservableList<FileInfo> clientFilesList; //прослушиваемый список файлов клиента
    private ObservableList<FileInfo> serverFilesList;

    private Label clientsFileLabel; //лисенер выделеной строки в clientFilesList
    private Label serversFileLabel;

    private String userDirectory;
    private Path filePath;
    private List<Callback> callbackList;

    /**
     * инициализация колбеков сети
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            GUIHelper.serverFilesList = new ArrayList<>(); //создаем список файлов сервера для замены при обновлении
            callbackList = new ArrayList<>(); //создаем список для хранения всех колбеков

            Callback authOkCallback = this::showStoragePanel; //при логине обновить панель- callbackList.get(0).callback();
            Callback serverFilesRefreshCallback = this::refreshServerFilesTable; //запрос обновления списка файлов сервера - callbackList.get(1).callback();
            Callback clientsFilesRefreshCallback = this::refreshClientFilesTable; //запрос обновления списка файлов клиента - callbackList.get(2).callback();
            callbackList.addAll(List.of(authOkCallback, serverFilesRefreshCallback, clientsFilesRefreshCallback)); //все колбеки в одном листе

            CountDownLatch connectionOpened = new CountDownLatch(1);
            new Thread(() -> Network.getInstance().start(connectionOpened, callbackList)).start();
            connectionOpened.await();
        } catch (InterruptedException e) {
            GUIHelper.showError(e);
        }
    }
    /**
     * отправка запроса авторизации обработка конопки панели авторизации- подключиться
     */
    public @FXML HBox authPanel;
    public @FXML TextField loginField;
    public @FXML PasswordField passField;
    public @FXML
    void sendAuth() {
        String login = loginField.getText();
        String password = passField.getText();
        GUIHelper.userDirectory = login; //назначить имя директории пользователя в гуихелпере (потом вытащим в ClientAuthHandler)
        this.userDirectory = login; //назначить имя директории пользователя в контроллере
        ClientAuthSender.sendAuthInfo(Network.getInstance().getCurrentChannel(), login, password);
    }

    public @FXML
    void sendFile() throws IOException {
        filePath = Paths.get("client_storage", userDirectory, clientsFileLabel.getText());
        Sender.sendFile(filePath, Network.getInstance().getCurrentChannel(), future -> {
            if (!future.isSuccess()) {
                GUIHelper.showError((Exception) future.cause());
            }
            if (future.isSuccess()) {
                Sender.sendCommand(Network.getInstance().getCurrentChannel(), ListCommandByte.SERVER_FILES_LIST_INFO);
            }
        });
    }

    /**
     * кнопка удаление файла на клиенте (локально)
     */
    public @FXML
    void clientFileDelete() {
        filePath = Paths.get("client_storage", userDirectory, clientsFileLabel.getText());
        if (filePath.toFile().delete()) {
            refreshClientFilesTable();
        } else {
            GUIHelper.showError(new RuntimeException("Не удалось удалить файл"));
        }
    }

    public @FXML
    void downloadFile() {
        filePath = Paths.get("server_storage", userDirectory, serversFileLabel.getText());
        Sender.sendCommandAndFileName(filePath, Network.getInstance().getCurrentChannel(), ListCommandByte.REQUEST_FILE);
    }

    public @FXML
    void serverFileDelete() {
        filePath = Paths.get("server_storage", userDirectory, serversFileLabel.getText());
        Sender.sendCommandAndFileName(filePath, Network.getInstance().getCurrentChannel(), ListCommandByte.DELETE_FILE);
    }

    /**
     * class ClientApp - stage.setOnHidden(e -> controller.shutdown());
     */
    public void shutdown() {
        Network.getInstance().stop();
    }

    /**
     * метод замены интерфейса с панели аутентификации на панель клиента,
     * генерации списков файлов и их прослушивание
     */
    public void showStoragePanel() {
        authPanel.setVisible(false); //скрыть панель авторизации
        storagePanel.setVisible(true); //показать панель работы с файлами

        clientFilesList = FXCollections.observableArrayList(); //создаем прослушиваемый лист файлов на клиенте
        GUIHelper.setCellValue(clientFilesName, clientFilesSize); //определяем значения строк таблицы клиентов

        serverFilesList = FXCollections.observableArrayList();
        GUIHelper.setCellValue(serverFilesName, serverFilesSize);

        clientFilesTable.setItems(clientFilesList); //установить элементы в таблицу клиент TableView<FileInfo>
        serverFilesTable.setItems(serverFilesList);

        clientsFileLabel = new Label(); //создаем новый лейбл клиенту
        GUIHelper.setFileLabel(clientFilesTable, clientsFileLabel); //установить слушателя выбранной строки в таблице

        serversFileLabel = new Label();
        GUIHelper.setFileLabel(serverFilesTable, serversFileLabel);

        refreshClientFilesTable(); //метод обновления списка файлов клиента
        Sender.sendCommand(Network.getInstance().getCurrentChannel(), ListCommandByte.SERVER_FILES_LIST_INFO); //запрос - обновить таблицу сервера:
    }


    /**
     * метод обновления списка файлов клиента
     */
    public void refreshClientFilesTable() {
        GUIHelper.updateUI(() -> {
            try {
                if (!Files.exists(Paths.get("client_storage", userDirectory))) { //если папка еще не создана
                    Files.createDirectory(Paths.get("client_storage", userDirectory)); //создаем новый каталог
                }
                clientFilesList.clear();
                //добавляем в список файлов клиента, все объекты файлинфо сгенеренные из директории пользователя:
                clientFilesList.addAll(Files.list(Paths.get("client_storage", userDirectory))// (берем директорию пользователя - вычитываем все файлы в поток путей)
                        .filter(p -> !Files.isDirectory(p)) //фильтруем - оставляя только файлы
                        .map(FileInfo::new) //преобразуем к файлинфо. берем каждый файл потока и отдаем в конструктор файлинфо = получаем поток обектов файл инфо
                        .sorted(Comparator.comparing(FileInfo::getName))//сортируем по имени
                        .collect(Collectors.toList()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * метод обновления таблицы списка файлов сервера.
     * (два разных списка файлов в контроллере и в хелпере)-
     * список контроллера чистим и наполняем из хелпера
     */
    public void refreshServerFilesTable() {
        GUIHelper.updateUI(() -> {
            serverFilesList.clear();
            serverFilesList.addAll(GUIHelper.serverFilesList.stream() //добавляем все обекты списка из хелпера - делаем поток
                    .sorted(Comparator.comparing(FileInfo::getName)) //сортируем по имени
                    .collect(Collectors.toList()));
        });
    }

    /**
     * закрытие окна-завершение приложения
     */
    public void btnExitAction(ActionEvent actionEvent) {
        //TODO отправить уведомление серверу об отключении клиента видео 4 время 2:11
        Platform.exit();
    }
}
