package net.jamnigdippold;

import java.util.ArrayList;
import java.util.List;

public class CrawlerLauncher {
    private static final Logger logger = ErrorLogger.getInstance();
    private List<WebsiteCrawler> crawlers;

    public CrawlerLauncher() {
        crawlers = new ArrayList<>();
    }

    protected void startNewCrawler(String crawledLink, int maxDepthOfRecursiveSearch, String targetLanguage, int currentDepthOfRecursiveSearch) {
        WebsiteCrawler recursiveCrawler = new WebsiteCrawler(crawledLink, maxDepthOfRecursiveSearch, targetLanguage, currentDepthOfRecursiveSearch);
        recursiveCrawler.start();
        crawlers.add(recursiveCrawler);
    }

    protected void waitForCrawlerThreadsToFinish() {
        for (WebsiteCrawler crawler : crawlers) {
            try {
                crawler.join();
            } catch (InterruptedException e) {
                logger.logError("Error whilst joining crawler threads: " + e);
            }
        }
    }

    protected String getOutputFromCrawlers() {
        StringBuilder output = new StringBuilder();
        for (WebsiteCrawler crawler : crawlers) {
            output.append(crawler.getOutput());
        }
        return output.toString();
    }

    protected void setCrawlers(List<WebsiteCrawler> crawlers) {
        this.crawlers = crawlers;
    }
}
