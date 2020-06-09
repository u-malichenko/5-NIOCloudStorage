package com.malichenko.client;


import com.malichenko.common.FileInfo;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * showError - показывает ошибки в отдельном окошке
 * updateUI - запуск задачи в потоке JavaFX
 * setCellValue - определяет значения в строках
 * setFileLabel - установить слушателя выбранной строки в таблице
 */
public class GUIHelper {
    public static String userDirectory; //название директории пользователя
    public static List<FileInfo> serverFilesList; //список файлов сервера для замены при обновлении
    private static Logger logger = LogManager.getLogger();

    /**
     * проверка что поток является JavaFX r.run(); или Platform.runLater(r)
     * class Controller - refreshServerFilesTable()  { GUIHelper.updateUI(() -> {
     * class Controller - refreshClientFilesTable() { GUIHelper.updateUI(() -> {
     * @param r
     */
    public static void updateUI(Runnable r) {
        logger.debug("updateUI проверяем что поток FX - Platform.isFxApplicationThread(): " + Platform.isFxApplicationThread());
        if (Platform.isFxApplicationThread()) { // Возвращает true, если вызывающий поток является Потоком Приложения JavaFX
            logger.debug("updateUI запускаем Runnable r.run()");
            r.run();
        } else {
            logger.debug("updateUI иначе! Запустите указанный Runnable в потоке приложений JavaFX в неустановленное время в будущем. - Platform.runLater(r)");
            Platform.runLater(r);
        }
    }

    /**
     * установить слушателя выбранной строки в таблице
     */
    public static void setFileLabel(TableView<FileInfo> filesTable, Label fileLabel) {
        //получение выделенных в TableView строк- selectionModel
        TableView.TableViewSelectionModel<FileInfo> selectionModel = filesTable.getSelectionModel();
        //Для прослушивания изменений выделенного элемента-
        selectionModel.selectedItemProperty().addListener((observableValue, oldInfo, newInfo) -> {
            if (newInfo != null) {
                fileLabel.setText(newInfo.getName());// заменить имя
                logger.debug("setFileLabel задаем имя fileLabel.setText "+newInfo.getName());
            }
        });
    }

    /**
     * установить значения в строке
     */
    public static void setCellValue(TableColumn<FileInfo, String> filesName, TableColumn<FileInfo, Long> filesSize) {
        //определяем фабрику для столбца filesName с привязкой к свойству name FileInfo
        filesName.setCellValueFactory(new PropertyValueFactory<>("name"));
        logger.debug("setCellValue filesName.setCellValueFactory "+filesName);
        //определяем фабрику для столбца filesSize - получаем размер методом getSize() FileInfo
        filesSize.setCellValueFactory(param -> {
            long size = param.getValue().getSize();
            //logger.debug("setCellValue filesSize.setCellValueFactory "+size);
            return new ReadOnlyObjectWrapper(size + " bytes");
        });
    }

    /**
     * показывает ошибки в отдельном окошке.
     * ClientApp
     * @param e получаем исключение для отображения
     */
    public static void showError(Exception e) {
        GUIHelper.updateUI(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Something went wrong!");
            alert.setHeaderText(e.getMessage());
            VBox dialogPaneContent = new VBox();
            Label label = new Label("Stack Trace:");
            String stackTrace = ExceptionUtils.getStackTrace(e);
            TextArea textArea = new TextArea();
            textArea.setText(stackTrace);
            dialogPaneContent.getChildren().addAll(label, textArea);
            alert.getDialogPane().setContent(dialogPaneContent);
            alert.setResizable(true);
            alert.showAndWait();
            e.printStackTrace();
        });
    }
}