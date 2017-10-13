/*
 * Written by WCUPA Computer Science club members
 * November 2016
 */
package manifestgenerator.models;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;

/**
 * @author Mohamed Alie Pussah (mp754927@wcupa.edu)
 * Encapsulates actions for efficiently parsing a CSV file
 * to extract predefined palette data
 */
public class PaletteManager extends Task<ObservableList<Palette>>
{
    private final ReadOnlyObjectWrapper<ObservableList<Palette>> palettes;
    private final ReadOnlyIntegerWrapper totalPages;
    private final ReadOnlyIntegerWrapper totalManifestPages;
    private final ReadOnlyBooleanWrapper working;
    private final File file;
    private final File temporaryFile;
    private final HashMap<String, String> summaryHeadersOnCurrentPage;
    private final ArrayList<Column> headersOnCurrentPage;
    private final ArrayList<Cases> casesOnCurrentPage;
    private int pageNumber;
    private int rowCount;

    private final Pattern SUMMARY_HEADER_PATTERN;
    private final Pattern COLUMN_HEADER_PATTERN;
    private final Pattern DATA_ROW_PATTERN;
    private final Pattern PAGE_NUMBER_PATTERN;
    private final String WANTED_COLUMNS;

    public PaletteManager(File csvFile) {
        palettes = new ReadOnlyObjectWrapper<>(this, "palettes", FXCollections.observableArrayList());
        file = csvFile;
        temporaryFile = new File("temp.csv");
        updateProgress(temporaryFile.length(), file.length());
        updateMessage("Getting Started...");
        summaryHeadersOnCurrentPage = new HashMap<>();
        headersOnCurrentPage = new ArrayList<>();
        casesOnCurrentPage = new ArrayList<>();
        totalPages = new ReadOnlyIntegerWrapper(0);
        totalManifestPages = new ReadOnlyIntegerWrapper();
        working = new ReadOnlyBooleanWrapper(true);

        SUMMARY_HEADER_PATTERN = Pattern.compile("\\b(ROUTE|WRIN|TRAILER\\s*(POSITION|POS)|STOP|CASES|DESCRIPTION)\\s*:{1}\\s*\\,+\\w+\\b");
        COLUMN_HEADER_PATTERN = Pattern.compile("^,+(ROUTE|WRIN|TRAILER\\s*(POSITION|POS)|STOP|CASES|DESCRIPTION)+\\,+.*$");
        DATA_ROW_PATTERN = Pattern.compile("^,*(\\b(?:\\d*\\.)?\\d+\\b\\,+)+\\b(\\w+(\\s|[\\/\\-\\_])?)+\\b\\s*\\,+(\\b(?:\\d*\\.)?\\d+\\b\\,+)\\b\\w+\\b\\,+.*\\b(\\b(?:\\d*\\.)?\\d+\\b)\\,*$");
        //PAGE_NUMBER_PATTERN = Pattern.compile("^\\bPage\\s\\d+\\b(?=\\s*of\\,+\\d+\\,+.*$)");
        PAGE_NUMBER_PATTERN = Pattern.compile("^\\bPage\\s\\d+");
        WANTED_COLUMNS = "ROUTE,WRIN,TRAILER POSITION,STOP,CASES,DESCRIPTION,TRAILER POS";
    }

    private ArrayList<Column> getHeadersOnCurrentPage() {
        Collections.sort(headersOnCurrentPage);
        return headersOnCurrentPage;
    }

    private boolean isEndOfCurrentPage(String line) {
        Matcher matcher = PAGE_NUMBER_PATTERN.matcher(line);
        return matcher.find();
    }

    private void updateSummaryHeadersOnCurrentPage(String line) {        
        Matcher matcher = SUMMARY_HEADER_PATTERN.matcher(line);
        while (matcher.find()) {
            String match = matcher.group();
            String header = match.substring(0, match.indexOf(":")).trim();
            String value = match.substring(match.lastIndexOf(",") + 1).trim();
            summaryHeadersOnCurrentPage.put(header, value);
        }
    }

    private void updateHeadersOnCurrentPage(String line) {
        Matcher matcher = COLUMN_HEADER_PATTERN.matcher(line);
        if (!matcher.matches()) {
            return;
        }
        int column = 0;
        /*
         * I am using String.split() here because it provides
         * /* an accurate count of columns in the file. Normally,
         * /* you will want to use the group() method from the
         * /* matcher
         */
        Queue<String> row = new LinkedList<>(Arrays.asList(matcher.group().split(",")));
        while (!row.isEmpty()) {
            String data = row.poll().trim();
            column++;
            if (data == null || "".equals(data) || !WANTED_COLUMNS.contains(data)) {
                continue;
            }

            Cell header = new Cell(rowCount, column, data);
            headersOnCurrentPage.add(new Column(header));
        }
    }

    private void processCases(Column column, Cases cases, String data) throws NumberFormatException {
        switch (column.HEADER.VALUE.toLowerCase()) {
            case "route":
                cases.setRoute(data);
                break;
            case "stop":
                cases.setStop(data);
                break;
            case "cases":
                cases.increaseQuantityBy(Integer.parseInt(data));
                break;
            case "description":
                cases.setContent(data);
                break;
            case "wrin":
                cases.setContentId(data);
                break;
            case "trailer":
            case "trailer pos":
            case "trailer position":
                cases.setTrailerPosition(data);
        }
    }

