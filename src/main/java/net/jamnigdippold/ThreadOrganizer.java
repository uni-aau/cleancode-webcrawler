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
        saveOutputToFile();
    }

    private void initializeCrawlers() {
        crawlers = new WebsiteCrawler[websiteUrls.length];
        for (int i = 0; i < websiteUrls.length; i++) {
            crawlers[i] = new WebsiteCrawler(websiteUrls[i], depthsOfRecursiveSearch[i], languageCodes[i]);
        }
    }

    private void startCrawlers() {
        for (WebsiteCrawler crawler : crawlers) {
            crawler.start();
        }
    }

    private void waitForCrawlersToFinish() {
        for (WebsiteCrawler crawler : crawlers) {
            try {
                crawler.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void getOutputFromCrawlers() {
        output = new StringBuilder();
        for (WebsiteCrawler crawler : crawlers) {
            output.append(crawler.getOutput());
        }
    }

    private void saveOutputToFile() {
        try {
            FileWriter writer = new FileWriter(outputPath);
            writer.write(output.toString());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
