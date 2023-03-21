package net.jamnigdippold;

import java.util.Scanner;

// Todo muss noch umgeschrieben werden - Nur testweise
public class WebsiteCrawler {
    private String websiteUrl;
    private int depthOfRecursiveSearch;
    private String languageCode;

    public void startCrawling() {
        getConsoleInput();
        crawlHeadlines(websiteUrl);
        crawlWebsiteLinks(websiteUrl);
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

    private void crawlHeadlines(String websiteUrl) {
        Elements headlineSelection;
        Document websiteDocumentConnection;
        try {
            websiteDocumentConnection = Jsoup.connect(websiteUrl).get();
            headlineSelection = websiteDocumentConnection.select("h1, h2, h3, h4, h5, h6");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        printCrawledHeadlines(headlineSelection);
    }

    private void crawlWebsiteLinks(String websiteUrl) {
        Elements linksSelection;
        Document websiteDocumentConnection;
        try {
            websiteDocumentConnection = Jsoup.connect(websiteUrl).get();
            linksSelection = websiteDocumentConnection.select("a[href]");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        printCrawledLinks(linksSelection);
    }
}
