package net.jamnigdippold;

import java.io.FileWriter;
import java.io.IOException;

public class ThreadOrganizer {
    private static final Logger logger = ErrorLogger.getInstance();
    private final String[] websiteUrls;
    private final int[] depthsOfRecursiveSearch;
    private final String[] languageCodes;
    private final String outputPath;
    private String output;
    private CrawlerLauncher launcher;

    public ThreadOrganizer(String[] websiteUrls, int[] depthsOfRecursiveSearch, String[] languageCodes, String outputPath) {
        this.websiteUrls = websiteUrls;
        this.depthsOfRecursiveSearch = depthsOfRecursiveSearch;
        this.languageCodes = languageCodes;
        this.outputPath = outputPath;
        this.output = "";
        this.launcher = new CrawlerLauncher();
    }

    public void startConcurrentCrawling() {
        startCrawlers();
        getOutputFromCrawlers();
        appendLoggingErrors();
        saveOutputToFile();
    }

    protected void startCrawlers() {
        for (int i = 0; i < websiteUrls.length; i++) {
            launcher.startNewCrawler(websiteUrls[i], depthsOfRecursiveSearch[i], languageCodes[i], 0);
        }
    }

    protected void getOutputFromCrawlers() {
        launcher.waitForCrawlerThreadsToFinish();
        output = launcher.getOutputFromCrawlers();
    }

    protected void appendLoggingErrors() {
        String errorLog = logger.getErrorLogAsString();
        output += errorLog;
    }

    protected void saveOutputToFile() {
        try (FileWriter writer = new FileWriter(outputPath)) {
            writer.write(output);
        } catch (IOException e) {
            logger.logError("Error while closing file writer: " + e);
        }
    }

    protected String getOutput() {
        return output;
    }

    protected void setLauncher(CrawlerLauncher launcher) {
        this.launcher = launcher;
    }
}
