/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package manifestgenerator;

import java.beans.XMLDecoder;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Modality;
import javafx.stage.Stage;
import manifestgenerator.models.ManifestPrinter;
import manifestgenerator.models.ManifestExporter;
import manifestgenerator.models.ManifestViewModel;
import manifestgenerator.models.Palette;
import manifestgenerator.models.PaletteListViewCell;
import manifestgenerator.models.PaletteManager;
import manifestgenerator.models.PreferencesViewModel;

/**
 * FXML Controller class
 *
 * @author talk2
 */
public class RootLayoutController
        implements Initializable
{
    // <editor-fold defaultstate="collapsed" desc="Fields">
    private Stage mainStage;
    private final ManifestViewModel viewModel;
    private final ObservableList<Palette> palettes;
    private PreferencesViewModel preferencesViewModel;
    private final String PREFERENCES = "preferences.xml";
    private PreferencesController preferencesController;
    private boolean isBusy;

    @FXML
    private TextField browseTextField;

    @FXML
    private ProgressBar progressBar;

    @FXML
    private Label progressLabel;

    @FXML
    private Label originalFileLabel;

    @FXML
    private Label foundOnPageLabel;

    @FXML
    private Label manifestPositionLabel;

    @FXML
    private Button printButton;

    @FXML
    private Button exportButton;

    @FXML
    private ListView<Palette> manifestListView;

    @FXML
    private Button browseButton;

    @FXML
    private MenuItem browseMenuItem;

    @FXML
    private MenuItem preferencesMenuItem;

    @FXML
    private MenuItem exitMenuItem;

    @FXML
    private MenuItem helpMenuItem;

    @FXML
    private MenuItem printMenuItem;

    @FXML
    private MenuItem exportMenuItem;

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Action Handlers">
    @FXML
    void onBrowseAction(ActionEvent event) throws InterruptedException {
        onBrowse();
    }

    @FXML
    void onExportAction(ActionEvent event) throws InterruptedException {
        onExportToWord();
    }

    @FXML
    void onPrintAction(ActionEvent event) throws IOException, InterruptedException {
        onPrint();
    }

    @FXML
    void onHelpMenuAction(ActionEvent event) {
        onHelpMenuAction();
    }

    @FXML
    void onPreferencesAction(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass()
                    .getResource("/manifestgenerator/views/Preferences.fxml"));
            BorderPane page = loader.load();

            preferencesController = loader.getController();
            preferencesController.setStage(mainStage);
            preferencesController.setViewModel(preferencesViewModel
                    .getDefaultInputDirectory(),
                    preferencesViewModel.getDefaultOutputDirectory());

            Stage preferencesDialog = new Stage();
            preferencesDialog.setResizable(Boolean.FALSE);
            preferencesDialog.setTitle("Modify Preferences");
            preferencesDialog.initModality(Modality.WINDOW_MODAL);
            preferencesDialog.initOwner(mainStage);
            final int WIDTH = 700, HEIGHT = 200;
            Scene preferencesScene = new Scene(page, WIDTH, HEIGHT);
            preferencesDialog.setScene(preferencesScene);
            preferencesDialog.getIcons().add(mainStage.getIcons().get(0));
            preferencesDialog.setOnCloseRequest(closeEvent -> {
                System.out.println("Preferences closed");
                preferencesController.savePreferences(PREFERENCES);
                preferencesDialog.close();
                closeEvent.consume();
            });

            preferencesDialog.showAndWait();

        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    void onExit(ActionEvent event) {
        Platform.exit();
    }

    @FXML
    void onAboutAction(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass()
                    .getResource("/manifestgenerator/views/AboutPageView.fxml"));
            BorderPane page = loader.load();
            
            Stage aboutDialog = new Stage();
            aboutDialog.setResizable(Boolean.FALSE);
            aboutDialog.setTitle("About Mnifest Generator");
            aboutDialog.initModality(Modality.WINDOW_MODAL);
            aboutDialog.initOwner(mainStage);
            aboutDialog.getIcons().addAll(mainStage.getIcons());
            final int WIDTH = 600, HEIGHT = 600;
            Scene aboutScene = new Scene(page, WIDTH, HEIGHT);
            aboutDialog.setScene(aboutScene);
            aboutScene.addEventFilter(KeyEvent.KEY_PRESSED, keyEvent -> {
                if(keyEvent.getCode() == KeyCode.ESCAPE) {
                    aboutDialog.close();
                }
            });
            
            aboutScene.addEventFilter(MouseEvent.MOUSE_PRESSED, mouseEvent -> {
                if(mouseEvent.getButton() == MouseButton.PRIMARY) {
                    aboutDialog.close();
                }
            });
            aboutDialog.centerOnScreen();
            aboutDialog.showAndWait();
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }        
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Helpers">
    private void onBrowse() {

        if (browseButton.isDisabled()) {
            return;
        }

        clearUserInterface();        

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Manifest Data File");
        fileChooser.getExtensionFilters().clear();
        fileChooser.getExtensionFilters()
                .add(new ExtensionFilter("Comma Separated Values", "*.csv"));
        if (preferencesViewModel.getDefaultInputDirectory() != null) {
            File inputDirectory = new File(preferencesViewModel.getDefaultInputDirectory());
            fileChooser.setInitialDirectory(inputDirectory);
        }
        File file = fileChooser.showOpenDialog(mainStage);
        if (file != null) {
            viewModel.setOriginalFilePath(file.getPath());
            viewModel.setOriginalFileName(file.getName());

            PaletteManager paletteManager = new PaletteManager(file);
            StringBinding progressBinding = new StringBinding()
            {
                {
                    super.bind(paletteManager.messageProperty());
                    super.bind(paletteManager.progressProperty());
                }

                @Override
                protected String computeValue() {
                    return String.format("%s%s%%", paletteManager.messageProperty().get(),
                            Math.round(paletteManager.progressProperty().multiply(100).get()));
                }
            };
            progressLabel.textProperty().bind(progressBinding);
            progressBar.progressProperty().bind(paletteManager.progressProperty());
            manifestListView.itemsProperty().bind(paletteManager.palettesProperty());
            paletteManager.setOnSucceeded(event -> {
                manifestListView.getSelectionModel().select(0);
                palettes.clear();
                palettes.addAll(paletteManager.getPalettes());

            });
            viewModel.totalPageCountInFileProperty().bind(paletteManager.totalPagesProperty());
            viewModel.totalPagesInManifestProperty().bind(paletteManager.totalManifestPagesProperty());
            viewModel.exportButtonDisabledProperty().bind(paletteManager.workingProperty());
            viewModel.printButtonDisabledProperty().bind(paletteManager.workingProperty());
            new Thread(paletteManager).start();
        } else {
            clearUserInterface();
        }
    }

    private void clearUserInterface() {
        progressLabel.textProperty().unbind();
        progressLabel.textProperty().set("");
        progressBar.progressProperty().unbind();
        progressBar.progressProperty().set(0);
        manifestListView.itemsProperty().unbind();
        manifestListView.itemsProperty().set(FXCollections.observableArrayList());
        viewModel.totalPageCountInFileProperty().unbind();
        viewModel.totalPageCountInFileProperty().set(0);
        viewModel.totalPagesInManifestProperty().unbind();
        viewModel.totalPagesInManifestProperty().set(0);
        viewModel.exportButtonDisabledProperty().unbind();
        viewModel.exportButtonDisabledProperty().set(true);
        viewModel.printButtonDisabledProperty().unbind();
        viewModel.printButtonDisabledProperty().set(true);
        viewModel.setOriginalFilePath(null);
        viewModel.setOriginalFileName("N/A");
        viewModel.setCurrentPageInManifest(0);
        viewModel.setReferencePage(0);
        viewModel.progressProperty().set(0);
        viewModel.progressTextProperty().set(null);
        printButton.disableProperty().bind(viewModel.printButtonDisabledProperty());
        exportButton.disableProperty().bind(viewModel.exportButtonDisabledProperty());       
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Public Methods">
    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {

        browseTextField.textProperty().bind(viewModel.originalFilePathProperty());
        printButton.disableProperty().bind(viewModel.printButtonDisabledProperty());
        exportButton.disableProperty().bind(viewModel.exportButtonDisabledProperty());

        manifestListView.setCellFactory(listView -> new PaletteListViewCell());
        manifestListView.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        viewModel.setCurrentPageInManifest(
                                manifestListView.getSelectionModel()
                                        .getSelectedIndex() + 1
                        );
                        viewModel.setReferencePage(newValue.getReferencePage());
                    }
                });

        originalFileLabel.textProperty()
                .bind(Bindings.concat(
                        "Original File: ", viewModel.originalFileNameProperty()
                ));

        foundOnPageLabel.textProperty()
                .bind(Bindings.concat(
                        "Found on Page: ", viewModel.referencePageProperty(),
                        " of ",
                        viewModel.totalPageCountInFileProperty()
                ));

        manifestPositionLabel.textProperty()
                .bind(Bindings.concat(
                        "Manifest: Page ",
                        viewModel.currentPageInManifestProperty(),
                        " of ", viewModel.totalPagesInManifestProperty()
                ));

        //helpMenuItem.setAccelerator(KeyCombination.keyCombination("F1"));
        progressBar.progressProperty().bind(viewModel.progressProperty());
        progressLabel.textProperty().bind(viewModel.progressTextProperty());
        printMenuItem.disableProperty().bind(printButton.disableProperty());
        exportMenuItem.disableProperty().bind(exportButton.disableProperty());
        browseMenuItem.disableProperty().bind(browseButton.disableProperty());
    }

    public void setMainStage(Stage stage) {
        mainStage = stage;
    }

    public void onExportToWord() {

        if (exportButton.isDisabled()) {
            return;
        }

        progressBar.progressProperty().unbind();
        progressLabel.textProperty().unbind();
        printButton.disableProperty().unbind();
        exportButton.disableProperty().unbind();
        browseButton.disableProperty().unbind();

        ManifestExporter exporter = new ManifestExporter(palettes,
                mainStage, preferencesViewModel.getDefaultOutputDirectory());
        StringBinding progressBinding = new StringBinding()
        {
            {
                super.bind(exporter.messageProperty());
                super.bind(exporter.progressProperty());
            }

            @Override
            protected String computeValue() {
                if (exporter.getProgress() < 0) {
                    return exporter.messageProperty().get();
                }
                return String.format("%s%s%%", exporter.messageProperty().get(),
                        Math.round(exporter.progressProperty().multiply(100).get()));
            }
        };
        progressBar.progressProperty().bind(exporter.progressProperty());
        progressLabel.textProperty().bind(progressBinding);
        exportButton.disableProperty().bind(exporter.workingProperty());
        printButton.disableProperty().bind(exporter.workingProperty());
        browseButton.disableProperty().bind(exporter.workingProperty());
        new Thread(exporter).run();
    }

    public void onPrint() throws IOException, InterruptedException {

        if (printButton.isDisabled()) {
            return;
        }

        progressBar.progressProperty().unbind();
        progressLabel.textProperty().unbind();
        printButton.disableProperty().unbind();
        exportButton.disableProperty().unbind();
        browseButton.disableProperty().unbind();

        ManifestPrinter printer = new ManifestPrinter(palettes, mainStage);
        StringBinding progressBinding = new StringBinding()
        {
            {
                super.bind(printer.messageProperty());
                super.bind(printer.progressProperty());
            }

            @Override
            protected String computeValue() {
                if (printer.getProgress() < 0) {
                    return printer.messageProperty().get();
                }

                return String.format("%s%s%%", printer.messageProperty().get(),
                        Math.round(printer.progressProperty().multiply(100).get()));
            }
        };
        progressBar.progressProperty().bind(printer.progressProperty());
        progressLabel.textProperty().bind(progressBinding);
        printButton.disableProperty().bind(printer.workingProperty());
        exportButton.disableProperty().bind(printer.workingProperty());
        browseButton.disableProperty().bind(printer.workingProperty());
        new Thread(printer).run();
    }

    public void onHelpMenuAction() {
        // Create a custom Web view for showing help files
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Help");
        alert.setHeaderText("Comping Soon");
        alert.setContentText("A Help menu will be included in the next release.");
        alert.showAndWait();
    }

    public RootLayoutController() {
        viewModel = new ManifestViewModel();
        palettes = FXCollections.observableArrayList();
        FileInputStream stream = null;
        try {
            File file = new File(PREFERENCES);
            if (!file.exists()) {
                file.createNewFile();
            }
            stream = new FileInputStream(PREFERENCES);
            XMLDecoder decoder = new XMLDecoder(stream);
            if (file.length() > 0) {
                preferencesViewModel = (PreferencesViewModel) decoder
                        .readObject();
            }
            else {
                preferencesViewModel = new PreferencesViewModel();
            }
            decoder.close();
        }
        catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
        finally {
            try {
                if (stream != null) {
                    stream.close();
                }
            }
            catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        if (preferencesViewModel == null) {
            preferencesViewModel = new PreferencesViewModel();
        }
    }

    // </editor-fold>
}
