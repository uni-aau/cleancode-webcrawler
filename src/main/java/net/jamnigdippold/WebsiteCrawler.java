package net.jamnigdippold;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WebsiteCrawler {
    private final String websiteUrl;
    private final int maxDepthOfRecursiveSearch;
    private final int currentDepthOfRecursiveSearch;
    private Document websiteDocumentConnection;
    private Elements crawledHeadlineElements;
    private List<String> crawledLinks;
    private final String sourceLanguage = "en"; // Todo Checker für Headersprache
    private final String targetLanguage;

    public WebsiteCrawler(String websiteUrl, int maxDepthOfRecursiveSearch, String targetLanguage) {
        this(websiteUrl, maxDepthOfRecursiveSearch, targetLanguage, 0);
    }

    public WebsiteCrawler(String websiteUrl, int maxDepthOfRecursiveSearch, String targetLanguage, int currentDepthOfRecursiveSearch) {
        this.websiteUrl = websiteUrl;
        this.maxDepthOfRecursiveSearch = maxDepthOfRecursiveSearch;
        this.targetLanguage = targetLanguage;
        this.currentDepthOfRecursiveSearch = currentDepthOfRecursiveSearch;
    }

    public void startCrawling() {
        printInput();
        establishConnection();
        crawlHeadlines();
        printCrawledHeadlines();
        crawlWebsiteLinks();
        recursivelyCrawlLinkedWebsites();
    }

    private void printInput() {
        if (currentDepthOfRecursiveSearch == 0) {
            System.out.println("input: <a>" + websiteUrl + "</a>");
            System.out.println("<br>depth: " + maxDepthOfRecursiveSearch);
            System.out.println("<br>source language: " + sourceLanguage);
            System.out.println("<br>Target language: " + targetLanguage);
            System.out.println("<br>summary:");
        }
    }

    private void establishConnection() {
        try {
            websiteDocumentConnection = Jsoup.connect(websiteUrl).get();
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }

    private void crawlHeadlines() {
        crawledHeadlineElements = websiteDocumentConnection.select("h1, h2, h3, h4, h5, h6");
    }

    private void crawlWebsiteLinks() {
        Elements crawledLinkElements = websiteDocumentConnection.select("a[href]");
        crawledLinks = new ArrayList<>();
        for (Element crawledLinkElement : crawledLinkElements) {
            crawledLinks.add(crawledLinkElement.attr("href"));
        }
    }

    private void recursivelyCrawlLinkedWebsites() { //TODO: function both starts new crawlers and prints links, but the printing should happen here to uphold desired output format (Link, Output of Link, Link, Output of Link ...)
        for (String crawledLink : crawledLinks) {
            crawledLink = convertRelativeUrlToAbsoluteURL(crawledLink);
            if (!isBrokenLink(crawledLink)) {
                printCrawledLink(crawledLink);
                startNewCrawler(crawledLink);
            } else {
                printBrokenLink(crawledLink);
            }
        }
    }

    private String convertRelativeUrlToAbsoluteURL(String relativeUrl) {
        String absoluteUrl = relativeUrl;
        if (!relativeUrl.startsWith("http"))
            absoluteUrl = websiteUrl + relativeUrl.substring(1);
        return absoluteUrl;
    }

    private boolean isBrokenLink(String crawledLink) {
        try {
            Jsoup.connect(crawledLink).get();
            return false;
        } catch (IOException exception) {
            return true;
        }
    }

    private void startNewCrawler(String crawledLink) {
        if (currentDepthOfRecursiveSearch <= maxDepthOfRecursiveSearch) {
            WebsiteCrawler recursiveCrawler = new WebsiteCrawler(crawledLink, maxDepthOfRecursiveSearch, targetLanguage, currentDepthOfRecursiveSearch + 1);
            recursiveCrawler.startCrawling();
        }
    }

    private void printCrawledHeadlines() {
        for (Element crawledHeadlineElement : crawledHeadlineElements) {
            printHeaderLevel(crawledHeadlineElement);
            if (currentDepthOfRecursiveSearch > 0) {
                printDepthIndicator();
            }
            System.out.println(getTranslatedHeadline(crawledHeadlineElement.text()));
        }
        System.out.println();
    }

    private void printHeaderLevel(Element crawledHeadlineElement) {
        int numOfHeader = (crawledHeadlineElement.normalName().charAt(1)) - '0';
        for (int i = 0; i < numOfHeader; i++) {
            System.out.print("#");
        }
        System.out.print(" ");
    }

    private void printCrawledLink(String crawledLink) { //TODO: decide whether "avoid duplication" or "flags are ugly"/"have few arguments" is more important
        System.out.print("<br>--");
        printDepthIndicator();
        System.out.print("link to <a>");
        System.out.print(crawledLink);
        System.out.println("</a>\n");
    }

    private void printBrokenLink(String crawledLink) {
        System.out.print("<br>--");
        printDepthIndicator();
        System.out.print("broken link <a>");
        System.out.print(crawledLink);
        System.out.println("</a>\n");
    }

    private void printDepthIndicator() {
        for (int i = 0; i < currentDepthOfRecursiveSearch; i++) {
            System.out.print("--");
        }
        System.out.print("> ");
    }

    private RequestBody createNewRequestBody(String headerText) {
        return new FormBody.Builder()
                .add("source_language", sourceLanguage)
                .add("target_language", targetLanguage)
                .add("text", headerText)
                .build();
    }

    private Request createTranslationApiRequest(RequestBody body) {
        return new Request.Builder()
                .url("https://text-translator2.p.rapidapi.com/translate")
                .post(body)
                .addHeader("content-type", "application/x-www-form-urlencoded")
                .addHeader("X-RapidAPI-Key", "fe74ad9331msh075615faa2bbedap19fc94jsn8e377fd515bc")
                .addHeader("X-RapidAPI-Host", "text-translator2.p.rapidapi.com")
                .build();
    }

    private String convertApiResponseToString(Request translationApiRequest) { // Todo 2 verschiedene Sachen in einer Methode
        OkHttpClient client = new OkHttpClient(); // Todo auslagern und umbennen?
        Response apiRequestTranslation;
        String apiRequestTranslationString;
        try {
            apiRequestTranslation = client.newCall(translationApiRequest).execute();
            apiRequestTranslationString = apiRequestTranslation.body().string();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return apiRequestTranslationString;
    }

    private String parseJsonData(String apiRequestTranslationString) {
        ObjectMapper objectMapper = new ObjectMapper(); // Todo auslagern und umbennen?
        JsonNode jsonNode;
        try {
            jsonNode = objectMapper.readTree(apiRequestTranslationString);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return jsonNode.get("data").get("translatedText").asText();
    }

    // Todo Die Methode muss noch angepasst werden
    private String getTranslatedHeadline(String crawledHeadlineText) {
        RequestBody body = createNewRequestBody(crawledHeadlineText);
        Request request = createTranslationApiRequest(body);
        String translationString = convertApiResponseToString(request);
        return parseJsonData(translationString);
    }
}
