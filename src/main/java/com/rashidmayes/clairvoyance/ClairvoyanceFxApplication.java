package com.rashidmayes.clairvoyance;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.IAerospikeClient;
import com.rashidmayes.clairvoyance.controller.ConnectController;
import com.rashidmayes.clairvoyance.model.ApplicationModel;
import com.rashidmayes.clairvoyance.util.ClairvoyanceLogger;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class ClairvoyanceFxApplication extends Application {

    // pagination based on this page size
    public static final int RECORDS_PER_PAGE = 30;
    // safety net to prevent from too much of memory consumption
    public static final long MAX_RECORDS_PER_SET = 1_000_000;
    // refresh namespace tree on the left side of the browser at given interval
    public static final long REFRESH_BROWSER_INTERVAL = 10;
    public static final TimeUnit REFRESH_BROWSER_INTERVAL_UNIT = TimeUnit.MINUTES;

    public static final Preferences PREFERENCES = Preferences.userNodeForPackage(ClairvoyanceFxApplication.class);

    public static IAerospikeClient getClient() throws AerospikeException {
        var aerospikeClientResult = ApplicationModel.INSTANCE.getAerospikeClient();
        if (aerospikeClientResult.hasError()) {
            throw new AerospikeException(aerospikeClientResult.getError());
        }
        return aerospikeClientResult.getData();
    }

    public static void displayAlert(String text) {
        Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, text)
                .showAndWait());
    }

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage stage) throws Exception {
        ClairvoyanceLogger.logger.info("starting clairvoyance...");

        stage.setOnCloseRequest(onCloseEventHandler());

        Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();
        stage.setTitle("Clairvoyance");
        stage.setX(primaryScreenBounds.getMinX());
        stage.setY(primaryScreenBounds.getMinY());
        stage.setWidth(primaryScreenBounds.getWidth());
        stage.setHeight(primaryScreenBounds.getHeight());
        stage.getIcons().add(new Image("images/icon.png"));

        var resource = getClass().getClassLoader().getResource("fxml/connect.fxml");
        Objects.requireNonNull(resource, "connect.fxml is missing");
        Parent root = FXMLLoader.load(resource);
        root.setUserData(ConnectController.ConnectionStyle.FIRST_TIME);
        Scene scene = new Scene(root);

        stage.setScene(scene);
        stage.show();
    }

    private EventHandler<WindowEvent> onCloseEventHandler() {
        return event -> {
            try {
                PREFERENCES.sync();
            } catch (BackingStoreException e) {
                ClairvoyanceLogger.logger.error(e.getMessage(), e);
            }
            System.exit(0);
        };
    }

}
