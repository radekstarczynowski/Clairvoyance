package com.rashidmayes.clairvoyance.controller;

import com.rashidmayes.clairvoyance.ClairvoyanceFxApplication;
import com.rashidmayes.clairvoyance.model.ApplicationModel;
import com.rashidmayes.clairvoyance.model.ConnectionInfo;
import com.rashidmayes.clairvoyance.util.ClairvoyanceLogger;
import com.rashidmayes.clairvoyance.util.Result;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.net.URL;
import java.util.Objects;

public class ConnectController {

    public enum ConnectionStyle {
        FIRST_TIME, RECONNECT
    }

    public record ConnectionResult(boolean connected) {
    }

    @FXML
    private GridPane rootPane;
    @FXML
    private TextField hostField;
    @FXML
    private TextField portField;
    @FXML
    private TextField usernameField;
    @FXML
    private TextField passwordField;
    @FXML
    private Button connectAlternateButton;

    @FXML
    public void initialize() {
        hostField.setText(ClairvoyanceFxApplication.PREFERENCES.get("last.host", null));
        portField.setText(ClairvoyanceFxApplication.PREFERENCES.get("last.port", "3000"));
    }

    @FXML
    public void handleCancelAction(ActionEvent event) {
        event.consume();
        var source = (Node) event.getSource();
        var stage = (Stage) source.getScene().getWindow();
        rootPane.setUserData(new ConnectController.ConnectionResult(false));
        stage.close();
    }

    @FXML
    protected void handleConnectAction(ActionEvent event) {
        try {
            event.consume();
            var source = (Node) event.getSource();
            var stage = (Stage) source.getScene().getWindow();
            var connectionInfoResult = getConnectionInfo(event.getSource() == connectAlternateButton);
            if (connectionInfoResult.hasError()) {
                ClairvoyanceLogger.logger.warn(ClairvoyanceLogger.IN_APP_CONSOLE, "could not connect to cluster: {}", connectionInfoResult.getError());
                ClairvoyanceFxApplication.displayAlert(connectionInfoResult.getError());
                return;
            }
            ApplicationModel.INSTANCE.setConnectionInfo(connectionInfoResult.getData());
            var aerospikeClientResult = ApplicationModel.INSTANCE.createNewAerospikeClient();
            if (aerospikeClientResult.hasError()) {
                ClairvoyanceLogger.logger.warn(ClairvoyanceLogger.IN_APP_CONSOLE, "could not connect to cluster: {}", aerospikeClientResult.getError());
                ClairvoyanceFxApplication.displayAlert("could not connect to cluster");
                return;
            }

            if (rootPane.getUserData() != null && rootPane.getUserData() == ConnectionStyle.RECONNECT) {
                rootPane.setUserData(new ConnectController.ConnectionResult(true));
                stage.close();
            } else {
                URL resource = getClass().getClassLoader().getResource("fxml/browser.fxml");
                Objects.requireNonNull(resource, "browser.fxml is null");
                Parent root = FXMLLoader.load(resource);
                Scene scene = new Scene(root);
                stage.setScene(scene);
            }

            ClairvoyanceFxApplication.PREFERENCES.put("last.host", connectionInfoResult.getData().host());
            ClairvoyanceFxApplication.PREFERENCES.putInt("last.port", connectionInfoResult.getData().port());
        } catch (Exception e) {
            ClairvoyanceLogger.logger.error(e.getMessage(), e);
            ClairvoyanceFxApplication.displayAlert(String.format("Error connecting: %s", e.getMessage()));
        }
    }

    private Result<ConnectionInfo, String> getConnectionInfo(boolean useServiceAlternate) {
        var hostResult = getHost();
        var portResult = getPort();
        var username = this.usernameField.getText();
        var password = this.passwordField.getText();

        if (hostResult.hasError()) {
            return Result.error(hostResult.getError());
        }
        if (portResult.hasError()) {
            return Result.error(portResult.getError());
        }
        return Result.of(new ConnectionInfo(
                hostResult.getData(),
                portResult.getData(),
                username,
                password,
                useServiceAlternate
        ));
    }

    private Result<String, String> getHost() {
        var host = this.hostField.getText();
        if (host == null || host.isBlank()) {
            return Result.error("host cannot be empty");
        }
        return Result.of(host);
    }

    private Result<Integer, String> getPort() {
        var portValue = this.portField.getText();
        if (portValue == null || portValue.isBlank()) {
            return Result.error("port cannot be empty");
        }
        try {
            var port = Integer.parseInt(portValue);
            return Result.of(port);
        } catch (NumberFormatException exception) {
            return Result.error("invalid port");
        }
    }

}
