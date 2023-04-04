package net.jamnigdippold;

import javax.swing.*;
import java.util.Scanner;

public class Main {
    private static String websiteUrl;
    private static int depthOfRecursiveSearch;
    private static String languageCode;
    private static String outputPath;

    public static void main(String[] args) {
        getConsoleInput();
        WebsiteCrawler crawler = new WebsiteCrawler(websiteUrl, depthOfRecursiveSearch, languageCode, outputPath);
        crawler.startCrawling();
        System.exit(0);
    }

    private static void getConsoleInput() {
        System.out.println("Enter the website url that should be crawled");
        Scanner inputScanner = new Scanner(System.in);
        websiteUrl = inputScanner.next();

        System.out.println("Enter the depth of search");
        depthOfRecursiveSearch = inputScanner.nextInt();

        System.out.println("Enter your language code [zB de]");
        languageCode = inputScanner.next();

        System.out.println("Choose a location for the output File");
        JFileChooser chooser = new JFileChooser();
        chooser.showDialog(null, "TEST");
        outputPath = chooser.getSelectedFile().getPath();

        inputScanner.close();
    }
}