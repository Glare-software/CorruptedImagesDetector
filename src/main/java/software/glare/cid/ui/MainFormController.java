package software.glare.cid.ui;

import com.sun.javafx.collections.ObservableListWrapper;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Duration;
import javafx.util.StringConverter;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.math3.util.Precision;
import org.controlsfx.control.HyperlinkLabel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.glare.cid.FileType;
import software.glare.cid.Status;
import software.glare.cid.process.BasicReportImpl;
import software.glare.cid.process.ProgressData;
import software.glare.cid.process.Report;
import software.glare.cid.process.ScanPerformer;
import software.glare.cid.process.processes.processor.algorithm.ByteAsImagesProcessAlgorithm;
import software.glare.cid.process.processes.processor.result.BytesProcessResult;

import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Created by fdman on 15.03.2015.
 */
public class MainFormController {
    private final Logger log = LoggerFactory.getLogger(MainFormController.class);
    private final MainForm mainForm;
    private FormConfigController formConfigController;
    private FormConfig formConfig;
    private ScanBtnEventHandler scanBtnEventHandler;
    private ResultsTreePostProcessor<TreeItem<BytesProcessResult>, BytesProcessResult> resultsTreePostProcessor;
    private Configuration freeMarkerCfg;
    private Stage stage;
    private ShowHelpEventHandler showHelpEventEventHandler = new ShowHelpEventHandler();
    private ShowAboutEventHandler showAboutEventEventHandler = new ShowAboutEventHandler();
    private HideAboutStackPaneEventHandler hideAboutStackPaneEventHandler = new HideAboutStackPaneEventHandler();
    private HideAboutEventHandler hideAboutEventHandler = new HideAboutEventHandler();
    private HideHelpStackPaneEventHandler hideHelpStackPaneEventHandler = new HideHelpStackPaneEventHandler();
    private HideHelpEventHandler hideHelpEventHandler = new HideHelpEventHandler();

    public MainFormController(Stage stage) throws IOException, URISyntaxException {
        this.stage = stage;
        this.mainForm = new MainForm();
        this.scanBtnEventHandler = new ScanBtnEventHandler();
        createOrRestoreFormConfig();
        setupStage();
        setupFreeMarker();
        setupOverlayPanesBehaviour();
        setupMoveRenameBtnBehaviour();
        setupMainTreeTableBehaviour();
        setupFilterComboboxesBehaviour();
        setupScanBtnBehavior();
        setupFileTypeCheckBoxesBehavior();
        setupFolderTextFieldBehavior();
        setupFileChooserBehavior();
        setupHelpFormBtnsBehaviour();
    }

    private void createOrRestoreFormConfig() {
        FormConfig formConfig = null;
        formConfigController = new FormConfigController();
        if (formConfigController.isBackingStoreAvailable()) {
            formConfig = formConfigController.restore();
        }
        this.formConfig = (formConfig == null ? new FormConfig() : formConfig);
    }

    private void setupStage() {
        stage.setTitle(UIConstants.MAIN_TITLE);
        stage.getIcons().add(new javafx.scene.image.Image("icons/com.iconfinder/tango-icon-library/1415555653_folder-saved-search-32.png"));
        stage.setIconified(true);
        this.stage.setOnCloseRequest(event -> {
            //TODO some checks
            formConfigController.save(formConfig);
        });
    }

    private void setupFreeMarker() throws IOException, URISyntaxException {
        freeMarkerCfg = new Configuration(Configuration.VERSION_2_3_21);
        freeMarkerCfg.setClassForTemplateLoading(getClass(), "/freemarker/");
        freeMarkerCfg.setDefaultEncoding("UTF-8");
        freeMarkerCfg.setTemplateExceptionHandler(TemplateExceptionHandler.DEBUG_HANDLER);
    }

    private void setupOverlayPanesBehaviour() {
        //show about pane
        mainForm.aboutMenuItem.setOnAction(showAboutEventEventHandler);
        //hide about pane
        mainForm.aboutStackPane.setOnMouseClicked(event -> hideAboutEventHandler.hide());

        //show help pane
        mainForm.helpMenuItem.setOnAction(showHelpEventEventHandler);
        //hide help pane
        mainForm.helpStackPane.setOnMouseClicked(event -> hideHelpEventHandler.hide());
    }

    private void setupMoveRenameBtnBehaviour() {
        mainForm.moveRenameBtn.setOnAction(new MoveRenameBtnEventHandler());
    }

