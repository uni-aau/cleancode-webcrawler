package net.jamnigdippold;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WebsiteCrawler extends Thread {
    private static final Logger logger = ErrorLogger.getInstance();
    private String websiteUrl;
    private int maxDepthOfRecursiveSearch;
    private int currentDepthOfRecursiveSearch;
    private Document websiteDocumentConnection;
    private Elements crawledHeadlines;
    private List<String> crawledLinks;
    private String sourceLanguage = "auto";
    private String targetLanguage;
    private List<WebsiteCrawler> recursiveCrawlers;
    private StringBuilder output;
    private Translator translator;

    public WebsiteCrawler(String websiteUrl, int maxDepthOfRecursiveSearch, String targetLanguage) {
        initializeValues(websiteUrl, maxDepthOfRecursiveSearch, targetLanguage, 0);
    }

    public WebsiteCrawler(String websiteUrl, int maxDepthOfRecursiveSearch, String targetLanguage, int currentDepthOfRecursiveSearch) {
        initializeValues(websiteUrl, maxDepthOfRecursiveSearch, targetLanguage, currentDepthOfRecursiveSearch);
    }

    protected static boolean isBrokenLink(String crawledLink) {
        try {
            Jsoup.connect(crawledLink).get();
            return false;
        } catch (IOException | IllegalArgumentException exception) {
            return true;
        }
    }

    protected void initializeValues(String websiteUrl, int maxDepthOfRecursiveSearch, String targetLanguage, int currentDepthOfRecursiveSearch) {
        this.websiteUrl = websiteUrl;
        this.maxDepthOfRecursiveSearch = maxDepthOfRecursiveSearch;
        this.targetLanguage = targetLanguage;
        this.currentDepthOfRecursiveSearch = currentDepthOfRecursiveSearch;
        this.sourceLanguage = "auto";
        this.output = new StringBuilder();
        this.recursiveCrawlers = new ArrayList<>();
    }

    @Override
    public void run() {
        if (!isBrokenLink(websiteUrl)) {
            if (currentDepthOfRecursiveSearch > maxDepthOfRecursiveSearch)
                outputCrawledLink(websiteUrl, false);
            else
                startCrawling();
        } else
            outputCrawledLink(websiteUrl, true);
    }

    public void startCrawling() {
        establishConnection();
        crawlHeadlines();
        initializeTranslator();
        // setSourceLanguage();
        outputInput();
        outputCrawledHeadlines();
        crawlWebsiteLinks();
        recursivelyCrawlLinkedWebsites();
    }

    protected void outputInput() {
        if (currentDepthOfRecursiveSearch == 0) {
            output.append("input: <a>").append(websiteUrl).append("</a>\n");
            output.append("<br>depth: ").append(maxDepthOfRecursiveSearch).append("\n");
            output.append("<br>source language: ").append(sourceLanguage).append("\n");
            output.append("<br>Target language: ").append(targetLanguage).append("\n");
            output.append("<br>summary:\n");
        } else {
            outputCrawledLink(websiteUrl, false);
        }
    }

    protected void establishConnection() {
        try {
            websiteDocumentConnection = Jsoup.connect(websiteUrl).get();
        } catch (IOException e) {
            logger.logError("Error whilst connection to websiteUrl " + e.getMessage());
        }
    }

    protected void crawlHeadlines() {
        crawledHeadlines = websiteDocumentConnection.select("h1, h2, h3, h4, h5, h6");
    }

    protected void crawlWebsiteLinks() {
        Elements crawledLinkElements = websiteDocumentConnection.select("a[href]");
        crawledLinks = new ArrayList<>();
        for (Element crawledLinkElement : crawledLinkElements) {
            crawledLinks.add(crawledLinkElement.attr("href"));
        }
    }

    protected void recursivelyCrawlLinkedWebsites() {
        for (String crawledLink : crawledLinks) {
            crawledLink = convertRelativeUrlToAbsoluteURL(crawledLink);
            startNewCrawler(crawledLink);
        }
        waitForCrawlerThreads();
        appendOutputFromRecursiveCrawlers();
    }

    protected void waitForCrawlerThreads() {
        for (WebsiteCrawler crawler : recursiveCrawlers) {
            try {
                crawler.join();
            } catch (InterruptedException e) {
                logger.logError("Error whilst joining crawler threads " + e.getMessage());
            }
        }
    }

    protected void appendOutputFromRecursiveCrawlers() {
        for (WebsiteCrawler crawler : recursiveCrawlers) {
            output.append(crawler.getOutput());
        }
    }

    protected String convertRelativeUrlToAbsoluteURL(String relativeUrl) {
        String absoluteUrl = relativeUrl;
        if (!relativeUrl.startsWith("http"))
            absoluteUrl = websiteUrl + relativeUrl.substring(1);

        return absoluteUrl;
    }

    protected void startNewCrawler(String crawledLink) {
        WebsiteCrawler recursiveCrawler = new WebsiteCrawler(crawledLink, maxDepthOfRecursiveSearch, targetLanguage, currentDepthOfRecursiveSearch + 1);
        recursiveCrawler.start();
        recursiveCrawlers.add(recursiveCrawler);
    }

    protected void initializeTranslator() {
        translator = new TextTranslator();
        translator.setTargetLanguage(targetLanguage);
    }

/*    protected void setSourceLanguage() {
        translator.setTranslationSourceLanguage(crawledHeadlines);
        sourceLanguage = translator.getSourceLanguage();
    }*/

    protected void outputCrawledHeadlines() {
        for (Element crawledHeadline : crawledHeadlines) {
            outputHeaderLevel(crawledHeadline);
            if (currentDepthOfRecursiveSearch > 0) {
                outputDepthIndicator(currentDepthOfRecursiveSearch);
            }
            output.append(translator.translate(crawledHeadline.text()) + "\n");
        }
        output.append("\n");
    }

    protected void outputHeaderLevel(Element crawledHeadline) {
        int numOfHeader = getHeaderLevelFromName(crawledHeadline.normalName());
        for (int i = 0; i < numOfHeader; i++) {
            output.append("#");
        }
        output.append(" ");
    }

    protected int getHeaderLevelFromName(String headerLevelName) {
        //expected headerLevelNames follow the format "h1", "h2", ... , "h6"
        String headerNumber = headerLevelName.substring(1);
        return Integer.parseInt(headerNumber);
    }

    protected void outputCrawledLink(String crawledLink, boolean isBrokenLink) {
        output.append("<br>--");
        outputDepthIndicator(currentDepthOfRecursiveSearch - 1);
        if (isBrokenLink) output.append("broken link <a>");
        else output.append("link to <a>");
        output.append(crawledLink);
        output.append("</a>\n\n");
    }

    protected void outputDepthIndicator(int depth) {
        for (int i = 0; i < depth; i++) {
            output.append("--");
        }
        output.append("> ");
    }

    public Elements getCrawledHeadlines() {
        return crawledHeadlines;
    }

    public void setCrawledHeadlines(Elements crawledHeadlines) {
        this.crawledHeadlines = crawledHeadlines;
    }

    public List<String> getCrawledLinks() {
        return crawledLinks;
    }

    public void setCrawledLinks(List<String> crawledLinks) {
        this.crawledLinks = crawledLinks;
    }

    public void setWebsiteDocumentConnection(Document websiteDocumentConnection) {
        this.websiteDocumentConnection = websiteDocumentConnection;
    }

    public void setCurrentDepthOfRecursiveSearch(int currentDepthOfRecursiveSearch) {
        this.currentDepthOfRecursiveSearch = currentDepthOfRecursiveSearch;
    }

    public void setMaxDepthOfRecursiveSearch(int maxDepthOfRecursiveSearch) {
        this.maxDepthOfRecursiveSearch = maxDepthOfRecursiveSearch;
    }

    public String getWebsiteUrl() {
        return websiteUrl;
    }

    public int getMaxDepthOfRecursiveSearch() {
        return maxDepthOfRecursiveSearch;
    }

    public int getCurrentDepthOfRecursiveSearch() {
        return currentDepthOfRecursiveSearch;
    }

    public String getTargetLanguage() {
        return targetLanguage;
    }

    public void setTargetLanguage(String targetLanguage) {
        this.targetLanguage = targetLanguage;
    }

    public void setTranslator(Translator translator) {
        this.translator = translator;
    }

    public Translator getTranslator() {
        return translator;
    }

    public String getSourceLanguage() {
        return sourceLanguage;
    }

    public void setWebsiteUrl(String websiteUrl) {
        this.websiteUrl = websiteUrl;
    }

    public String getOutput() {
        return output.toString();
    }

    public void setUpOutput() {
        output = new StringBuilder();
    }

    public void setRecursiveCrawlers(List<WebsiteCrawler> crawlers) {
        recursiveCrawlers = crawlers;
    }
}
