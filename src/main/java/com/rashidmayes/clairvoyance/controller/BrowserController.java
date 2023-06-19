package com.rashidmayes.clairvoyance.controller;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import com.aerospike.client.AerospikeException;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.cluster.Node;
import com.rashidmayes.clairvoyance.ClairvoyanceFxApplication;
import com.rashidmayes.clairvoyance.SimpleTreeNode;
import com.rashidmayes.clairvoyance.model.*;
import com.rashidmayes.clairvoyance.util.ClairvoyanceLogger;
import com.rashidmayes.clairvoyance.util.FileUtil;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.LoggerFactory;

import java.text.NumberFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BrowserController implements ChangeListener<TreeItem<SimpleTreeNode>> {

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(
            1,
            runnable -> {
                var thread = Executors.defaultThreadFactory().newThread(runnable);
                thread.setName("update-cluster-info-scheduled-thread-" + thread.threadId());
                thread.setDaemon(true);
                return thread;
            }
    );

    private final Image rootIcon;
    private final Image nodeIcon;
    private final Image namespaceIcon;
    private final Image setIcon;

    private final NumberFormat numberFormat = NumberFormat.getNumberInstance();

    @FXML
    public GridPane browserGridPane;
    @FXML
    private TextArea console;
    @FXML
    private TreeView<SimpleTreeNode> namespacesTree;
    @FXML
    private TabPane tabs;

    private final NodeInfoMapper nodeInfoMapper = new NodeInfoMapper();

    public BrowserController() {
        this.rootIcon = new Image("images/ic_cluster.png");
        this.nodeIcon = new Image("images/ic_cluster.png");
        this.namespaceIcon = new Image("images/ic_storage.png");
        this.setIcon = new Image("images/ic_set.png");
    }

    @FXML
    public void initialize() {
        createLoggerAppenderForConsole();
        namespacesTree.getSelectionModel()
                .selectedItemProperty()
                .addListener(this);
        executor.scheduleAtFixedRate(updateClusterTreeView(), 0, 2, TimeUnit.MINUTES);
    }

    @Override
    public void changed(ObservableValue<? extends TreeItem<SimpleTreeNode>> observable, TreeItem<SimpleTreeNode> oldValue, TreeItem<SimpleTreeNode> newValue) {
        try {
            if (newValue == null) {
                return;
            }
            var id = newValue.getValue().value.getId();
            var optionalTab = getTab(id);

            if (optionalTab.isEmpty()) {
                var tab = new Tab();
                tab.setId(id);
                tab.setText(newValue.getValue().displayName);
                var identifiable = newValue.getValue().value;

                if (identifiable instanceof NamespaceInfo) {
                    var resource = getClass().getClassLoader().getResource("fxml/tab_namespace.fxml");
                    Objects.requireNonNull(resource, "tab_namespace.fxml is missing");
                    tab.setContent(FXMLLoader.load(resource));
                } else if (identifiable instanceof SetInfo) {
                    var resource = getClass().getClassLoader().getResource("fxml/tab_set.fxml");
                    Objects.requireNonNull(resource, "tab_set.fxml is missing");
                    tab.setContent(FXMLLoader.load(resource));
                } else {
                    var resource = getClass().getClassLoader().getResource("fxml/tab_cluster.fxml");
                    Objects.requireNonNull(resource, "tab_cluster.fxml is missing");
                    tab.setContent(FXMLLoader.load(resource));
                }
                tab.getContent().setUserData(identifiable);
                tabs.getTabs().add(tab);
                tabs.getSelectionModel().select(tab);
            } else {
                tabs.getSelectionModel().select(optionalTab.get());
            }
        } catch (Exception e) {
            ClairvoyanceLogger.logger.error(e.getMessage(), e);
            ClairvoyanceFxApplication.displayAlert(e.getMessage());
        }
    }

    @FXML
    protected void handleReconnect(ActionEvent event) {
        event.consume();
        try {
            var resource = getClass().getClassLoader().getResource("fxml/connect.fxml");
            Objects.requireNonNull(resource, "connect.fxml is missing");
            Parent root = FXMLLoader.load(resource);
            root.setUserData(ConnectController.ConnectionStyle.RECONNECT);

            Scene scene = new Scene(root, 500, 400);
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(scene);

            stage.showAndWait();
            var userData = root.getUserData();
            if (userData != null && ((ConnectController.ConnectionResult) userData).connected()) {
                ClairvoyanceLogger.logger.info("received reconnect request");
                tabs.getTabs().clear();
                namespacesTree.getRoot().getChildren().clear();
                //createNewClient();
                updateClusterTreeView().run();
            }
        } catch (Exception e) {
            ClairvoyanceLogger.logger.error(e.getMessage(), e);
            ClairvoyanceFxApplication.displayAlert("there was an error when trying to reconnect");
        }
    }

    @FXML
    protected void handleClearCache(ActionEvent event) {
        event.consume();
        ApplicationModel.INSTANCE.runInBackground(FileUtil::clearCache);
    }

    @FXML
    protected void handleAbout(ActionEvent event) {
        event.consume();
        try {
            var optionalTab = getTab("about");
            if (optionalTab.isEmpty()) {
                Tab tab = new Tab();
                tab.setId("about");
                tab.setText("About");

                var resource = getClass().getClassLoader().getResource("fxml/tab_about.fxml");
                Objects.requireNonNull(resource, "tab_about.fxml is missing");
                var root = (Parent) FXMLLoader.load(resource);
                tab.setContent(root);

                tabs.getTabs().add(tab);
                tabs.getSelectionModel().select(tab);
            } else {
                tabs.getSelectionModel().select(optionalTab.get());
            }
        } catch (Exception e) {
            ClairvoyanceLogger.logger.error(e.getMessage(), e);
        }
    }

    @FXML
    protected void handleWeb(ActionEvent event) {
        if (event.getSource() instanceof MenuItem) {
            event.consume();
            try {
                MenuItem item = (MenuItem) event.getSource();
                ClairvoyanceLogger.logger.info("opening {}", item.getId());

                Tab tab = new Tab();
                tab.setId(item.getId());
                tab.setText(item.getText());

                var resource = getClass().getClassLoader().getResource("fxml/tab_web.fxml");
                Objects.requireNonNull(resource, "tab_web.fxml is missing");
                Parent root = FXMLLoader.load(resource);
                tab.setContent(root);

                WebEngine engine = ((WebView) root).getEngine();
                engine.load(item.getId());

                tabs.getTabs().add(tab);
                tabs.getSelectionModel().select(tab);
            } catch (Exception e) {
                ClairvoyanceLogger.logger.error(e.getMessage(), e);
            }
        }
    }

    @FXML
    protected void handleExit(ActionEvent event) {
        event.consume();
        System.exit(0);
    }

    @FXML
    public void clearConsole(ActionEvent event) {
        event.consume();
        console.clear();
    }

    public Runnable updateClusterTreeView() {
        return () -> {
            Platform.runLater(() -> {
                ClairvoyanceLogger.logger.info(ClairvoyanceLogger.IN_APP_CONSOLE, "refreshing cluster tree view");
                try {
                    var client = ClairvoyanceFxApplication.getClient();
                    updateTreeView(client);
                } catch (Exception e) {
                    ClairvoyanceLogger.logger.error(e.getMessage());
                }
            });
        };
    }

    private void createLoggerAppenderForConsole() {
        var loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        var patternLayoutEncoder = new PatternLayoutEncoder();

        patternLayoutEncoder.setPattern("[%d{HH:mm:ss}] [%class{20}$%method] %msg%n");
        patternLayoutEncoder.setContext(loggerContext);
        patternLayoutEncoder.start();
        var textAreaLogAppender = new TextAreaLogAppender(console);
        textAreaLogAppender.setContext(loggerContext);
        textAreaLogAppender.start();

        ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) ClairvoyanceLogger.logger;
        logger.addAppender(textAreaLogAppender);
        logger.setLevel(Level.INFO);
        logger.setAdditive(true);
    }

    private void updateTreeView(IAerospikeClient client) {
        var rootNode = createRootModelNode(ApplicationModel.INSTANCE.getConnectionInfo());
        if (namespacesTree.getRoot() == null) {
            var treeRootView = new TreeItem<>(rootNode, new ImageView(rootIcon));
            treeRootView.setExpanded(true);
            namespacesTree.setRoot(treeRootView);
        } else {
            namespacesTree.getRoot().setValue(rootNode);
        }
        buildNodes(client);
    }

    private void buildNodes(IAerospikeClient client) {
        for (Node node : client.getNodes()) {
            buildNode(node);
        }
    }

    private void buildNode(Node node) {
        var nodeInfo = nodeInfoMapper.getNodeInfo(node);
        var nodeViewNodeResult = findViewNodeInChildren(nodeInfo.getId(), namespacesTree.getRoot());
        if (nodeViewNodeResult.isPresent()) {
            var nodeModelNode = createNodeModelNode(nodeInfo);
            var nodeViewNode = nodeViewNodeResult.get();
            nodeViewNode.setValue(nodeModelNode);

            buildNamespaces(nodeInfo, nodeViewNode);
        } else {
            var nodeModelNode = createNodeModelNode(nodeInfo);
            var nodeViewNode = new TreeItem<>(nodeModelNode, new ImageView(nodeIcon));
            nodeViewNode.setExpanded(true);
            namespacesTree.getRoot().getChildren().add(nodeViewNode);

            buildNamespaces(nodeInfo, nodeViewNode);
        }
    }

    private void buildNamespaces(NodeInfo nodeInfo, TreeItem<SimpleTreeNode> parent) {
        for (NamespaceInfo namespace : nodeInfo.getNamespaces()) {
            buildNamespaceNode(parent, namespace);
        }
    }

    private void buildNamespaceNode(TreeItem<SimpleTreeNode> parent, NamespaceInfo namespace) {
        var namespaceViewNodeResult = findViewNodeInChildren(namespace.getId(), parent);
        var namespaceModelNode = createNamespaceModelNode(namespace);
        if (namespaceViewNodeResult.isPresent()) {
            var namespaceViewNode = namespaceViewNodeResult.get();
            namespaceViewNode.setValue(namespaceModelNode);

            buildSets(namespace, namespaceViewNode);
        } else {
            var namespaceViewNode = new TreeItem<>(namespaceModelNode, new ImageView(namespaceIcon));
            namespaceViewNode.setExpanded(true);
            parent.getChildren().add(namespaceViewNode);

            buildSets(namespace, namespaceViewNode);
        }
    }

    private void buildSets(NamespaceInfo namespace, TreeItem<SimpleTreeNode> namespaceNode) {
        for (SetInfo setInfo : namespace.getSets()) {
            buildSetNode(namespaceNode, setInfo);
        }
    }

    private void buildSetNode(TreeItem<SimpleTreeNode> namespaceNode, SetInfo setInfo) {
        var setViewNodeResult = findViewNodeInChildren(setInfo.getId(), namespaceNode);
        var setModelNode = createSetModelNode(setInfo);
        if (setViewNodeResult.isPresent()) {
            var setViewNode = setViewNodeResult.get();
            setViewNode.setValue(setModelNode);
        } else {
            var setViewNode = new TreeItem<>(setModelNode, new ImageView(setIcon));
            namespaceNode.getChildren().add(setViewNode);
        }
    }

    private Optional<TreeItem<SimpleTreeNode>> findViewNodeInChildren(String itemId, TreeItem<SimpleTreeNode> parent) {
        for (var namespacesTreeNode : parent.getChildren()) {
            if (namespacesTreeNode.getValue().getValue().getId().equals(itemId)) {
                return Optional.of(namespacesTreeNode);
            }
        }
        return Optional.empty();
    }

    private SimpleTreeNode createRootModelNode(ConnectionInfo connectionInfo) {
        var connectionString = connectionInfo.toString();
        return new SimpleTreeNode(
                connectionString,
                new RootInfo(connectionString)
        );
    }

    private SimpleTreeNode createNodeModelNode(NodeInfo nodeInfo) {
        return new SimpleTreeNode(nodeInfo.getName(), nodeInfo);
    }

    private SimpleTreeNode createNamespaceModelNode(NamespaceInfo namespaceInfo) {
        return new SimpleTreeNode(namespaceInfo.getName(), namespaceInfo);
    }

    private SimpleTreeNode createSetModelNode(SetInfo setInfo) {
        return new SimpleTreeNode(
                String.format("%s [count: %s]", setInfo.name, numberFormat.format(setInfo.objectCount)),
                setInfo
        );
    }

    private Optional<Tab> getTab(String tabId) {
        for (var tab : tabs.getTabs()) {
            if (tab.getId() != null && tab.getId().equals(tabId)) {
                return Optional.of(tab);
            }
        }
        return Optional.empty();
    }

    private static void createNewClient() {
        var aerospikeClientResult = ApplicationModel.INSTANCE.createNewAerospikeClient();
        if (aerospikeClientResult.hasError()) {
            throw new AerospikeException(aerospikeClientResult.getError());
        }
    }

}
