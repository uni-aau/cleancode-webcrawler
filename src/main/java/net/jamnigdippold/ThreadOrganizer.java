package net.jamnigdippold;

import java.io.FileWriter;
import java.io.IOException;

public class ThreadOrganizer {
    private WebsiteCrawler[] crawlers;
    private final String[] websiteUrls;
    private final int[] depthsOfRecursiveSearch;
    private final String[] languageCodes;
    private final String outputPath;
    private StringBuilder output;

    public ThreadOrganizer(String[] websiteUrls, int[] depthsOfRecursiveSearch, String[] languageCodes, String outputPath) {
        this.websiteUrls = websiteUrls;
        this.depthsOfRecursiveSearch = depthsOfRecursiveSearch;
        this.languageCodes = languageCodes;
        this.outputPath = outputPath;
    }

    public void startConcurrentCrawling() {
        initializeCrawlers();
        startCrawlers();
        waitForCrawlersToFinish();
        getOutputFromCrawlers();
        appendLoggingErrors();
        saveOutputToFile();
    }

    protected void initializeCrawlers() {
        crawlers = new WebsiteCrawler[websiteUrls.length];
        for (int i = 0; i < websiteUrls.length; i++) {
            crawlers[i] = new WebsiteCrawler(websiteUrls[i], depthsOfRecursiveSearch[i], languageCodes[i]);
        }
    }

    protected void startCrawlers() {
        for (WebsiteCrawler crawler : crawlers) {
            crawler.start();
        }
    }

    protected void waitForCrawlersToFinish() {
        for (WebsiteCrawler crawler : crawlers) {
            try {
                crawler.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    protected void getOutputFromCrawlers() {
        output = new StringBuilder();
        for (WebsiteCrawler crawler : crawlers) {
            output.append(crawler.getOutput());
        }
    }

    protected void appendLoggingErrors() {
        String errorLog = ErrorLogger.getInstance().getErrorLogAsString();
        output.append(errorLog);
    }

    protected void saveOutputToFile() {
        try {
            FileWriter writer = new FileWriter(outputPath);
            writer.write(output.toString());
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected WebsiteCrawler[] getCrawlers() {
        return crawlers;
    }

    protected void setCrawlers(WebsiteCrawler[] crawlers) {
        this.crawlers = crawlers;
    }

    protected String getOutput() {
        return output.toString();
    }
}