    private void setupMainTreeTableBehaviour() {
        mainForm.treeTableView.setRowFactory(new Callback<TreeTableView, TreeTableRow>() {
            @Override
            public TreeTableRow call(TreeTableView param) {

                TreeTableRow<BytesProcessResult> treeTableRow = new TreeTableRow<BytesProcessResult>() {
                    @Override
                    protected void updateItem(BytesProcessResult item, boolean empty) {
                        super.updateItem(item, empty);
                        setDisclosureNode(null);
                        getStyleClass().clear();
                        getStyleClass().add("my-tree-table-row-text-fill");
                        if (item != null) {
                            if (!StringUtils.isEmpty(item.getStatus().toString())) {
                                //getStyleClass().add("tree-table-row-bckgnd-with-status");
                                getStyleClass().add("tree-table-row-bckgnd-" + item.getStatus().toString().toLowerCase());
                            } else {
                                getStyleClass().add("tree-table-row-bckgnd-empty");
                                setId(null);
                            }
                        } else {
                            getStyleClass().add("tree-table-row-bckgnd-empty");
                            setId(null);
                        }
                    }
                };

                treeTableRow.setOnMouseClicked(new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent event) {
                        //row event handled first. select it after click
                        if (!event.isConsumed() && (event).getButton() == MouseButton.PRIMARY) {
                            TreeItem selectedItem = ((TreeTableRow) event.getSource()).getTreeItem();
                            if (selectedItem != null) {
                                mainForm.treeTableView.getSelectionModel().select(selectedItem);
                                BytesProcessResult processResult = (BytesProcessResult) selectedItem.getValue();
                                if (processResult != null) {
                                    if (!processResult.isLeaf()) {
                                        //expand/collapse folder
                                        selectedItem.setExpanded(!selectedItem.isExpanded());
                                    } else if (processResult.isLeaf() && event.getClickCount() >= 2 && processResult.getPath() != null) {
                                        openFileOrItsFolder(processResult);
                                    }
                                }
                            }

                        }
                    }

                    private void openFileOrItsFolder(BytesProcessResult processResult) {
                        File f = processResult.getPath().toFile();
                        if (f.exists()) {
                            if (Desktop.isDesktopSupported()) {
                                try {
                                    Desktop.getDesktop().browse(f.toURI());
                                } catch (IOException e) {
                                    log.warn("Can`t read file. Exception: {}", ExceptionUtils.getMessage(e));
                                    try {
                                        Desktop.getDesktop().browse(f.getParentFile().toURI());
                                    } catch (IOException e1) {
                                        log.error("Can`t read parent file. Exception: {}", ExceptionUtils.getStackTrace(e));
                                    }
                                }
                            } else {
                                log.warn("Desktop browsing is not supported by your OS JVM :(. Can`t open {} ", processResult.getPath().toString());
                            }
                        } else {
                            log.warn("File {} is not found :(", f.getAbsolutePath());
                        }
                    }
                });


                easterEgg(treeTableRow);

                return treeTableRow;
            }

            private void easterEgg(TreeTableRow<BytesProcessResult> treeTableRow) {
                Calendar calendar = Calendar.getInstance();
                if (calendar.get(Calendar.DATE) == 1 && calendar.get(Calendar.MONTH) == 3) //01.04 easter egg
                {
                    treeTableRow.setRotate((Math.random() - 0.5) * 3);
                }
            }
        });

    }

    private void setupFilterComboboxesBehaviour() {
        mainForm.clauseFilterComboBox.setConverter(new StringConverter<Clause>() {
            @Override
            public String toString(Clause object) {
                switch (object) {
                    case EQUAL_OR_STRONGER:
                        return "equal or stronger than";
                    case EQUAL:
                        return "equal to";
                    default:
                        return null;
                }
            }

            @Override
            public Clause fromString(String string) {
                return Clause.valueOf(string);
            }
        });

        mainForm.statusFilterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.getPriority() <= Status.SKIPPED.getPriority()) {
                mainForm.moveRenameInfoLabel.setText("Please, select more meaningful status than SKIPPED for activating 'Rename' button");
                mainForm.moveRenameBtn.setDisable(true);
            } else {
                mainForm.moveRenameBtn.setDisable(false);
                mainForm.moveRenameInfoLabel.setText("");
            }
            if (resultsTreePostProcessor != null) {
                refreshTreeTableView();
            }

        });
        mainForm.clauseFilterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (resultsTreePostProcessor != null) {
                refreshTreeTableView();
            }
        });
    }

    private void setupScanBtnBehavior() {
        File folder = new File(mainForm.folderPath.getText());
        if (folder.exists() && folder.isDirectory()) {
            mainForm.scanBtn.setDisable(false);
        } else {
            mainForm.scanBtn.setDisable(true);
        }
        mainForm.scanBtn.setOnAction(scanBtnEventHandler);
    }

    private void setupHelpFormBtnsBehaviour() {
        mainForm.helpNextBtn.setOnAction(showHelpEventEventHandler);
        mainForm.helpPrevBtn.setOnAction(showHelpEventEventHandler);
        mainForm.helpCloseBtn.setOnAction(hideHelpEventHandler);
    }

    private void setupFileTypeCheckBoxesBehavior() {
        mainForm.jpgCheckBox.selectedProperty().bindBidirectional(formConfig.jpgCheckBoxSelectedStateProperty());
        mainForm.gifCheckBox.selectedProperty().bindBidirectional(formConfig.gifCheckBoxSelectedStateProperty());
        mainForm.nefCheckBox.selectedProperty().bindBidirectional(formConfig.nefCheckBoxSelectedStateProperty());
        mainForm.cidCheckBox.selectedProperty().bindBidirectional(formConfig.bidCheckBoxSelectedStateProperty());
    }

    private void setupFolderTextFieldBehavior() {

        mainForm.folderPath.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && checkIsValidFolder(new File(newValue))) {
                mainForm.scanBtn.setDisable(false);
            } else {
                mainForm.scanBtn.setDisable(true);
            }
        });
        mainForm.folderPath.textProperty().bindBidirectional(formConfig.getFolderPathProperty());
    }

    private void setupFileChooserBehavior() {
        mainForm.directoryChooser.setTitle("Select folder to scan");
        mainForm.selectPathBtn.setOnAction(event -> {
            //update init directory
            if (mainForm.folderPath.getText() != null) {
                File prevSelectedFolder = new File(mainForm.folderPath.getText());
                if (checkIsValidFolder(prevSelectedFolder)) {
                    mainForm.directoryChooser.setInitialDirectory(prevSelectedFolder);
                }
            }
            //show and check selected
            File selectedFolder = mainForm.directoryChooser.showDialog(stage);

            if (selectedFolder != null) {
                if (checkIsValidFolder(selectedFolder)) {
                    mainForm.folderPath.setText(selectedFolder.getAbsolutePath());
                } else {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setHeaderText(null);
                    alert.setContentText("Please, select a valid folder.\nYou must to have read permissions on it.");
                    alert.show();
                }
            }
        });
    }

    private boolean checkIsValidFolder(File folder) {
        if (folder != null && folder.exists() && folder.isDirectory() && folder.canRead()) {
            return true;
        }
        return false;
    }

    private Set<FileType> getSelectedFileTypes() {
        Set<FileType> result = new HashSet<>();
        if (mainForm.jpgCheckBox.isSelected()) {
            result.add(FileType.JPG);
        }
        if (mainForm.gifCheckBox.isSelected()) {
            result.add(FileType.GIF);
        }
        if (mainForm.nefCheckBox.isSelected()) {
            result.add(FileType.NEF);
        }
        if (mainForm.cidCheckBox.isSelected()) {
            result.add(FileType.CID);
        }
        return result;
    }

    private void refreshTreeTableView() {
        if (resultsTreePostProcessor != null) {
            TreeItem<BytesProcessResult> tmpForViewRoot = resultsTreePostProcessor.cloneTree(resultsTreePostProcessor.getRoot());
            resultsTreePostProcessor.sortAndFilterTree(tmpForViewRoot,
                    bytesProcessResultTreeItem -> bytesProcessResultTreeItem.getChildren().sort((o1, o2) -> {
                        if (o1.isLeaf() && !o2.isLeaf()) {
                            return 1;
                        } else if (!o1.isLeaf() && o2.isLeaf()) {
                            return -1;
                        } else {
                            return o1.getValue().getPath().compareTo(o2.getValue().getPath());
                        }
                    }),
                    bytesProcessResultTreeItem -> {
                        if (!bytesProcessResultTreeItem.getChildren().isEmpty()) {
                            return false; //don`t filter if contains children
                        }
                        if (mainForm.clauseFilterComboBox.getSelectionModel().getSelectedItem().equals(Clause.EQUAL)) {
                            bytesProcessResultTreeItem.getValue().getStatus().equals(mainForm.statusFilterComboBox.getSelectionModel().getSelectedItem());
                            if (!bytesProcessResultTreeItem.getValue().getStatus().equals(mainForm.statusFilterComboBox.getSelectionModel().getSelectedItem())) {
                                //log.trace("FILTERED by equality| bytesProcessResultTreeItem.getValue().getStatus() {} mainForm.statusFilterComboBox.getSelectionModel().getSelectedItem() {}", bytesProcessResultTreeItem.getValue().getStatus(), mainForm.statusFilterComboBox.getSelectionModel().getSelectedItem());
                                return true;
                            }
                        } else if (mainForm.clauseFilterComboBox.getSelectionModel().getSelectedItem().equals(Clause.EQUAL_OR_STRONGER)) {
                            if (bytesProcessResultTreeItem.getValue().getStatus().getPriority() < mainForm.statusFilterComboBox.getSelectionModel().getSelectedItem().getPriority()) {
                                //log.trace("FILTERED by priority| bytesProcessResultTreeItem.getValue().getStatus() {} mainForm.statusFilterComboBox.getSelectionModel().getSelectedItem() {}", bytesProcessResultTreeItem.getValue().getStatus(), mainForm.statusFilterComboBox.getSelectionModel().getSelectedItem());
                                return true;
                            }
                        }
                        return false;
                    });
            tmpForViewRoot.setExpanded(true);
            mainForm.treeTableView.setRoot(tmpForViewRoot);
            resultsTreePostProcessor.setFoldersInfo(tmpForViewRoot);
        }

    }

    public MainForm getMainForm() {
        return mainForm;
    }

    private class ScanBtnEventHandler implements EventHandler<ActionEvent> {
        private final Logger log = LoggerFactory
                .getLogger(ScanBtnEventHandler.class);
        private ScanPerformer scanPerformer;
        private volatile boolean scanning = false;

        @Override
        public void handle(ActionEvent event) {
            if (scanning) {
                scanPerformer.pauseScan();
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Confirm");
                    alert.setHeaderText(null);
                    alert.setContentText("Scan is in progress. Cancel?");

                    ButtonType buttonTypeYes = new ButtonType("Yes");
                    ButtonType buttonTypeCancel = new ButtonType("No, continue scanning", ButtonBar.ButtonData.CANCEL_CLOSE);

                    alert.getButtonTypes().setAll(buttonTypeYes, buttonTypeCancel);
                    Optional<ButtonType> result = alert.showAndWait();
                    if (result.get() == buttonTypeYes) {
                        scanPerformer.cancelScan();
                        setUIDisabled(false);
                        scanning = false;
                    } else {

                        scanPerformer.unpauseScan();
                        scanning = true;
                    }
                });
            } else {
                if (!mainForm.nefCheckBox.isSelected() &&
                        !mainForm.jpgCheckBox.isSelected() &&
                        !mainForm.gifCheckBox.isSelected() &&
                        !mainForm.cidCheckBox.isSelected()
                        ) {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setHeaderText(null);
                    alert.setContentText("Select at least one extension");
                    alert.show();
                    return;
                }
                scanning = true;
                Report report = new BasicReportImpl();
                Platform.runLater(() -> setUIDisabled(true));

                scanPerformer = new ScanPerformer(mainForm.folderPath.getText(),
                        getSelectedFileTypes(),
                        ByteAsImagesProcessAlgorithm.class,
                        report,
                        (aVoid) -> {
                            log.info("Scan finished");
                            scanning = false;
                            Platform.runLater(() -> {
                                setNewResultTreePostProcessor(report);
                                refreshTreeTableView();
                                setUIDisabled(false);
                                mainForm.progressBar.setProgress(1);
                                mainForm.statusBarText.setText("Scanning of \'" + mainForm.folderPath.getText() + "\' completed");
                            });
                        },
                        (aVoid) -> {
                            log.info("Scan cancelled");
                            scanning = false;
                            Platform.runLater(() -> {
                                setUIDisabled(false);
                                setNewResultTreePostProcessor(report);
                                refreshTreeTableView();
                                if (mainForm.progressBar.getProgress() < 0) {
                                    mainForm.progressBar.setProgress(Double.MIN_NORMAL);
                                }
                                mainForm.statusBarText.setText("Scan cancelled");
                            });
                        },
                        new Consumer<ProgressData>() {
                            //tricky hack with progress calculations
                            private double maxProgressValue = -1d;

                            @Override
                            public void accept(ProgressData aProgressData) {
                                Platform.runLater(() -> {
                                    maxProgressValue = Math.max(aProgressData.getTotal(), maxProgressValue);
                                    double progress = Precision.round(((maxProgressValue - aProgressData.getTotal()) / maxProgressValue), 2);
                                    if (!Double.isNaN(progress) && progress > 0) {
                                        mainForm.progressBar.setProgress(progress + 0.05);
                                    } else {
                                        mainForm.progressBar.setProgress(-1);
                                        mainForm.statusBarText.setText("Calculating...");
                                    }
                                    mainForm.statusBarText.setText(aProgressData.getInfo());
                                });
                            }
                        }
                );
                scanPerformer.performScan();
                mainForm.progressBar.setProgress(Double.MIN_NORMAL);
            }
        }

        private void setUIDisabled(boolean disable) {
            if (disable) {
                mainForm.scanBtn.setText("Cancel scan");
            } else {
                mainForm.scanBtn.setText("Start scan");
            }
            mainForm.folderPath.setDisable(disable);
            mainForm.selectPathBtn.setDisable(disable);
            mainForm.gifCheckBox.setDisable(disable);
            mainForm.cidCheckBox.setDisable(disable);
            mainForm.jpgCheckBox.setDisable(disable);
            mainForm.nefCheckBox.setDisable(disable);
            mainForm.clauseFilterComboBox.setDisable(disable);
            mainForm.statusFilterComboBox.setDisable(disable);
            mainForm.moveRenameInfoLabel.setVisible(!disable);
            mainForm.moveRenameBtn.setVisible(!disable);
        }


        private void setNewResultTreePostProcessor(Report report) {
            java.util.List<BytesProcessResult> copiedReportLines = new ArrayList<>(report.getLines());//prevent concurrent exception if some thread will not stop for some reason
            log.info("Files scanned: {}", copiedReportLines.size());
            ResultsTreeBuilder resultsTreeBuilder = new ResultsTreeBuilder(mainForm.folderPath.getText());
            TreeItem treeItem = resultsTreeBuilder.generateTree(copiedReportLines);
            resultsTreePostProcessor = new ResultsTreePostProcessor<>(treeItem);

        }
    }

    private class MoveRenameBtnEventHandler implements EventHandler<ActionEvent> {

        private java.util.List<String> renamedTotalFilesList;
        private java.util.List<String> notRenamedTotalFilesList;
        private TreeItem<BytesProcessResult> root;
        private LocalDateTime localDateTime;
        private String dateTimeForReportFileName;
        private String dateTimeForReportText;
        private String totalReportName;

        @Override
        public void handle(ActionEvent event) {
            if (mainForm.treeTableView.getRoot() == null) {
                Alert alertOops = new Alert(Alert.AlertType.INFORMATION);
                alertOops.setTitle("Ooops!");
                alertOops.setHeaderText(null);
                alertOops.setContentText("Please, do scan before");
                alertOops.show();
                return;
            }

            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Please, confirm");
            confirmAlert.setHeaderText(null);
            confirmAlert.setContentText("All not *.cid files at the table will be renamed with \"<status>.cid\" postfix. Continue?");
            ButtonType buttonTypeOne = new ButtonType("Yes");
            ButtonType buttonTypeCancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
            confirmAlert.getButtonTypes().setAll(buttonTypeOne, buttonTypeCancel);

            Optional<ButtonType> result = confirmAlert.showAndWait();
            if (result.get() == buttonTypeOne) {
                localDateTime = LocalDateTime.now();
                dateTimeForReportFileName = DateTimeFormatter.ofPattern("YYYYMMdd_HHmmss").format(localDateTime);
                dateTimeForReportText = DateTimeFormatter.ofPattern("dd MMM YYYY HH:mm:ss").format(localDateTime);
                root = mainForm.treeTableView.getRoot();
                totalReportName = root.getValue().getPath().toAbsolutePath().toString() + File.separator
                        + "~CID total report "
                        + dateTimeForReportFileName
                        + ".txt";
                renamedTotalFilesList = new ArrayList<>();
                notRenamedTotalFilesList = new ArrayList<>();
                iterateTreeAndRename(root);
                makeTotalReportAndStore();
                refreshTreeTableView();
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setHeaderText(null);
                    alert.setContentText(renamedTotalFilesList.size() > 0 ? renamedTotalFilesList.size() + " files was renamed" : "Files were not renamed");
                    alert.setTitle(renamedTotalFilesList.size() > 0 ? "Renamed files list" : "Information");
                    alert.show();
                });


            } else {
                // ... user chose CANCEL or closed the dialog
            }

        }

        private void iterateTreeAndRename(final TreeItem<BytesProcessResult> parentTreeItem) {
            ObservableList<TreeItem<BytesProcessResult>> children = parentTreeItem.getChildren();
            java.util.List<String> renamedAtCurrentLevelFilesList = new ArrayList<>();
            for (TreeItem<BytesProcessResult> childItem : children) {
                iterateTreeAndRename(childItem);

                BytesProcessResult bytesProcessResult = childItem.getValue();
                String renamedFileName = getNewFileName(bytesProcessResult);
                if (isNeedToRename(bytesProcessResult) && renameFile(bytesProcessResult.getPath().toFile(), renamedFileName)) {
                    childItem.getValue().setPath(new File(renamedFileName).toPath());
                    renamedAtCurrentLevelFilesList.add(renamedFileName);
                    renamedTotalFilesList.add(bytesProcessResult.getPath().toAbsolutePath().toString() + " renamed to " + FilenameUtils.getName(renamedFileName));
                    log.trace("{} renamed to {}", bytesProcessResult.getPath().toAbsolutePath().toString(), FilenameUtils.getName(renamedFileName));
                } else {
                    if (bytesProcessResult.getStatus() != Status.FOLDER) {
                        notRenamedTotalFilesList.add(bytesProcessResult.getPath().toAbsolutePath().toString() + " was not renamed");
                        log.trace("{} was not renamed", bytesProcessResult.getPath().toAbsolutePath().toString());
                    }
                }
            }
            makeFolderReportAndStore(renamedAtCurrentLevelFilesList, parentTreeItem.getValue().getPath().toAbsolutePath().toString());
        }

        private void makeFolderReportAndStore(java.util.List<String> renamedFilesList, String reportFolder) {
            if (renamedFilesList.size() > 0) {
                Template template;
                try {
                    template = freeMarkerCfg.getTemplate("folderReportEn.ftl");
                } catch (IOException e) {
                    log.error("{}", ExceptionUtils.getStackTrace(e));
                    return;
                }

                Map<String, Object> data = new HashMap<>();
                data.put("appVersionTitle", UIConstants.MAIN_TITLE);
                data.put("currentDir", reportFolder);
                data.put("dateTime", "" + dateTimeForReportText);

                data.put("renamingInfo", notRenamedTotalFilesList.size() > 0 ? "Renamed " + renamedTotalFilesList.size() + " file(-s) at " + dateTimeForReportText : "");
                data.put("files", renamedFilesList);

                data.put("fullReportPathAndName", totalReportName);

                String fileName = reportFolder + File.separator
                        + "~CID report "
                        + this.dateTimeForReportFileName
                        + ".txt";
                processFreeMarkerTemplate(template,
                        fileName, data);
            }

        }

        private void makeTotalReportAndStore() {
            if (renamedTotalFilesList.size() > 0 || notRenamedTotalFilesList.size() > 0) {
                Template template;
                try {
                    template = freeMarkerCfg.getTemplate("totalReportEn.ftl");
                } catch (IOException e) {
                    log.error("{}", ExceptionUtils.getStackTrace(e));
                    return;
                }

                Map<String, Object> data = new HashMap<>();
                data.put("appVersionTitle", UIConstants.MAIN_TITLE);
                data.put("dateTime", dateTimeForReportText);
                data.put("rootDir", root.getValue().getPath().toAbsolutePath());

                data.put("renamingInfo", renamedTotalFilesList.size() > 0 ? "Renamed " + renamedTotalFilesList.size() + " file(-s) at " + dateTimeForReportText : "");
                data.put("okFiles", renamedTotalFilesList);

                data.put("renamingErrors", notRenamedTotalFilesList.size() > 0 ? notRenamedTotalFilesList.size() + " files were not renamed" : "");
                data.put("errFiles", notRenamedTotalFilesList);

                processFreeMarkerTemplate(template,
                        totalReportName, data);
            }

        }

        private void processFreeMarkerTemplate(Template template, String fileName, Map<String, Object> data) {
            try (Writer fileWriter = new FileWriter(new File(fileName));) {
                template.process(data, fileWriter);
                fileWriter.flush();
            } catch (IOException | TemplateException e) {
                log.error("{}", ExceptionUtils.getStackTrace(e));
            }
        }

        private boolean isNeedToRename(BytesProcessResult processResult) {
            if (processResult.isLeaf()) {
                File file = processResult.getPath().toFile();
                if (file.exists() && !file.isDirectory() && !FilenameUtils.getExtension(file.getName()).toUpperCase().equals("CID")) {
                    return true;
                }
            }
            return false;
        }

        private String getNewFileName(BytesProcessResult processResult) {
            return processResult.getPath().toFile().getAbsolutePath() + "." + processResult.getStatus().toString().toLowerCase() + ".cid";
        }

        private boolean renameFile(File oldFile, String newFileName) {
            try {
                oldFile.renameTo(new File(newFileName));
                return true;
            } catch (Exception e) {
                log.error("Renaming of {} exception {}", oldFile.getAbsoluteFile().toString(), ExceptionUtils.getStackTrace(e));
            }
            return false;
        }
    }

    class MainForm {
        private final Button scanBtn = new Button("Start scan");
        private final CheckBox jpgCheckBox = new CheckBox(FileType.JPG.getExtensions()[0]);
        private final CheckBox gifCheckBox = new CheckBox(FileType.GIF.getExtensions()[0]);
        private final CheckBox nefCheckBox = new CheckBox(FileType.NEF.getExtensions()[0]);
        private final CheckBox cidCheckBox = new CheckBox(FileType.CID.getExtensions()[0]);
        private final TextField folderPath = new TextField();
        private final Button selectPathBtn = new Button("...");
        private final DirectoryChooser directoryChooser = new DirectoryChooser(); //use file chooser cause it is more flexible (show files in folders)
        private final ComboBox<Status> statusFilterComboBox = new ComboBox<>(FXCollections.observableArrayList(getStatusFilterComboboxItems()));
        private final ComboBox<Clause> clauseFilterComboBox = new ComboBox<>(new ObservableListWrapper<>(Arrays.asList(Clause.values())));
        private final Button moveRenameBtn = new Button("Rename...");
        private final TreeTableView treeTableView = new TreeTableView();
        private final Label moveRenameInfoLabel = new Label("Please, select more meaningful status than SKIPPED for activating 'Rename' button");
        private final Label statusBarText = new Label("Select folder and press scan button");
        private final ProgressBar progressBar = new ProgressBar(Double.MIN_NORMAL);
        private final Label progressBarProgress = new Label("0%");
        private final MenuBar menuBar = new MenuBar();
        private final Menu menuHelp = new Menu("Help");
        private final MenuItem helpMenuItem = new MenuItem("Help", new ImageView("icons/com.iconfinder/tango-icon-library/1423615094_help-browser-16.png"));
        private final MenuItem aboutMenuItem = new MenuItem("About", new ImageView("icons/com.iconfinder/tango-icon-library/1423615052_contact-new-16.png"));
        private final StackPane aboutStackPane = new StackPane();
        private final StackPane helpArrowsStackPane = new StackPane();
        private final StackPane helpStackPane = new StackPane();
        private final TextArea helpNoteTextArea = new TextArea();
        private final Button helpPrevBtn = new Button("<<");
        private final Button helpNextBtn = new Button(">>");
        private final Button helpCloseBtn = new Button("Close help");
        private final Label aboutHeaderLabel = new Label(UIConstants.MAIN_TITLE);
        private final HyperlinkLabel aboutLabel = new HyperlinkLabel(
                "Glare Software™ team:\n" +
                        "Author - Dmitry Fedorchenko\n" +
                        "QA Engineer - Tatiana Fedorchenko\n\n" +
                        "Source code:\n[at GitHub]\n\n" +
                        "License:\n[GNU GENERAL PUBLIC LICENSE Version 3]");
        private final Text aboutThanksToText = new Text(("Logback\n\n" +
                "Commons Collections\n\n" +
                "Commons IO\n\n" +
                "Commons Lang\n\n" +
                "Commons Math\n\n" +
                "Commons Pool\n\n" +
                "ControlsFX\n\n" +
                "FreeMarker\n\n" +
                "jrawio\n\n" +
                "SLF4J"));
        private final Hyperlink aboutFooterLabel = new Hyperlink("Contact us");
        private final TranslateTransition aboutThanksToTransition = new TranslateTransition(new Duration(3200), aboutThanksToText);
        private ScrollPane thanksToScrollPane = new ScrollPane(aboutThanksToText);
        private StackPane aboutMaskStackPane = new StackPane();
        private StackPane mainStackPane = new StackPane();
        private HBox extensionsHbox;
        private Label extensionsLbl;

        private MainForm() {
            initMenu();
            initMainComponents();
            initAboutComponents();
            initHelpComponents();
            createMainContent();
            createAboutContent();
            createHelpContent();
        }

        private void createHelpContent() {
            GridPane helpGrid = new GridPane();
            helpGrid.setMaxSize(400, 100);
            helpGrid.setStyle("-fx-background-color: transparent; -fx-effect: dropshadow(two-pass-box, #eddeb7, 7, 0.2, 4, 4);");
            setupGridParams(helpGrid, 5);


            helpGrid.add(helpNoteTextArea, 0, 0, 3, 1);
            helpGrid.add(helpPrevBtn, 0, 1, 1, 1);
            helpGrid.add(helpNextBtn, 1, 1, 1, 1);
            //helpGrid.add(helpCloseBtn, 2, 1, 1, 1);

            helpGrid.getColumnConstraints().addAll(
                    new ColumnConstraints(-1, -1, -1, Priority.NEVER, HPos.LEFT, false),
                    new ColumnConstraints(-1, -1, -1, Priority.NEVER, HPos.LEFT, false),
                    new ColumnConstraints(-1, -1, -1, Priority.ALWAYS, HPos.RIGHT, false)
            );
            helpGrid.getRowConstraints().addAll(
                    new RowConstraints(-1, -1, -1, Priority.NEVER, VPos.TOP, true),
                    new RowConstraints(-1, -1, -1, Priority.NEVER, VPos.BOTTOM, false)
            );


            helpStackPane.getChildren().add(helpArrowsStackPane);
            helpStackPane.getChildren().add(helpGrid);
            helpStackPane.setVisible(false);
            mainStackPane.getChildren().add(helpStackPane);
            //helpStackPane content created dynamically every time
        }

        private void createAboutContent() {
            GridPane aboutGrid = new GridPane();
            aboutGrid.setMaxSize(400, 100);
            aboutGrid.setStyle("-fx-background-color: beige; -fx-effect: dropshadow(two-pass-box, #eddeb7, 7, 0.2, 4, 4);");
            setupGridParams(aboutGrid, 16);
            aboutThanksToText.setStyle("-fx-text-alignment: center;");
            HBox headerWrapper = new HBox(aboutHeaderLabel);
            headerWrapper.setAlignment(Pos.CENTER);
            HBox footerWrapper = new HBox(aboutFooterLabel);
            footerWrapper.setAlignment(Pos.CENTER);
            thanksToScrollPane.getStyleClass().addAll("transparentAboutScrollPane");
            VBox overlayedBackgroundVBox = new VBox();
            overlayedBackgroundVBox.setStyle("-fx-background-color: linear-gradient(beige, transparent, beige);");
            aboutMaskStackPane.getChildren().addAll(thanksToScrollPane, overlayedBackgroundVBox);
            aboutGrid.add(headerWrapper, 0, 0, 2, 1);
            aboutGrid.add(aboutLabel, 0, 1, 1, 2);
            aboutGrid.add(new Label("Thanks to 3d party software:"), 1, 1, 1, 1);
            aboutGrid.add(aboutMaskStackPane, 1, 2, 1, 1);
            aboutGrid.add(footerWrapper, 0, 3, 2, 1);
            aboutGrid.getColumnConstraints().addAll(
                    new ColumnConstraints(-1, -1, -1, Priority.ALWAYS, HPos.CENTER, true),
                    new ColumnConstraints(-1, -1, -1, Priority.NEVER, HPos.CENTER, true)
            );
            aboutGrid.getRowConstraints().addAll(
                    new RowConstraints(-1, -1, -1, Priority.NEVER, VPos.TOP, false),
                    new RowConstraints(-1, -1, -1, Priority.ALWAYS, VPos.TOP, false),
                    new RowConstraints(-1, -1, -1, Priority.NEVER, VPos.TOP, false),
                    new RowConstraints(-1, -1, -1, Priority.NEVER, VPos.BOTTOM, false)
            );

            aboutStackPane.getChildren().add(aboutGrid);
            aboutStackPane.setVisible(false);
            mainStackPane.getChildren().add(aboutStackPane);
        }

        private void initMenu() {
            menuHelp.getItems().addAll(helpMenuItem, aboutMenuItem);
            menuBar.getMenus().addAll(menuHelp);

        }

        private void initHelpComponents() {
            helpNextBtn.setId("nextBtn");
            helpPrevBtn.setId("prevBtn");
            helpStackPane.setStyle("-fx-background-color: rgba(237, 188, 0, 0.20);");
            helpStackPane.setVisible(false);
            helpNoteTextArea.getStyleClass().addAll("transparentAboutTextArea");

            helpNoteTextArea.setPrefSize(300, 200);
            helpNoteTextArea.setMinSize(300, 200);
            helpNoteTextArea.setEditable(false);
            helpPrevBtn.addEventFilter(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
                @Override
                public void handle(KeyEvent event) {
                    if (event.getCode() == KeyCode.TAB && event.isShiftDown()) {
                        helpCloseBtn.requestFocus();
                        event.consume();
                    }
                }
            });
            helpCloseBtn.addEventFilter(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
                @Override
                public void handle(KeyEvent event) {
                    if (event.getCode() == KeyCode.TAB && !event.isShiftDown()) {
                        helpPrevBtn.requestFocus();
                        event.consume();
                    }
                }
            });
        }

        private void initMainComponents() {
            moveRenameBtn.setDisable(true);
            moveRenameBtn.setVisible(false);
            moveRenameInfoLabel.setVisible(false);
            statusFilterComboBox.setValue(Status.OK);
            clauseFilterComboBox.setValue(Clause.EQUAL_OR_STRONGER);
            scanBtn.setMinWidth(90);
            moveRenameBtn.setMinWidth(90);
            treeTableView.getColumns().setAll(getTreeTableViewColumns());
            treeTableView.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY);
            treeTableView.setShowRoot(true);
            progressBar.progressProperty().addListener(new ChangeListener<Number>() {
                private double maxProgressValue = 0;

                @Override
                public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                    if (newValue.doubleValue() >= 0) {
                        if (newValue.doubleValue() <= Double.MIN_NORMAL) {
                            maxProgressValue = 0;
                        } else if (newValue.doubleValue() >= 1) {
                            maxProgressValue = 1;
                        } else {
                            maxProgressValue = Math.max(maxProgressValue, newValue.doubleValue());
                        }
                        progressBarProgress.setText("" + Precision.round(maxProgressValue * 100., 0) + "%");
                    }

                }
            });
        }

        private void initAboutComponents() {
            aboutFooterLabel.setFocusTraversable(false);
            aboutFooterLabel.addEventFilter(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
                @Override
                public void handle(KeyEvent event) {
                    if (event.getCode() == KeyCode.TAB) {
                        event.consume();
                    }
                }
            });
            aboutThanksToTransition.setInterpolator(Interpolator.LINEAR);
            aboutThanksToTransition.setAutoReverse(true);
            aboutThanksToTransition.setCycleCount(Timeline.INDEFINITE);

            helpPrevBtn.addEventFilter(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
                @Override
                public void handle(KeyEvent event) {
                    if (event.getCode() == KeyCode.TAB && event.isShiftDown()) {
                        helpCloseBtn.requestFocus();
                        event.consume();
                    }
                }
            });
            helpCloseBtn.addEventFilter(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
                @Override
                public void handle(KeyEvent event) {
                    if (event.getCode() == KeyCode.TAB && !event.isShiftDown()) {
                        helpPrevBtn.requestFocus();
                        event.consume();
                    }
                }
            });

        }


        public StackPane getMainStackPane() {
            return mainStackPane;
        }

        private void createMainContent() {
            GridPane mainContentGrid = new GridPane();
            setupGridParams(mainContentGrid, UIConstants.GAP_STD);

            Node extensionsHbox = getExtensionsHbox();
            Node treeTableViewVbox = getTreeTableViewVbox();
            Node statusBarGrid = getStatusBarGrid();

            mainContentGrid.add(extensionsHbox, 0, 0);
            mainContentGrid.add(getSelectPathHbox(), 3, 0);
            mainContentGrid.add(treeTableViewVbox, 0, 2, 4, 1);
            mainContentGrid.add(getFilterHbox(), 0, 1, 1, 1);
            mainContentGrid.add(getMoveRenameHbox(), 2, 1, 2, 1);
            mainContentGrid.add(statusBarGrid, 0, 3, 4, 1);

            mainContentGrid.setHgrow(extensionsHbox, Priority.ALWAYS);
            mainContentGrid.setVgrow(treeTableViewVbox, Priority.ALWAYS);

            VBox mainContentVBox = new VBox(menuBar, mainContentGrid, statusBarGrid);
            mainContentVBox.setFillWidth(true);
            mainContentVBox.setVgrow(mainContentGrid, Priority.ALWAYS);
            mainStackPane.getChildren().add(mainContentVBox);
        }


        private void setupGridParams(GridPane grid, double gap) {
            grid.setHgap(gap);
            grid.setVgap(gap);
            grid.setPadding(UIConstants.INSETS_STD);
            grid.setAlignment(Pos.CENTER);
        }

        private Node getTreeTableViewVbox() {
            VBox vBox = new VBox(10, treeTableView);
            vBox.setVgrow(treeTableView, Priority.ALWAYS);
            vBox.setAlignment(Pos.CENTER_RIGHT);
            return vBox;
        }

        private Node getMoveRenameHbox() {
            HBox hbox = new HBox(10,
                    moveRenameInfoLabel,
                    moveRenameBtn);
            hbox.setAlignment(Pos.CENTER_RIGHT);
            return hbox;
        }

        private Node getStatusBarGrid() {
            GridPane statusBarGrid = new GridPane();
            setupGridParams(statusBarGrid, 16);
            statusBarGrid.setAlignment(Pos.TOP_LEFT);
            statusBarGrid.add(statusBarText, 0, 0, 1, 1);
            statusBarGrid.add(progressBarProgress, 1, 0, 1, 1);
            statusBarGrid.add(progressBar, 2, 0, 1, 1);
            statusBarGrid.setHgrow(progressBar, Priority.NEVER);
            statusBarGrid.setHgrow(statusBarText, Priority.ALWAYS);
            statusBarGrid.setHgrow(progressBarProgress, Priority.NEVER);

            statusBarGrid.getColumnConstraints().addAll(
                    new ColumnConstraints(-1, -1, -1, Priority.ALWAYS, HPos.LEFT, true),
                    new ColumnConstraints(-1, -1, -1, Priority.NEVER, HPos.RIGHT, false),
                    new ColumnConstraints(-1, -1, -1, Priority.NEVER, HPos.RIGHT, false)
            );

            statusBarGrid.getRowConstraints().addAll(
                    new RowConstraints(-1, -1, -1, Priority.NEVER, VPos.CENTER, false)
            );

            return statusBarGrid;
        }

        private Node getExtensionsHbox() {
            extensionsLbl = new Label("Extensions: ");
            extensionsHbox = new HBox(10, extensionsLbl, jpgCheckBox, gifCheckBox, nefCheckBox, cidCheckBox);
            extensionsHbox.setAlignment(Pos.CENTER_LEFT);
            return extensionsHbox;
        }

        private Node getSelectPathHbox() {
            Label selectScanFolderLbl = new Label("Select folder to scan: ");
            folderPath.setMinWidth(350);
            HBox selectPathHbox = new HBox(10, selectScanFolderLbl, folderPath, selectPathBtn, scanBtn/*, debugBtn*/);
            selectPathHbox.setAlignment(Pos.CENTER_RIGHT);
            return selectPathHbox;
        }

        private Node getFilterHbox() {
            Label filterLbl = new Label("Show results that are");
            HBox filterHbox = new HBox(10,
                    filterLbl,
                    clauseFilterComboBox,
                    statusFilterComboBox);
            filterHbox.setAlignment(Pos.CENTER_LEFT);
            return filterHbox;
        }

        private Collection<Status> getStatusFilterComboboxItems() {
            return Arrays.asList(Status.values()).
                    stream().
                    filter(status -> status != Status.SMTH_GOES_WRONG).
                    collect(Collectors.toList());
        }

        private Collection<TreeTableColumn<?, ?>> getTreeTableViewColumns() {
            List<TreeTableColumn<?, ?>> columns = new LinkedList<>();
            TreeTableColumn<BytesProcessResult, String> nameCol = new TreeTableColumn<>("Name");
            TreeTableColumn<BytesProcessResult, String> statusCol = new TreeTableColumn<>("Status");
            TreeTableColumn<BytesProcessResult, String> descriptionColumn = new TreeTableColumn<>("Description");
            TreeTableColumn<BytesProcessResult, String> detailsColumn = new TreeTableColumn<>("Details");

            nameCol.setMinWidth(100);
            statusCol.setMinWidth(30);
            descriptionColumn.setMinWidth(150);
            detailsColumn.setMinWidth(150);

            nameCol.setCellValueFactory((TreeTableColumn.CellDataFeatures<BytesProcessResult, String> p) -> new ReadOnlyStringWrapper(p.getValue().getValue() == null ? "-" : p.getValue().getValue().getPath().toFile().getName()));
            statusCol.setCellValueFactory(p -> new ReadOnlyStringWrapper(p.getValue().getValue() == null || p.getValue().getValue().getStatus() == null || p.getValue().getValue().getStatus() == Status.FOLDER ? "" : p.getValue().getValue().getStatus().toString()));
            statusCol.setCellFactory(new StatusCellFactory());
            descriptionColumn.setCellValueFactory(p -> new ReadOnlyStringWrapper(p.getValue().getValue() == null ? "-" : p.getValue().getValue().getDescription()));
            descriptionColumn.setCellFactory(new DescriptionAndDetailsCellFactory("File processing description"));
            detailsColumn.setCellValueFactory(p -> new ReadOnlyStringWrapper(p.getValue().getValue() == null ? "-" : p.getValue().getValue().getDetails()));
            detailsColumn.setCellFactory(new DescriptionAndDetailsCellFactory("File processing details"));
            columns.add(nameCol);
            columns.add(statusCol);
            columns.add(descriptionColumn);
            columns.add(detailsColumn);
            return columns;
        }

        private class StatusCellFactory implements Callback<TreeTableColumn<BytesProcessResult, String>, TreeTableCell<BytesProcessResult, String>> {
            public void hackTooltipStartTiming(Tooltip tooltip) {
                try {
                    Field fieldBehavior = tooltip.getClass().getDeclaredField("BEHAVIOR");
                    fieldBehavior.setAccessible(true);
                    Object objBehavior = fieldBehavior.get(tooltip);

                    Field fieldTimer = objBehavior.getClass().getDeclaredField("activationTimer");
                    fieldTimer.setAccessible(true);
                    Timeline objTimer = (Timeline) fieldTimer.get(objBehavior);

                    objTimer.getKeyFrames().clear();
                    objTimer.getKeyFrames().add(new KeyFrame(new Duration(50)));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public TreeTableCell<BytesProcessResult, String> call(TreeTableColumn<BytesProcessResult, String> param) {
                return new TreeTableCell<BytesProcessResult, String>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        if (!empty &&
                                getTreeTableRow().getTreeItem() != null) {
                            TreeItem<BytesProcessResult> treeItem = getTreeTableRow().getTreeItem();
                            AtomicInteger i = new AtomicInteger(0);
                            if (treeItem.getValue().isLeaf()) {
                                HBox hBox = new HBox();
                                hBox.getChildren().add(new Label(item));
                                hBox.setAlignment(Pos.CENTER_LEFT);
                                setGraphic(hBox);
                            } else {
                                GridPane statusesGrid = new GridPane();
                                setupGridParams(statusesGrid, 0);
                                statusesGrid.getRowConstraints().addAll(new RowConstraints(-1, -1, -1, Priority.NEVER, VPos.TOP, false));
                                Map<Status, Long> byStatusesMap = treeItem.getValue().getResultPostInfo().getByStatusesMap();
                                long totalNonFoldersInside = treeItem.getValue().getResultPostInfo().getTotalNonFoldersInside();
                                TreeSet<Status> sortedStatuses = new TreeSet<>(byStatusesMap.keySet());
                                sortedStatuses.stream().filter(new Predicate<Status>() {
                                    @Override
                                    public boolean test(Status status) {
                                        return status != Status.SMTH_GOES_WRONG && status.getPriority() > Status.FOLDER.getPriority();
                                    }
                                }).sorted((o1, o2) -> {
                                    return Integer.compare(o2.getPriority(), o1.getPriority());
                                }).forEachOrdered(new Consumer<Status>() {
                                    @Override
                                    public void accept(Status status) {
                                        if (totalNonFoldersInside != 0L) {
                                            float percent = (0f + byStatusesMap.get(status)) / totalNonFoldersInside * 100;
                                            //BigDecimal percent = new BigDecimal(byStatusesMap.get(status)).divide(new BigDecimal(totalNonFoldersInside)).multiply(new BigDecimal(100));
                                            float percentRounded = Precision.round(percent, 1);
                                            if (percentRounded != 0) {
                                                Label currentStatusLbl = new Label();
                                                HBox hBox = new HBox(currentStatusLbl);
                                                hBox.setMaxHeight(5);
                                                hBox.setMinHeight(5);
                                                hBox.setPrefHeight(5);
                                                hBox.getStyleClass().add("tree-table-row-bckgnd-" + status.toString().toLowerCase());
                                                hBox.setHgrow(currentStatusLbl, Priority.ALWAYS);
                                                statusesGrid.add(hBox, i.get(), 0, 1, 1);
                                                ColumnConstraints columnConstraints = new ColumnConstraints(-1, -1, -1, Priority.ALWAYS, HPos.LEFT, true);
                                                columnConstraints.setPercentWidth(percent);
                                                statusesGrid.getColumnConstraints().add(columnConstraints);
                                                i.incrementAndGet();
                                            }
                                        }

                                    }
                                });
                                Tooltip tooltip = new Tooltip(treeItem.getValue().getResultPostInfo().getPostInfoFormatted());
                                hackTooltipStartTiming(tooltip);
                                setTooltip(tooltip);
                                setGraphic(statusesGrid);
                            }
                        } else {
                            setText(null);
                            setGraphic(null);
                            setTooltip(null);
                        }
                    }
                };
            }
        }

        private class DescriptionAndDetailsCellFactory implements Callback<TreeTableColumn<BytesProcessResult, String>, TreeTableCell<BytesProcessResult, String>> {

            private final String title;

            public DescriptionAndDetailsCellFactory(String title) {
                this.title = title;
            }

            @Override
            public TreeTableCell<BytesProcessResult, String> call(TreeTableColumn<BytesProcessResult, String> param) {
                TreeTableCell<BytesProcessResult, String> cell = new LabeledWithButtonTreeTableCell();
                return cell;
            }

            private class LabeledWithButtonTreeTableCell extends TreeTableCell<BytesProcessResult, String> {
                @Override
                protected void updateItem(String item, boolean empty) {
                    if (!empty &&
                            !StringUtils.isBlank(item) &&
                            getTreeTableRow().getTreeItem() != null &&
                            getTreeTableRow().getTreeItem().getValue() != null) {
                        TreeItem<BytesProcessResult> treeItem = getTreeTableRow().getTreeItem();
                        Label lbl = new Label(item.replaceAll("\\s*[\\r\\n]+\\s*", "").trim());
                        Button button = new Button("...");
                        button.setMaxSize(lbl.getHeight() / 2, lbl.getHeight() / 2);
                        HBox hBox = new HBox(lbl, button);
                        hBox.setAlignment(Pos.CENTER_LEFT);
                        button.getStyleClass().add("tree-table-row-details-btn");
                        button.setOnAction(event -> {
                            getTreeTableView().getSelectionModel().select(treeItem);
                            String message = item.replaceAll("\\r\\n", "").trim();
                            message = message.length() > 2000 ? message.substring(0, 1997) + "...\n\n... U can copy to clipboard the full log" : message;
                            Alert alert = new Alert(Alert.AlertType.INFORMATION);
                            alert.setTitle(title);
                            alert.setHeaderText(null);
                            alert.setContentText(treeItem.getValue().getPath().toString());

                            // Create expandable Exception.
                            Label label = new Label("The exception stacktrace was:");
                            TextArea textArea = new TextArea(message);
                            textArea.setEditable(false);
                            textArea.setWrapText(true);
                            textArea.setMaxWidth(Double.MAX_VALUE);
                            textArea.setMaxHeight(Double.MAX_VALUE);
                            GridPane.setVgrow(textArea, Priority.ALWAYS);
                            GridPane.setHgrow(textArea, Priority.ALWAYS);
                            GridPane expContent = new GridPane();
                            expContent.setMaxWidth(Double.MAX_VALUE);
                            expContent.add(label, 0, 0);
                            expContent.add(textArea, 0, 1);
                            alert.getDialogPane().setExpandableContent(expContent);
                            ButtonType buttonTypeCopy = new ButtonType("Copy details to clipboard");
                            ButtonType buttonTypeOk = new ButtonType("Ok", ButtonBar.ButtonData.CANCEL_CLOSE);

                            alert.getButtonTypes().setAll(buttonTypeCopy, buttonTypeOk);

                            final Button buttonCopy = (Button) alert.getDialogPane().lookupButton(buttonTypeCopy);
                            buttonCopy.setMinWidth(300);
                            buttonCopy.setMaxWidth(300);
                            buttonCopy.setPrefWidth(300);
                            buttonCopy.addEventFilter(ActionEvent.ACTION, (e) -> {
                                final Clipboard clipboard = Clipboard.getSystemClipboard();
                                final ClipboardContent content = new ClipboardContent();
                                content.putString(treeItem.getValue().getPath().toString() + "\n" + item);
                                clipboard.setContent(content);
                                buttonCopy.setText("Copied");
                                Executors.newSingleThreadExecutor().submit(() -> {
                                    try {
                                        Thread.sleep(1000);
                                    } catch (InterruptedException e1) {
                                    } finally {
                                        Platform.runLater(() -> {
                                            buttonCopy.setText("Copy details to clipboard");
                                        });
                                    }
                                });
                                e.consume();
                            });
                            switch (treeItem.getValue().getStatus()) {
                                case CRITICAL:
                                case ERROR:
                                    alert.setAlertType(Alert.AlertType.ERROR);
                                    break;
                                case WARN:
                                    alert.setAlertType(Alert.AlertType.WARNING);
                                    break;
                                default:
                                    alert.setAlertType(Alert.AlertType.INFORMATION);
                            }
                            alert.showAndWait();
                        });
                        setGraphic(hBox);
                    } else {
                        setText(null);
                        setGraphic(null);
                    }
                }
            }
        }
    }

    private class ShowAboutEventHandler implements EventHandler<ActionEvent> {
        @Override
        public void handle(ActionEvent event) {
            mainForm.aboutStackPane.addEventFilter(KeyEvent.KEY_PRESSED, hideAboutStackPaneEventHandler);
            FadeTransition ft = new FadeTransition(Duration.millis(200), mainForm.aboutStackPane);
            ft.setFromValue(0.0);
            ft.setToValue(1.0);
            ft.setAutoReverse(false);

            setupAnimation();

            mainForm.aboutStackPane.setVisible(true);
            ft.play();
            mainForm.aboutFooterLabel.requestFocus();
        }

        private void setupAnimation() {
            //move text to start
            mainForm.aboutThanksToText.setY(mainForm.aboutMaskStackPane.getBoundsInParent().getMinY());
            //set movement bounds
            mainForm.aboutThanksToTransition.setToY(+mainForm.aboutMaskStackPane.getHeight() / 2 - mainForm.aboutThanksToText.getBoundsInLocal().getHeight());
            mainForm.aboutThanksToTransition.setFromY(+mainForm.aboutMaskStackPane.getHeight() / 2);
            mainForm.aboutThanksToTransition.play();
        }
    }

    private class ShowHelpEventHandler implements EventHandler<ActionEvent> {
        private int step = Integer.MIN_VALUE;

        @Override
        public void handle(ActionEvent event) {
            mainForm.helpStackPane.addEventFilter(KeyEvent.KEY_PRESSED, hideHelpStackPaneEventHandler);
            if (step == Integer.MIN_VALUE) {
                //some init code can be placed here
                step = 0;
            }

            if (event.getSource() instanceof Node) {
                Node sourceNode = (Node) event.getSource();
                if (sourceNode.getId() != null) {
                    if (sourceNode.getId() == "nextBtn") {
                        step++;
                        mainForm.helpNextBtn.requestFocus();
                    } else if (sourceNode.getId() == "prevBtn") {
                        step = Math.max(0, --step);
                        mainForm.helpPrevBtn.requestFocus();
                    }

                }
            } else {
                FadeTransition ft = new FadeTransition(Duration.millis(200), mainForm.helpStackPane);
                ft.setFromValue(0.0);
                ft.setToValue(1.0);
                ft.setAutoReverse(false);
                mainForm.helpStackPane.setVisible(true);
                mainForm.helpNextBtn.requestFocus();
                ft.play();
            }
            showHelpContentForCurrentStep();

        }

        private void showHelpContentForCurrentStep() {
            CubicCurveWithArrows arrow = null;
            mainForm.helpArrowsStackPane.getChildren().clear();
            //its not very clear way to keep messages, but its fast
            switch (step) {
                case 0:
                    arrow = new CubicCurveWithArrows(mainForm.helpNoteTextArea, new Point2D(20, 20), false);
                    mainForm.helpNoteTextArea.setText("This helpful help help you to use that program.\n" +
                            "You can close it by pressing at any place of a screen except navigate buttons.\n\n" +
                            "Please, press the next button to discover the world");
                    break;
                case 1:
                    arrow = null;
                    mainForm.helpNoteTextArea.setText("This program helps you to find corrupted images.\n" +
                            "Just do 3 simple steps...");
                    break;
                case 2:
                    mainForm.helpNoteTextArea.setText("1. Select which file types will be scanned");
                    arrow = new CubicCurveWithArrows(mainForm.helpNoteTextArea, mainForm.extensionsLbl, false);
                    break;
                case 3:
                    mainForm.helpNoteTextArea.setText("2. Select a folder for scan. " +
                            "Subfolders will be scanned too");
                    arrow = new CubicCurveWithArrows(mainForm.helpNoteTextArea, mainForm.selectPathBtn, false);
                    break;
                case 4:
                    mainForm.helpNoteTextArea.setText("3. Press scan button\n\n" +
                            "What`s next?...");
                    arrow = new CubicCurveWithArrows(mainForm.helpNoteTextArea, mainForm.scanBtn, false);
                    break;
                case 5:
                    mainForm.helpNoteTextArea.setText("After scanning you can:\n" +
                            "- view statistic about files statuses\n" +
                            "- view details and error information\n" +
                            "- navigate to directory with corrupted images\n\n" +
                            "Press next button to view the bonus function!\n");
                    arrow = null;
                    break;
                case 6:
                    mainForm.helpNoteTextArea.setText("You even can rename corrupted files.\n" +
                            "Rename and scan reports will be stored inside " +
                            "each subfolder with images. Total report will be " +
                            "stored inside main directory. Corrupted files will be renamed with '.cid' extension...");
                    arrow = new CubicCurveWithArrows(mainForm.helpNoteTextArea, mainForm.moveRenameBtn, false);
                    break;
                case 7:
                    mainForm.helpNoteTextArea.setText("Renaming help you to save that files separately and bring " +
                            "them to recovery master specialist or just to delete. " +
                            "Also you favorite image viewer will not hands up " +
                            "during render that files." +
                            "That`s all. :)");
                    arrow = new CubicCurveWithArrows(mainForm.helpNoteTextArea, mainForm.moveRenameBtn, false);
                    break;
                default:

            }
            if (arrow != null) {
                mainForm.helpArrowsStackPane.setAlignment(arrow, Pos.TOP_LEFT);
                mainForm.helpArrowsStackPane.getChildren().add(arrow);
            } else {
                mainForm.helpArrowsStackPane.getChildren().clear();
            }
        }
    }

    private class HideAboutStackPaneEventHandler implements EventHandler<KeyEvent> {
        @Override
        public void handle(KeyEvent event) {
            if (event.getCode() == KeyCode.ESCAPE) {
                hideAboutEventHandler.hide();
            }
        }
    }

    private class HideAboutEventHandler implements EventHandler<ActionEvent> {
        @Override
        public void handle(ActionEvent event) {
            hide();
        }

        public void hide() {
            mainForm.aboutStackPane.removeEventFilter(KeyEvent.KEY_PRESSED, hideAboutStackPaneEventHandler);
            FadeTransition ft = new FadeTransition(Duration.millis(100), mainForm.aboutStackPane);
            ft.setFromValue(1.0);
            ft.setToValue(0.0);
            ft.setAutoReverse(false);
            ft.play();
            ft.setOnFinished((e) -> {
                mainForm.aboutStackPane.setVisible(false);
            });
        }
    }

    private class HideHelpStackPaneEventHandler implements EventHandler<KeyEvent> {
        @Override
        public void handle(KeyEvent event) {
            if (event.getCode() == KeyCode.ESCAPE) {
                hideHelpEventHandler.hide();
            }
        }
    }

    private class HideHelpEventHandler implements EventHandler<ActionEvent> {
        @Override
        public void handle(ActionEvent event) {
            hide();
        }

        public void hide() {
            mainForm.helpStackPane.removeEventFilter(KeyEvent.KEY_PRESSED, hideHelpStackPaneEventHandler);
            FadeTransition ft = new FadeTransition(Duration.millis(100), mainForm.helpStackPane);
            ft.setFromValue(1.0);
            ft.setToValue(0.0);
            ft.setAutoReverse(false);
            ft.play();
            ft.setOnFinished((e) -> {
                mainForm.helpStackPane.setVisible(false);
            });
        }
    }
}
