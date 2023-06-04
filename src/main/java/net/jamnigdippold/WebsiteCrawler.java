package net.jamnigdippold;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WebsiteCrawler {
    private String websiteUrl;
    private int maxDepthOfRecursiveSearch;
    private int currentDepthOfRecursiveSearch;
    private Document websiteDocumentConnection;
    private Elements crawledHeadlines;
    private List<String> crawledLinks;
    private String sourceLanguage = "auto";
    private String targetLanguage;
    private FileWriter fileWriter;
    private Translator translator;

    public WebsiteCrawler(String websiteUrl, int maxDepthOfRecursiveSearch, String targetLanguage, String outputPath) {
        createFileWriter(outputPath);
        initializeValues(websiteUrl, maxDepthOfRecursiveSearch, targetLanguage, 0, fileWriter);
    }

    public WebsiteCrawler(String websiteUrl, int maxDepthOfRecursiveSearch, String targetLanguage, int currentDepthOfRecursiveSearch, FileWriter writer) {
        initializeValues(websiteUrl, maxDepthOfRecursiveSearch, targetLanguage, currentDepthOfRecursiveSearch, writer);
    }

    protected static boolean isBrokenLink(String crawledLink) {
        try {
            Jsoup.connect(crawledLink).get();
            return false;
        } catch (IOException | IllegalArgumentException exception) {
            return true;
        }
    }

    protected void createFileWriter(String outputPath) {
        try {
            fileWriter = new FileWriter(outputPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void initializeValues(String websiteUrl, int maxDepthOfRecursiveSearch, String targetLanguage, int currentDepthOfRecursiveSearch, FileWriter writer) {
        this.websiteUrl = websiteUrl;
        this.maxDepthOfRecursiveSearch = maxDepthOfRecursiveSearch;
        this.targetLanguage = targetLanguage;
        this.currentDepthOfRecursiveSearch = currentDepthOfRecursiveSearch;
        this.fileWriter = writer;
        this.sourceLanguage = "auto";
    }

    public void startCrawling() {
        establishConnection();
        crawlHeadlines();
        initializeTranslator();
//        setSourceLanguage();
        printInput();
        printCrawledHeadlines();
        crawlWebsiteLinks();
        recursivelyPrintCrawledWebsites();
        closeWriter();
    }

    protected void printInput() {
        if (currentDepthOfRecursiveSearch == 0) {
            printString("input: <a>" + websiteUrl + "</a>\n");
            printString("<br>depth: " + maxDepthOfRecursiveSearch + "\n");
            printString("<br>source language: " + sourceLanguage + "\n");
            printString("<br>Target language: " + targetLanguage + "\n");
            printString("<br>summary:\n");
        }
    }

    protected void establishConnection() {
        try {
            websiteDocumentConnection = Jsoup.connect(websiteUrl).get();
        } catch (IOException e) {
            throw new RuntimeException();
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


    // Hint: To uphold desired output format,
    // start new crawler & print links should happen here
    protected void recursivelyPrintCrawledWebsites() {
        for (String crawledLink : crawledLinks) {
            crawledLink = convertRelativeUrlToAbsoluteURL(crawledLink);
            boolean isBrokenLink = isBrokenLink(crawledLink);
            printCrawledLink(crawledLink, isBrokenLink);
            if (!isBrokenLink) {
                startNewCrawler(crawledLink);
            }
        }
    }

    protected String convertRelativeUrlToAbsoluteURL(String relativeUrl) {
        String absoluteUrl = relativeUrl;
        if (!relativeUrl.startsWith("http"))
            absoluteUrl = websiteUrl + relativeUrl.substring(1);

        return absoluteUrl;
    }

    private void startNewCrawler(String crawledLink) {
        if (currentDepthOfRecursiveSearch < maxDepthOfRecursiveSearch) {
            WebsiteCrawler recursiveCrawler = new WebsiteCrawler(crawledLink, maxDepthOfRecursiveSearch, targetLanguage, currentDepthOfRecursiveSearch + 1, fileWriter);
            recursiveCrawler.startCrawling();
        }
    }

    protected void initializeTranslator() {
        translator = new TranslatorNew();
        translator.setTargetLanguage(targetLanguage);
    }

/*    protected void setSourceLanguage() {
        translator.setTranslationSourceLanguage(crawledHeadlines);
        sourceLanguage = translator.getSourceLanguage();
    }*/

    protected void printCrawledHeadlines() {
        for (Element crawledHeadline : crawledHeadlines) {
            printHeaderLevel(crawledHeadline);
            if (currentDepthOfRecursiveSearch > 0) {
                printDepthIndicator();
            }
            printString(translator.translate(crawledHeadline.text()) + "\n");
        }
        printString("\n");
    }

    protected void printHeaderLevel(Element crawledHeadline) {
        int numOfHeader = getHeaderLevelFromName(crawledHeadline.normalName());
        for (int i = 0; i < numOfHeader; i++) {
            printString("#");
        }
        printString(" ");
    }

    protected int getHeaderLevelFromName(String headerLevelName) {
        //expected headerLevelNames follow the format "h1", "h2", ... , "h6"
        String headerNumber = headerLevelName.substring(1);
        return Integer.parseInt(headerNumber);
    }

    protected void printCrawledLink(String crawledLink, boolean isBrokenLink) {
        printString("<br>--");
        printDepthIndicator();
        if (isBrokenLink) printString("broken link <a>");
        else printString("link to <a>");
        printString(crawledLink);
        printString("</a>\n\n");
    }

    protected void printDepthIndicator() {
        for (int i = 0; i < currentDepthOfRecursiveSearch; i++) {
            printString("--");
        }
        printString("> ");
    }

    protected void printString(String printable) {
        System.out.print(printable);
        try {
            fileWriter.write(printable);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void closeWriter() {
        try {
            tryCloseWriter();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void tryCloseWriter() throws IOException {
        if (currentDepthOfRecursiveSearch == 0)
            fileWriter.close();
    }

    public void flushWriter() throws IOException {
        fileWriter.flush();
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

    public FileWriter getFileWriter() {
        return fileWriter;
    }

    public void setFileWriter(FileWriter fileWriter) {
        this.fileWriter = fileWriter;
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
}