    private void updateCasesOnCurrentPage(String line) {
        Matcher matcher = DATA_ROW_PATTERN.matcher(line);
        if (!matcher.matches()) {
            return;
        }
        // See getColumnHeaders for reasoning behind String.split()
        Queue<String> row = new LinkedList<>(Arrays.asList(matcher.group().split(",")));
        Cases cases = new Cases();
        int dataColumn = 0;
        for (Column column : getHeadersOnCurrentPage()) {
            while (!row.isEmpty()) {
                String data = row.poll().trim();
                dataColumn++;
                if (data == null || "".equals(data)) {
                    continue;
                }

                if (Math.abs(dataColumn - column.HEADER.COLUMN) > 1) {
                    continue;
                }
                processCases(column, cases, data);
                break;
            }
        }

        if (cases.getRoute().equals("")) {
            String route = summaryHeadersOnCurrentPage.get("ROUTE");
            cases.setRoute(route);
        }
        casesOnCurrentPage.add(cases);
    }

    private boolean casesOnCurrentPageBelongToTheSameStop() {
        String stop = casesOnCurrentPage.get(0).getStop();
        return casesOnCurrentPage.stream().allMatch(cases -> cases.getStop().equals(stop));
    }

    private int theSumOfAllCasesOnCurrentPage() {
        return casesOnCurrentPage.stream().mapToInt(cases -> cases.getQuantity()).sum();
    }

    private ArrayList<Palette> getProperlyStackedPalettes(ArrayList<Palette> palettes, int difference) {
        final int FIRST = 0;
        final int SECOND = 1;
        final int CASES_TO_REMOVE = 1;

        if (difference == 0 || difference == 1) {
            return palettes;
        }

        if ((difference < 6 && difference > 1) && palettes.get(FIRST).CASES.size() == 1) {
            return palettes;
        }

        String lastCaseId = palettes.get(FIRST).getLastCaseId();
        Cases cases = palettes.get(FIRST).removeCases(lastCaseId, CASES_TO_REMOVE);
        palettes.get(SECOND).addCases(cases);

        int newDifference = palettes.get(FIRST).getCaseCount()
                - palettes.get(SECOND).getCaseCount();

        return getProperlyStackedPalettes(palettes, newDifference);
    }

    private HashMap<String, ArrayList<Cases>> separateCasesOnCurrentPageByStop() {
        HashMap<String, ArrayList<Cases>> casesPerStop = new HashMap<>();
        for (Cases cases : casesOnCurrentPage) {
            if (casesPerStop.containsKey(cases.getStop())) {
                casesPerStop.get(cases.getStop()).add(cases);
                continue;
            }
            ArrayList<Cases> casesList = new ArrayList<>();
            casesList.add(cases);
            casesPerStop.put(cases.getStop(), casesList);
        }        
        return casesPerStop;
    }

    private ArrayList<Palette> getPalettesOnCurrentPage(int pageNumber) {
        if (casesOnCurrentPage.isEmpty()) {
            return new ArrayList<>();
        }

        final int MAX_PALETTES_PER_PAGE = 2;
        final int MAX_CASE_COUNT_PER_PALETTE = 21;
        ArrayList<Palette> palettesOnCurrentPage = new ArrayList<>(MAX_PALETTES_PER_PAGE);
        String trailerPosition = casesOnCurrentPage.get(0).getTrailerPosition();
        int theSumOfAllCasesOnCurrentPage = theSumOfAllCasesOnCurrentPage();

        // All cases on this page are going to the same stop
        // If the total number of cases on the page is <= 21,
        // Simply create a single palette, add it to the page's
        // palettes and return it
        if (theSumOfAllCasesOnCurrentPage <= MAX_CASE_COUNT_PER_PALETTE) {
            Palette palette = new Palette(trailerPosition);
            palette.setReferencePage(pageNumber);
            casesOnCurrentPage.forEach(cases -> palette.addCases(cases));
            palettesOnCurrentPage.add(palette);
            return palettesOnCurrentPage;
        }

        int midway = (theSumOfAllCasesOnCurrentPage % 2 == 0)
                ? theSumOfAllCasesOnCurrentPage / 2
                : (theSumOfAllCasesOnCurrentPage / 2) + 1;

        if (casesOnCurrentPageBelongToTheSameStop()) {
            return getStackedPalettes(trailerPosition, pageNumber, midway, palettesOnCurrentPage);
        }

        return getPalettesOnCurrentPage(pageNumber, separateCasesOnCurrentPageByStop(), midway);
    }

