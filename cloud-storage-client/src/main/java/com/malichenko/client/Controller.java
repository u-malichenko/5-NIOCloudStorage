package com.malichenko.client;

import com.malichenko.common.FileInfo;
import com.malichenko.common.ListCommandByte;
import com.malichenko.common.Sender;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
    public @FXML
    VBox storagePanel;

    public @FXML
    TableView<FileInfo> clientFilesTable;
    public @FXML
    TableColumn<FileInfo, String> clientFilesName;
    public @FXML
    TableColumn<FileInfo, Long> clientFilesSize;

    public @FXML
    TableView<FileInfo> serverFilesTable;
    public @FXML
    TableColumn<FileInfo, String> serverFilesName;
    public @FXML
    TableColumn<FileInfo, Long> serverFilesSize;

    private ObservableList<FileInfo> clientFilesList; //прослушиваемый список файлов клиента
    private ObservableList<FileInfo> serverFilesList;
    private Label clientsFileLabel; //лисенер выделеной строки в clientFilesList
    private Label serversFileLabel;
    private String userDirectory;
    private Path filePath;
    private List<Callback> callbackList;
    private static Logger logger = LogManager.getLogger();

    /**
     * инициализация колбеков сети
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            GUIHelper.serverFilesList = new ArrayList<>(); //создаем список файлов сервера для замены при обновлении
            callbackList = new ArrayList<>(); //создаем список для хранения всех колбеков

            Callback authOkCallback = this::showStoragePanel; //при логине обновить панель- callbackList.get(0).callback();
            Callback serverFilesRefreshCallback = this::refreshServerFilesTable; //запрос обновления списка файлов сервера-callbackList.get(1).callback();
            Callback clientsFilesRefreshCallback = this::refreshClientFilesTable; //запрос обновления списка файлов клиента-callbackList.get(2).callback();
            callbackList.addAll(List.of(authOkCallback, serverFilesRefreshCallback, clientsFilesRefreshCallback)); //все колбеки в одном листе
            logger.debug("initialize создали список колбеков");
            CountDownLatch connectionOpened = new CountDownLatch(1);
            new Thread(() -> Network.getInstance().start(connectionOpened, callbackList)).start();
            logger.debug("initialize в новом потоке запустили Network");
            connectionOpened.await();
            logger.debug("initialize дождались инициализации Network");
        } catch (InterruptedException e) {
            GUIHelper.showError(e);
        }
    }

    /**
     * отправка запроса авторизации обработка конопки панели авторизации- подключиться
     */
    public @FXML
    HBox authPanel;
    public @FXML
    TextField loginField;
    public @FXML
    PasswordField passField;

    public @FXML
    void sendAuth() {
        String login = loginField.getText();
        String password = passField.getText();
        GUIHelper.userDirectory = login; //назначить имя директории пользователя в гуихелпере (потом вытащим в ClientAuthHandler)
        logger.debug("sendAuth назначить имя директории пользователя в GUIHelper.userDirectory: " + login);
        this.userDirectory = login; //назначить имя директории пользователя в контроллере
        logger.debug("sendAuth назначить имя директории пользователя this.userDirectory: " + login);
        logger.debug("sendAuth запускаем статику отправки авторизации ClientAuthHandler.sendAuthInfo логин = " + login + " пароль = " + password);
        ClientAuthHandler.sendAuthInfo(Network.getInstance().getCurrentChannel(), login, password);
    }

    public @FXML
    void downloadFile() {
        filePath = Paths.get("server_storage", userDirectory, serversFileLabel.getText());
        logger.debug("downloadFile формируем путь к файлу для скачивания с сервера filePath: " + filePath + ", из userDirectory: " + userDirectory + " serversFileLabel.getText(): " + serversFileLabel.getText());
        logger.debug("downloadFile запускаем отправку команды и имени для запроса скачивания файла с сервера командный байт: REQUEST_FILE");
        Sender.sendCommandAndFileName(filePath, Network.getInstance().getCurrentChannel(), ListCommandByte.REQUEST_FILE);
    }

    public @FXML
    void sendFile() throws IOException {
        filePath = Paths.get("client_storage", userDirectory, clientsFileLabel.getText());
        logger.debug("sendFile формируем путь к файлу filePath: " + filePath + ", из userDirectory: " + userDirectory + " clientsFileLabel.getText(): " + clientsFileLabel.getText());
        logger.debug("sendFile запускаем отправку файла Sender.sendFile: " + filePath);
        Sender.sendFile(filePath, Network.getInstance().getCurrentChannel(), future -> {
            if (!future.isSuccess()) {
                logger.error("sendFile ошибка отправки файла Sender.sendFile: " + filePath);
                GUIHelper.showError((Exception) future.cause());
            }
            if (future.isSuccess()) {
                logger.debug("sendFile файл отправлен Sender.sendFile: " + filePath);
                logger.debug("sendFile запускаем отправку команды на обновление списка файлов сервера Sender.sendCommand: SERVER_FILES_LIST_INFO");
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
        logger.debug("clientFileDelete формируем путь к файлу filePath: " + filePath + ", из userDirectory: " + userDirectory + " clientsFileLabel.getText(): " + clientsFileLabel.getText());
        if (filePath.toFile().delete()) {
            logger.debug("clientFileDelete запускаем метод обновление списка файлов клиента refreshClientFilesTable()");
            refreshClientFilesTable();
        } else {
            logger.error("clientFileDelete ошибка удаления локального файла: " + filePath);
            refreshClientFilesTable();
            GUIHelper.showError(new RuntimeException("Не удалось удалить файл"));
        }
    }

    public @FXML
    void serverFileDelete() {
        filePath = Paths.get("server_storage", userDirectory, serversFileLabel.getText());
        logger.debug("serverFileDelete формируем путь к файлу для удаления с сервера filePath: " + filePath + ", из userDirectory: " + userDirectory + " serversFileLabel.getText(): " + serversFileLabel.getText());
        logger.debug("serverFileDelete запускаем отправку команды и имени для запроса удаления файла с сервера командный байт: DELETE_FILE");
        Sender.sendCommandAndFileName(filePath, Network.getInstance().getCurrentChannel(), ListCommandByte.DELETE_FILE);
    }

    /**
     * class ClientApp - stage.setOnHidden(e -> controller.shutdown());
     */
    public void shutdown() {
        logger.debug("client shutdown завершаем работу сети Network.getInstance().stop()");
        Network.getInstance().stop();
    }

    /**
     * метод замены интерфейса с панели аутентификации на панель клиента,
     * генерации списков файлов и их прослушивание
     */
    public void showStoragePanel() {
        authPanel.setVisible(false); //скрыть панель авторизации
        logger.debug("showStoragePanel скрываем authPanel");
        storagePanel.setVisible(true); //показать панель работы с файлами
        logger.debug("showStoragePanel отрисовываем storagePanel");

        clientFilesList = FXCollections.observableArrayList(); //создаем прослушиваемый лист файлов на клиенте
        GUIHelper.setCellValue(clientFilesName, clientFilesSize); //определяем значения строк таблицы клиентов
        logger.debug("showStoragePanel GUIHelper.setCellValue(clientFilesName:" + clientFilesName + " clientFilesSize: " + clientFilesSize);

        serverFilesList = FXCollections.observableArrayList();
        GUIHelper.setCellValue(serverFilesName, serverFilesSize);
        logger.debug("showStoragePanel GUIHelper.setCellValue(serverFilesName:" + serverFilesName + " serverFilesSize: " + serverFilesSize);

        clientFilesTable.setItems(clientFilesList); //установить элементы в таблицу клиент TableView<FileInfo>
        logger.debug("showStoragePanel clientFilesTable.setItems(clientFilesList)" + clientFilesList);
        serverFilesTable.setItems(serverFilesList);
        logger.debug("showStoragePanel serverFilesTable.setItems(serverFilesList)" + serverFilesList);

        clientsFileLabel = new Label(); //создаем новый лейбл активного поля
        GUIHelper.setFileLabel(clientFilesTable, clientsFileLabel); //установить слушателя выбранной строки в таблице
        logger.debug("showStoragePanel GUIHelper.setFileLabel(clientFilesTable: " + clientFilesTable + " clientsFileLabel: " + clientsFileLabel);

        serversFileLabel = new Label();
        GUIHelper.setFileLabel(serverFilesTable, serversFileLabel);
        logger.debug("showStoragePanel GUIHelper.setFileLabel(serverFilesTable,: " + serverFilesTable + " serversFileLabel: " + serversFileLabel);

        logger.debug("showStoragePanel запускаем метод обновление списка файлов клиента refreshClientFilesTable()");
        refreshClientFilesTable();
        logger.debug("showStoragePanel запускаем отправку команды на обновление списка файлов сервера Sender.sendCommand: SERVER_FILES_LIST_INFO");
        Sender.sendCommand(Network.getInstance().getCurrentChannel(), ListCommandByte.SERVER_FILES_LIST_INFO);
    }

    /**
     * метод обновления списка файлов клиента
     */
    public void refreshClientFilesTable() {
        logger.debug("refreshClientFilesTable запускаем метод обновление списка файлов клиента через GUIHelper.updateUI(() -> {");
        GUIHelper.updateUI(() -> {
            try {
                if (!Files.exists(Paths.get("client_storage", userDirectory))) {
                    Files.createDirectory(Paths.get("client_storage", userDirectory));
                }
                clientFilesList.clear();
                logger.debug("refreshClientFilesTable чистим список файлов clientFilesList.clear()");
                //добавляем в список файлов клиента, все объекты файлинфо сгенеренные из директории пользователя:
                clientFilesList.addAll(Files.list(Paths.get("client_storage", userDirectory))// (берем директорию пользователя - вычитываем все файлы в поток путей)
                        .filter(p -> !Files.isDirectory(p)) //фильтруем - оставляя только файлы
                        .map(FileInfo::new) //преобразуем к файлинфо. берем каждый файл потока и отдаем в конструктор файлинфо = получаем поток обектов файл инфо
                        .sorted(Comparator.comparing(FileInfo::getName))//сортируем по имени
                        .collect(Collectors.toList()));
                logger.debug("refreshClientFilesTable добавили все в clientFilesList: " + clientFilesList.toString());
            } catch (IOException e) {
                e.printStackTrace();
                logger.error(e);
            }
        });
    }

    /**
     * метод обновления таблицы списка файлов сервера.
     * (два разных списка файлов в контроллере и в хелпере)-
     * список контроллера чистим и наполняем из хелпера
     */
    public void refreshServerFilesTable() {
        logger.debug("refreshServerFilesTable запускаем метод обновление списка файлов сервера через GUIHelper.updateUI(() -> {");
        GUIHelper.updateUI(() -> {
            serverFilesList.clear();
            logger.debug("refreshServerFilesTable чистим список файлов serverFilesList.clear()");
            serverFilesList.addAll(GUIHelper.serverFilesList.stream() //добавляем все обекты списка из хелпера - делаем поток
                    .sorted(Comparator.comparing(FileInfo::getName)) //сортируем по имени
                    .collect(Collectors.toList()));
        });
        logger.debug("refreshServerFilesTable добавили все в serverFilesList: " + serverFilesList);
    }

    public void btnExitAction(ActionEvent actionEvent) {
        //TODO отправить уведомление серверу об отключении клиента видео 4 время 2:11
        logger.debug("btnExitAction завершаем работу!");
        Platform.exit();
    }
}
