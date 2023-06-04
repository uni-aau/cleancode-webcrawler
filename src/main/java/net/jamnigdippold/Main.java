package net.jamnigdippold;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.util.*;

public class Main {
    public static int urlInputAmount;
    public static String websiteUrl;
    public static String[] websiteUrls;
    public static int depthOfRecursiveSearch;
    public static int[] depthsOfRecursiveSearch;
    public static String languageCode;
    public static String[] languageCodes;
    public static String outputPath;
    public static Scanner inputScanner;
    public static JFileChooser fileChooser;
    public static int fileChooserStatus;

    public static void main(String[] args) {
//        getUserInput();
        WebsiteCrawler crawler = createCrawler();
        crawler.startCrawling();
//        crawler.startCrawlingMultipleUrls();
        System.exit(0);
    }

    public static WebsiteCrawler createCrawler() {
//         Todo only for testing
//        websiteUrls = new String[2];
//        websiteUrls[0] = "http://david.jamnig.net/cctest/threads/thread1";
//        websiteUrls[1] = "http://david.jamnig.net/cctest/threads/thread2";
        return new WebsiteCrawler("http://david.jamnig.net/cctest", 1, "de", "../main.md");
    }

    private static void closeScanner() {
        inputScanner.close();
    }

    protected static void setupScanner() {
        inputScanner = new Scanner(System.in);
    }

    protected static void getUserInput() {
        setupScanner();
        getAmountOfCrawlingWebsites();
        getMultipleWebsiteUrlInputs();
        getMultipleCrawlingDepthInputs();
        getMultipleLanguageInputs();
        getOutputFileInput();
        closeScanner();
    }

    public static void getAmountOfCrawlingWebsites() {
        boolean value = true;
        System.out.println("Enter the amount of website urls that should be crawled");

        while (value) {
            try {
                value = tryToGetAmountOfCrawlingWebsites();
            } catch (InputMismatchException e) {
                System.err.println("ERROR: Please enter a valid number.");
                inputScanner.nextLine();
            }
        }
        inputScanner.nextLine();
    }

    public static boolean tryToGetAmountOfCrawlingWebsites() {
        urlInputAmount = inputScanner.nextInt();
        if (urlInputAmount < 1) {
            System.err.println("ERROR: Please enter a valid url amount greater than zero!");
            return true;
        }
        return false;
    }

    public static void getMultipleWebsiteUrlInputs() {
        websiteUrls = new String[urlInputAmount];
        int currentUrl = 0;

        while (currentUrl < urlInputAmount) {
            String url = getInputUrl(currentUrl + 1);
            websiteUrls[currentUrl] = url;
            currentUrl++;
        }
    }

    public static String getInputUrl(int currentUrl) {
        System.out.println("Enter the website URL that should be crawled " + currentUrl + "/" + urlInputAmount);
        String url;
        do {
            url = inputScanner.nextLine();
            if (WebsiteCrawler.isBrokenLink(url)) {
                System.err.println("ERROR: Cannot connect to URL, please enter a valid URL");
            }
        } while (WebsiteCrawler.isBrokenLink(url));
        return url;
    }

    public static void getMultipleCrawlingDepthInputs() {
        depthsOfRecursiveSearch = new int[urlInputAmount];
        int currentUrl = 0;

        while (currentUrl < urlInputAmount) {
            getCrawlingDepth(currentUrl + 1);
            depthsOfRecursiveSearch[currentUrl] = depthOfRecursiveSearch;
            currentUrl++;
        }
    }

    public static void getCrawlingDepth(int currentUrl) {
        System.out.println("Enter the depth of search (how many additional Links should be analyzed) " + currentUrl + "/" + urlInputAmount);
        boolean value = true;

        while (value) {
            try {
                value = tryToGetCrawlingDepth();
            } catch (InputMismatchException e) {
                System.err.println("ERROR: Please enter a valid number.");
                inputScanner.nextLine();
            }
        }
        inputScanner.nextLine();
    }

    private static boolean tryToGetCrawlingDepth() {
        depthOfRecursiveSearch = inputScanner.nextInt();
        if (depthOfRecursiveSearch < 0) {
            System.err.println("ERROR: Please enter a positive number.");
        } else {
            return false;
        }
        return true;
    }

    public static void getMultipleLanguageInputs() {
        languageCodes = new String[urlInputAmount];
        int currentUrl = 0;

        while (currentUrl < urlInputAmount) {
            getLanguageCode(currentUrl + 1);
            languageCodes[currentUrl] = languageCode;
            currentUrl++;
        }
    }

    public static void getLanguageCode(int currentUrl) {
        System.out.println("Please enter the language code for the language into which the headers should be translated [e.g. de] " + currentUrl + "/" + urlInputAmount);
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