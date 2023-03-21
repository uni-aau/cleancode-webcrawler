package net.jamnigdippold;

import java.util.Scanner;

// Todo muss noch umgeschrieben werden - Nur testweise
public class WebsiteCrawler {
    private String websiteUrl;
    private int depthOfRecursiveSearch;
    private String languageCode;

    public void startCrawling() {
        getConsoleInput();
        crawlWebsite();
    }

    private void getConsoleInput() {
        System.out.println("Enter the website url that should be crawled");
        Scanner inputScanner = new Scanner(System.in);
        websiteUrl = inputScanner.next();

        System.out.println("Enter the depth of search");
        depthOfRecursiveSearch = inputScanner.nextInt();

        System.out.println("Enter your language code [zB de_DE]");
        languageCode = inputScanner.next();
    }

    private void crawlWebsite() {

    }
}
