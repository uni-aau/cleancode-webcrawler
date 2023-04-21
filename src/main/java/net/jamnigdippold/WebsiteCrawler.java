package net.jamnigdippold;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
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
    private Elements crawledHeadlineElements;
    private List<String> crawledLinks;
    private String sourceLanguage;
    private String targetLanguage;
    private FileWriter fileWriter;
    private OkHttpClient client = new OkHttpClient();

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
        this.sourceLanguage = "auto";
        this.currentDepthOfRecursiveSearch = currentDepthOfRecursiveSearch;
        this.fileWriter = writer;
    }

    public void startCrawling() {
        establishConnection();
        crawlHeadlines();
        setSourceLanguage();
        printInput();
        printCrawledHeadlines();
        crawlWebsiteLinks();
        recursivelyCrawlLinkedWebsites();
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
        crawledHeadlineElements = websiteDocumentConnection.select("h1, h2, h3, h4, h5, h6");
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
    protected void recursivelyCrawlLinkedWebsites() {
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

    protected void setSourceLanguage() {
        String headline = crawledHeadlineElements.get(0).text();
        sourceLanguage = getLanguageCodeFromHeadline(headline);
    }

    protected void printCrawledHeadlines() {
        for (Element crawledHeadlineElement : crawledHeadlineElements) {
            printHeaderLevel(crawledHeadlineElement);
            if (currentDepthOfRecursiveSearch > 0) {
                printDepthIndicator();
            }
            printString(getTranslatedHeadline(crawledHeadlineElement.text()) + "\n");
        }
        printString("\n");
    }

    protected void printHeaderLevel(Element crawledHeadlineElement) {
        int numOfHeader = (crawledHeadlineElement.normalName().charAt(1)) - '0'; // Todo description comment?
        for (int i = 0; i < numOfHeader; i++) {
            printString("#");
        }
        printString(" ");
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

    protected RequestBody createNewRequestBody(String headerText) {
        return new FormBody.Builder()
                .add("source_language", sourceLanguage)
                .add("target_language", targetLanguage)
                .add("text", headerText)
                .build();
    }

    protected Request createTranslationApiRequest(RequestBody body) {
        String apiKey = getApiKey();
        return new Request.Builder()
                .url("https://text-translator2.p.rapidapi.com/translate")
                .post(body)
                .addHeader("content-type", "application/x-www-form-urlencoded")
                .addHeader("X-RapidAPI-Key", apiKey)
                .addHeader("X-RapidAPI-Host", "text-translator2.p.rapidapi.com")
                .build();
    }

    protected String getApiKey() {
        return System.getenv("RAPIDAPI_API_KEY");
    }

    protected Response executeTranslationApiRequest(Request translationApiRequest) {
        try {
            return client.newCall(translationApiRequest).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected String extractTranslatedText(Response apiResponse) {
        try {
            return extractTranslation(apiResponse);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected String extractTranslation(Response apiResponse) throws IOException {
        String apiResponseBody;
        JsonNode node;

        apiResponseBody = apiResponse.body().string();
        node = new ObjectMapper().readTree(apiResponseBody);

        if (checkNodeSuccessStatus(node)) {
            return node.get("data").get("translatedText").asText();
        } else {
            return null;
        }
    }

    private boolean checkNodeSuccessStatus(JsonNode node) {
        return node.get("status").asText().equals("success");
    }

    protected String extractLanguageCode(Response apiResponse) {
        try {
            return tryToExtractLanguageCode(apiResponse);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    protected String tryToExtractLanguageCode(Response apiResponse) throws IOException {
        String apiResponseBody;
        JsonNode node;

        apiResponseBody = apiResponse.body().string();
        node = new ObjectMapper().readTree(apiResponseBody);

        return node.get("data").get("detectedSourceLanguage").get("code").asText();

    }

    protected String getTranslatedHeadline(String crawledHeadlineText) {
        Response apiResponse = executeAPIRequest(crawledHeadlineText);
        String translatedString = extractTranslatedText(apiResponse);
        if (translatedString == null)
            translatedString = crawledHeadlineText;

        return translatedString;
    }

    protected String getLanguageCodeFromHeadline(String crawledHeadlineText) {
        Response apiResponse = executeAPIRequest(crawledHeadlineText);

        return extractLanguageCode(apiResponse);
    }

    protected Response executeAPIRequest(String crawledHeadlineText) {
        RequestBody body = createNewRequestBody(crawledHeadlineText);
        Request request = createTranslationApiRequest(body);

        return executeTranslationApiRequest(request);
    }

    public void flushWriter() throws IOException {
        fileWriter.flush();
    }

    public Elements getCrawledHeadlineElements() {
        return crawledHeadlineElements;
    }

    public void setCrawledHeadlineElements(Elements crawledHeadlineElements) {
        this.crawledHeadlineElements = crawledHeadlineElements;
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

    public void setClient(OkHttpClient client) {
        this.client = client;
    }

    public FileWriter getFileWriter() {
        return fileWriter;
    }

    public void setFileWriter(FileWriter fileWriter) {
        this.fileWriter = fileWriter;
    }

    public String getSourceLanguage() {
        return sourceLanguage;
    }
}
