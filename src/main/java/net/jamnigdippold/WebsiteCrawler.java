package net.jamnigdippold;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.Scanner;

// Todo muss noch umgeschrieben werden - Nur testweise
public class WebsiteCrawler {
    private String websiteUrl;
    private int depthOfRecursiveSearch;
    private String languageCode;

    // Konsoleninput könnte man sonst auch via Main Klasse realisieren und dann der Klasse über den Konstruktor die Werte übergeben
    public void startCrawling() {
        getConsoleInput();
        crawlHeadlines();
        crawlWebsiteLinks();
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

    private void crawlHeadlines() {
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

    private void crawlWebsiteLinks() {
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

    private void printCrawledHeadlines(Elements crawledHeadlineElements) {
        for(Element crawledHeadlineElement : crawledHeadlineElements) {
            System.out.println(crawledHeadlineElement);
        }

    }

    private void printCrawledLinks(Elements crawledLinkElements) {
        for(Element crawledLinkElement: crawledLinkElements) {
            System.out.println(crawledLinkElement.attr("href"));
        }
    }

}
