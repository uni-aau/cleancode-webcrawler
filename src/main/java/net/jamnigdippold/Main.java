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

    public static void main(String[] args) {
        getUserInput();
        WebsiteCrawler crawler = new WebsiteCrawler(websiteUrl, depthOfRecursiveSearch, languageCode, outputPath);
        crawler.startCrawling();
        System.exit(0);
    }

    private static void getUserInput() {
        inputScanner = new Scanner(System.in);
        getWebsiteInput();
        getDepthInput();
        getLanguageInput();
        getOutputFileInput();
        inputScanner.close();
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
        System.out.println("Enter the depth of search (how many additional Links should be analyzed)");
        while (true) {
            try {
                depthOfRecursiveSearch = inputScanner.nextInt();
                if (depthOfRecursiveSearch < 0)
                    System.err.println("ERROR: Please enter a positive number.");
                else
                    break;
            } catch (InputMismatchException e) {
                System.err.println("ERROR: Please enter a valid number.");
                inputScanner.nextLine();
            }
        }
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

    private static void getInputFromFileChooser() {
        JFileChooser chooser = new JFileChooser();
        int fileChooserStatus = runFileChooser(chooser);
        interpretFileChooserStatus(fileChooserStatus);
        outputPath = chooser.getSelectedFile().getPath();
    }

    private static JFrame createFileChooserParent() {
        JFrame jFrame = new JFrame();
        jFrame.setLocationRelativeTo(null);
        jFrame.setVisible(true);
        jFrame.setExtendedState(JFrame.ICONIFIED);
        jFrame.setExtendedState(JFrame.NORMAL);
        return jFrame;
    }

    private static int runFileChooser(JFileChooser chooser) {
        chooser.setFileFilter(new FileNameExtensionFilter("Markdown File (.md)", "md"));
        JFrame parent = createFileChooserParent();
        int status = chooser.showSaveDialog(parent);
        parent.setVisible(false);
        return status;
    }

    private static void interpretFileChooserStatus(int status) {
        if (status == JFileChooser.CANCEL_OPTION) {
            System.err.println("ERROR: File choosing aborted. Stopping program.");
            System.exit(0);
        }
        if (status == JFileChooser.ERROR_OPTION) {
            System.err.println("ERROR: Unexpected error. Stopping program.");
            System.exit(-1);
        }
    }

    public static void addFileExtension() {
        if (!outputPath.endsWith(".md"))
            outputPath += ".md";
    }
}