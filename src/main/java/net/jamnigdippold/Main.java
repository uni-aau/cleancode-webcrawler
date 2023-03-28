package net.jamnigdippold;

import java.util.Scanner;

public class Main {
    private static String websiteUrl;
    private static int depthOfRecursiveSearch;
    private static String languageCode;

    public static void main(String[] args) {
        getConsoleInput();
        WebsiteCrawler crawler = new WebsiteCrawler(websiteUrl, depthOfRecursiveSearch,languageCode);
        crawler.startCrawling();
    }

    private static void getConsoleInput() {
        System.out.println("Enter the website url that should be crawled");
        Scanner inputScanner = new Scanner(System.in);
        websiteUrl = inputScanner.next();

        System.out.println("Enter the depth of search");
        depthOfRecursiveSearch = inputScanner.nextInt();

        System.out.println("Enter your language code [zB de]");
        languageCode = inputScanner.next();
    }
}