package net.jamnigdippold;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.util.*;

public class Main {
    public static String websiteUrl;
    public static int depthOfRecursiveSearch;
    public static String languageCode;
    public static String outputPath;
    public static Scanner inputScanner;
    public static JFileChooser fileChooser;
    public static int fileChooserStatus;

    public static void main(String[] args) {
        getUserInput();
        WebsiteCrawler crawler = new WebsiteCrawler(websiteUrl, depthOfRecursiveSearch, languageCode, outputPath);
        crawler.startCrawling();
        System.exit(0);
    }

    private static void closeScanner() {
        inputScanner.close();
    }

    protected static void setupScanner() {
        inputScanner = new Scanner(System.in);
    }

    protected static void getUserInput() {
        setupScanner();
        getWebsiteInput();
        getDepthInput();
        getLanguageInput();
        getOutputFileInput();
        closeScanner();
    }

    public static void getWebsiteInput() {
        System.out.println("Enter the website url that should be crawled");
        websiteUrl = inputScanner.nextLine();

        while (WebsiteCrawler.isBrokenLink(websiteUrl)) {
            System.err.println("ERROR: Cannot connect to url, please enter a valid url");
            websiteUrl = inputScanner.nextLine();
        }
    }

    public static void getDepthInput() {
        boolean value = true;
        System.out.println("Enter the depth of search (how many additional Links should be analyzed)");

        while (value) {
            try {
                value = tryToGetDepthInput();
            } catch (InputMismatchException e) {
                System.err.println("ERROR: Please enter a valid number.");
                inputScanner.nextLine();
            }
        }
        inputScanner.nextLine();
    }

    private static boolean tryToGetDepthInput() {
        depthOfRecursiveSearch = inputScanner.nextInt();
        if (depthOfRecursiveSearch < 0) {
            System.err.println("ERROR: Please enter a positive number.");
        } else {
            return false;
        }
        return true;
    }

    public static void getLanguageInput() {
        System.out.println("Enter your language code [zB de]");
        languageCode = inputScanner.nextLine();

        while (!isValidLanguageCode(languageCode)) {
            System.err.println("ERROR: Please enter a valid language code.");
            languageCode = inputScanner.nextLine();
        }
    }

    private static boolean isValidLanguageCode(String code) {
        List<String> validLanguageCodes = Arrays.asList(Locale.getISOLanguages());

        return validLanguageCodes.contains(code);
    }

    public static void getOutputFileInput() {
        System.out.println("Choose a location for the output File");
        getInputFromFileChooser();
        addFileExtension();
    }

    public static void getInputFromFileChooser() {
        createFileChooser();
        runFileChooser();
        interpretFileChooserStatus();
        outputPath = fileChooser.getSelectedFile().getPath();
    }

    public static JFrame createJFrame() {
        return new JFrame();
    }

    public static JFrame createFileChooserParent() {
        JFrame frame = createJFrame();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        frame.setExtendedState(JFrame.ICONIFIED);
        frame.setExtendedState(JFrame.NORMAL);

        return frame;
    }

    public static void createFileChooser() {
        fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("Markdown File (.md)", "md"));
    }

    public static void runFileChooser() {
        JFrame parent = createFileChooserParent();
        fileChooserStatus = fileChooser.showSaveDialog(parent);
        parent.setVisible(false);
    }

    public static void interpretFileChooserStatus() {
        if (fileChooserStatus == JFileChooser.CANCEL_OPTION) {
            System.err.println("ERROR: File choosing aborted. Stopping program.");
            System.exit(0);
        }
        if (fileChooserStatus == JFileChooser.ERROR_OPTION) {
            System.err.println("ERROR: Unexpected error. Stopping program.");
            System.exit(-1);
        }
    }

    public static void addFileExtension() {
        if (!outputPath.endsWith(".md"))
            outputPath += ".md";
    }
}