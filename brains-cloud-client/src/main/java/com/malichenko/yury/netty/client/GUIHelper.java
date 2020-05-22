package com.malichenko.yury.netty.client;


import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.List;

public class GUIHelper {
    public static String userDirectory; //название директории пользователя
    public static List<FileInfo> serverFilesList; //список файлов для отображения

    /**
     * метод обновления интерфейса
     * запускает параллельные задачи в одном окне
     * @param r
     */
    public static void updateUI(Runnable r) {
        if (Platform.isFxApplicationThread()) { // если запускает поток isFxApplication
            r.run();  //запускаем задачу
        } else {
            Platform.runLater(r); //запустить задачу в будущем позже
        }
    }

    public static void setFileLabel(TableView<FileInfo> filesTable, Label fileLabel) {
        TableView.TableViewSelectionModel<FileInfo> selectionModel = filesTable.getSelectionModel();
        selectionModel.selectedItemProperty().addListener(new ChangeListener<FileInfo>() {
            @Override
            public void changed(ObservableValue<? extends FileInfo> observableValue, FileInfo oldInfo, FileInfo newInfo) {
                if (newInfo != null) {
                    fileLabel.setText(newInfo.getName());
                }
            }
        });
    }


    public static void setCellValue(TableColumn<FileInfo, String> filesName,
                                    TableColumn<FileInfo, Long> filesSize) {
        filesName.setCellValueFactory(new PropertyValueFactory<>("name"));
        filesSize.setCellValueFactory(param -> {
            long size = param.getValue().getSize();
            return new ReadOnlyObjectWrapper(size + " bytes");
        });
    }

    /**
     * метод обработки ошибок на клиенте
     * @param e получаем исключение для отображения
     */
    public static void showError(Exception e) {
        GUIHelper.updateUI(() -> { //лямбда выполнить следующее задание в потоке

            //всплывающие окошки ошибок
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Something went wrong!");
            alert.setHeaderText(e.getMessage());

            VBox dialogPaneContent = new VBox();
            Label label = new Label("Stack Trace:");

            String stackTrace = ExceptionUtils.getStackTrace(e);
            TextArea textArea = new TextArea();
            textArea.setText(stackTrace);

            dialogPaneContent.getChildren().addAll(label, textArea);

            // Set content for Dialog Pane
            alert.getDialogPane().setContent(dialogPaneContent);
            alert.setResizable(true);
            alert.showAndWait(); // показать и ждать реакции

            e.printStackTrace();
        });

    }

}