    private ArrayList<Palette> getStackedPalettes(String trailerPosition, int pageNumber1, int midway, ArrayList<Palette> palettesOnCurrentPage) {
        Palette firstPalette = new Palette(trailerPosition);
        firstPalette.setReferencePage(pageNumber1);
        Cases remainder = null;
        while (firstPalette.getCaseCount() < midway) {
            remainder = firstPalette.addCases(casesOnCurrentPage.remove(0));
            if (remainder != null) {
                break;
            }
        }
        palettesOnCurrentPage.add(firstPalette);
        if (casesOnCurrentPage.isEmpty()) {
            if (remainder == null) {
                return palettesOnCurrentPage;
            }
            Palette secondPalette = new Palette(trailerPosition);
            secondPalette.setReferencePage(pageNumber1);
            secondPalette.addCases(remainder);
            palettesOnCurrentPage.add(secondPalette);
            return getProperlyStackedPalettes(palettesOnCurrentPage,
                    firstPalette.getCaseCount() - secondPalette.getCaseCount());
        }
        Palette secondPalette = new Palette(trailerPosition);
        secondPalette.setReferencePage(pageNumber1);
        if (remainder != null) {
            secondPalette.addCases(remainder);
        }
        casesOnCurrentPage.forEach(cases -> secondPalette.addCases(cases));
        palettesOnCurrentPage.add(secondPalette);
        return getProperlyStackedPalettes(palettesOnCurrentPage,
                firstPalette.getCaseCount() - secondPalette.getCaseCount());
    }

    private ArrayList<Palette> getPalettesOnCurrentPage(int pageNumber, HashMap<String, ArrayList<Cases>> casesPerStop, int midway) {

        if (casesPerStop.isEmpty()) {
            return new ArrayList<>();
        }

        ArrayList<Palette> palettesOnCurrentPage = new ArrayList<>();
        casesOnCurrentPage.clear();
        for(String stop : casesPerStop.keySet()){
            if(casesPerStop.get(stop).size() > 0) {
                casesOnCurrentPage.addAll(casesPerStop.get(stop));
            }
        }
        String trailerPosition = casesOnCurrentPage.get(0).getTrailerPosition();
        return getStackedPalettes(trailerPosition, pageNumber, midway, palettesOnCurrentPage);
    }

    @Override
    protected ObservableList call() throws Exception {
        updateMessage("Opening File...");
        updateProgress(temporaryFile.length(), file.length());
        FileReader fileReader = new FileReader(file);
        BufferedReader reader = new BufferedReader(fileReader);
        FileWriter fileWriter = new FileWriter(temporaryFile);
        BufferedWriter writer = new BufferedWriter(fileWriter);
        String line;
        while ((line = reader.readLine()) != null) {
            writer.write(line);
            rowCount++;
            if (isEndOfCurrentPage(line)) {
                pageNumber++;
                writer.flush();
                updateProgress(temporaryFile.length(), file.length());
                updateMessage(String.format("Processing Page %s of file...", pageNumber));
                Collections.sort(casesOnCurrentPage, (Cases first, Cases second) -> second.getQuantity() - first.getQuantity());
                final ArrayList<Palette> palettesOnCurrentPage = getPalettesOnCurrentPage(pageNumber);
                Collections.sort(palettesOnCurrentPage);
                Platform.runLater(new Runnable()
                {
                    @Override
                    public void run() {
                        if (!palettesOnCurrentPage.isEmpty()) {
                            palettes.get().addAll(palettesOnCurrentPage);
                        }
                    }
                });
                summaryHeadersOnCurrentPage.clear();
                headersOnCurrentPage.clear();
                casesOnCurrentPage.clear();
                continue;
            }

            updateSummaryHeadersOnCurrentPage(line);
            updateHeadersOnCurrentPage(line);
            updateCasesOnCurrentPage(line);
        }

        fileReader.close();
        reader.close();
        fileWriter.close();
        writer.close();
        updateMessage("Returning Manifests...");
        updateProgress(temporaryFile.length(), file.length());
        return palettes.get();
    }

    @Override
    protected void succeeded() {
        super.succeeded();
        // TODO: Add control for buttons and manifest list view selection index
        totalPages.set(pageNumber);
        totalManifestPages.set(getPalettes().size());
        updateProgress(file.length(), file.length());
        updateMessage("Done Generating Manifests...");
        temporaryFile.deleteOnExit();
        working.set(false);
    }

    @Override
    protected void cancelled() {
        super.cancelled();
        updateMessage("Cancelled!");
    }

    @Override
    protected void failed() {
        super.failed();
        // TODO: Log error and notify user
        System.out.println(getException().getMessage());
        updateMessage("Something went wrong!");
    }

    public final ObservableList getPalettes() {
        return palettes.get();
    }

    public final ReadOnlyObjectProperty palettesProperty() {
        return palettes.getReadOnlyProperty();
    }

    public final int getTotalPages() {
        return totalPages.get();
    }

    public final ReadOnlyIntegerProperty totalPagesProperty() {
        return totalPages.getReadOnlyProperty();
    }

    public final int getTotalManifestPages() {
        return totalManifestPages.get();
    }

    public final ReadOnlyIntegerProperty totalManifestPagesProperty() {
        return totalManifestPages.getReadOnlyProperty();
    }

    public final boolean getWorking() {
        return working.get();
    }

    public final ReadOnlyBooleanProperty workingProperty() {
        return working.getReadOnlyProperty();
    }
}
