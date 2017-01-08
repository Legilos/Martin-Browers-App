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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.binding.StringBinding;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.print.PageLayout;
import javafx.print.PageOrientation;
import javafx.print.Paper;
import javafx.print.Printer;
import javafx.print.PrinterJob;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Modality;
import javafx.stage.Stage;
import manifestgenerator.models.Cases;
import manifestgenerator.models.ManifestPrinter;
import manifestgenerator.models.ManifestExporter;
import manifestgenerator.models.PrintView;
import manifestgenerator.models.ManifestViewModel;
import manifestgenerator.models.Palette;
import manifestgenerator.models.PaletteListViewCell;
import manifestgenerator.models.PaletteManager;
import manifestgenerator.models.PreferencesViewModel;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTString;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblWidth;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;

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
        // Create a custom UI for this

        String about = "Manifest Generator reads an in-house (specific) CSV "
                + "file and collects route data for various trailers and their "
                + "respective palettes.\n\r"
                + "The application was written by students from the "
                + "Computer Sceince Club at West Chester University "
                + "of Pennsylvania.\n\r"
                + "Team Members Include:\n"
                + "Adrian Rodriguez\n"
                + "Gina Dedes\n"
                + "Jason Jackson\n"
                + "Mohamed Pussah\n"
                + "Patrick Savella\n"
                + "Won Murdocq\n"
                + "----------------------------------------------------------------------\n"
                + "Apllication Support:\n"
                + "Adrian Rodriguez (rodriguez.adrian609@gmail.com|609 403 0337)\n"
                + "Mohamed Pussah (talk2alie@outlook.com|267 357 6840)";

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About");
        alert.setHeaderText("About Manifest Generator");
        alert.setContentText(about);
        alert.showAndWait();
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Helpers">
    private void onBrowse() {
        progressBar.progressProperty().unbind();
        progressLabel.textProperty().unbind();

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
                palettes.addAll(paletteManager.getPalettes());

            });
            viewModel.totalPageCountInFileProperty().bind(paletteManager.totalPagesProperty());
            viewModel.totalPagesInManifestProperty().bind(paletteManager.totalManifestPagesProperty());
            viewModel.exportButtonDisabledProperty().bind(paletteManager.workingProperty());
            viewModel.printButtonDisabledProperty().bind(paletteManager.workingProperty());
            viewModel.currentIndexProperty().bind(manifestListView.getSelectionModel().selectedIndexProperty());
            new Thread(paletteManager).start();

            viewModel.setNextButtonDisabled(Boolean.FALSE);
        }
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

        helpMenuItem.setAccelerator(KeyCombination.keyCombination("F1"));
        progressBar.progressProperty().bind(viewModel.progressProperty());
        progressLabel.textProperty().bind(viewModel.progressTextProperty());
        printMenuItem.disableProperty().bind(printButton.disableProperty());
        exportMenuItem.disableProperty().bind(exportButton.disableProperty());
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

        ManifestPrinter printer = new ManifestPrinter(palettes, mainStage);
        StringBinding progressBinding = new StringBinding()
        {
            {
                super.bind(printer.messageProperty());
                super.bind(printer.progressProperty());
            }

            @Override
            protected String computeValue() {
                if(printer.getProgress() < 0) {
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
        new Thread(printer).run();
    }

    public void onHelpMenuAction() {
        // Create a custom Web view for showing help files
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Help");
        alert.setHeaderText("You are being helped!");
        alert.setContentText("I am helping you now!!");
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
