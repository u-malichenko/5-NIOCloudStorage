package com.malichenko.yury.netty.client;

import com.malichenko.yury.netty.common.Command;
import com.malichenko.yury.netty.common.Sender;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
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
    public @FXML
    BorderPane storagePanel; //главная панель

    public @FXML
    TableView<FileInfo> clientFilesTable; //таблица файлов клиента

    public @FXML
    TableColumn<FileInfo, String> clientFilesName; //столбец имя файла клиента

    public @FXML
    TableColumn<FileInfo, Long> clientFilesSize; //столбец размер файла клиента

    public @FXML
    TableView<FileInfo> serverFilesTable; //таблица файлов сервера

    public @FXML
    TableColumn<FileInfo, String> serverFilesName; //столбец имя файла сервера

    public @FXML
    TableColumn<FileInfo, Long> serverFilesSize; //столбец размер файла сервера

    private ObservableList<FileInfo> clientFilesList; //прослушиваемый список файлов клиента
    private ObservableList<FileInfo> serverFilesList; //прослушиваемый список файлов сервера
    private String userDirectory; //каталог пользователя
    private Label clientsFileLabel; //TODO имя файла из списка
    private Label serversFileLabel; //TODO
    private Path filePath; //TODO
    private List<Callback> callbackList; //лист для обработки вызовов колбека

    /**
     * метод инициализации запускается при старте
     * @param url
     * @param resourceBundle
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            GUIHelper.serverFilesList = new ArrayList<>();
            callbackList = new ArrayList<>();

            Callback authOkCallback = this::showStoragePanel; //callbackList.get(0).callback();
            Callback serverFilesRefreshCallback = this::refreshServerFilesTable; //callbackList.get(1).callback();
            Callback clientsFilesRefreshCallback = this::refreshClientFilesTable;
            callbackList.addAll(List.of(authOkCallback, serverFilesRefreshCallback, clientsFilesRefreshCallback));

            CountDownLatch networkStarter = new CountDownLatch(1);
            new Thread(() -> Network.getInstance().startNetwork(networkStarter, callbackList)).start();
            networkStarter.await();
        } catch (InterruptedException e) {
            GUIHelper.showError(e);
        }
    }

    public @FXML
    VBox authPanel;
    public @FXML
    TextField loginField;
    public @FXML
    PasswordField passField;

    public @FXML
    void sendAuth() {
        String login = loginField.getText();
        String password = passField.getText();
        GUIHelper.userDirectory = login;
        this.userDirectory = login;
        Sender.sendAuthInfo(Network.getInstance().getCurrentChannel(), login, password);
    }

    public @FXML
    void sendFile() {
        filePath = Paths.get("client_storage", userDirectory, clientsFileLabel.getText());
        Sender.sendFile(filePath, Network.getInstance().getCurrentChannel(), Command.TRANSFER_FILE, future -> {
            if (!future.isSuccess()) {
                GUIHelper.showError((Exception) future.cause());
            }
            if (future.isSuccess()) {
                Sender.sendCommand(Network.getInstance().getCurrentChannel(), Command.SERVER_FILES_LIST_INFO);
            }
        });
    }

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
        filePath = Paths.get("client_storage", userDirectory, serversFileLabel.getText());
        Sender.sendCommand(Network.getInstance().getCurrentChannel(), Command.DOWNLOAD_FILE);
        Sender.sendFileName(filePath, Network.getInstance().getCurrentChannel());
    }

    public @FXML
    void serverFileDelete() {
        filePath = Paths.get("client_storage", userDirectory, serversFileLabel.getText());
        Sender.sendCommand(Network.getInstance().getCurrentChannel(), Command.DELETE_FILE);
        Sender.sendFileName(filePath, Network.getInstance().getCurrentChannel());
    }


    public void shutdown() {
        Network.getInstance().stop();
    }

    /**
     * метод замены интерфейса с панели аутентификации на панель клиента
     */
    public void showStoragePanel() {
        authPanel.setVisible(false);
        storagePanel.setVisible(true);
        clientFilesList = FXCollections.observableArrayList();
        GUIHelper.setCellValue(clientFilesName, clientFilesSize);
        serverFilesList = FXCollections.observableArrayList();
        GUIHelper.setCellValue(serverFilesName, serverFilesSize);
        clientFilesTable.setItems(clientFilesList);
        serverFilesTable.setItems(serverFilesList);
        clientsFileLabel = new Label();
        GUIHelper.setFileLabel(clientFilesTable, clientsFileLabel);
        serversFileLabel = new Label();
        GUIHelper.setFileLabel(serverFilesTable, serversFileLabel);
        refreshClientFilesTable();
        Sender.sendCommand(Network.getInstance().getCurrentChannel(), Command.SERVER_FILES_LIST_INFO);
    }


    public void refreshClientFilesTable() {
        GUIHelper.updateUI(() -> { //передаем ранабл и запускаем обновление таблицы файлов клиента в окне клиента
            try {
                if (!Files.exists(Paths.get("client_storage", userDirectory))) { //если папка еще не создана
                    Files.createDirectory(Paths.get("client_storage", userDirectory)); //создаем новый каталог
                }
                clientFilesList.clear(); //чистим список файлов
                //добавляем в список файлов клиента все объекты файлинфо сгенеренные из директории пользователя
                // (берем директорию пользователя - вычитываем все файлы в поток путей)
                //фильтруем - оставляя только файлы
                //преобразуем поток путей к потоку файлов
                //преобразуем к файлинфо берем каждый файл потока и отдаем в конструктор файлинфо = получаем поток обектов файл инфо
                //сортируем по имени
                //собираем в лист
                clientFilesList.addAll(Files.list(Paths.get("client_storage", userDirectory))
                        .filter(p -> !Files.isDirectory(p))
                        .map(Path::toFile)
                        .map(FileInfo::new)
                        .sorted(Comparator.comparing(FileInfo::getName))
                        .collect(Collectors.toList()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * метод обновления таблицы списка файлов сервера
     * два разных списка файлов в контроллере и в хелпере
     */
    public void refreshServerFilesTable() {
        GUIHelper.updateUI(() -> { //передаем ранабл и запускаем обновление таблицы файлов сервера в окне клиента
            serverFilesList.clear(); //чистим список файлов
            //добавляем все обекты списка из хелпера - делаем поток
            //сортируем по имени
            //собираем в лист
            serverFilesList.addAll(GUIHelper.serverFilesList.stream()
                    .sorted(Comparator.comparing(FileInfo::getName)).
                            collect(Collectors.toList()));
        });
    }

    public void btnExitAction(ActionEvent actionEvent) {
        Platform.exit();
    }
}
