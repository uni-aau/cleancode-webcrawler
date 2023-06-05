package net.jamnigdippold;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockitoAnnotations;

import java.io.FileWriter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ThreadOrganizerTest {
    private final String[] websiteUrls = {"http://example.com", "http://exampl2.com"};
    private final int[] depthsOfRecursiveSearch = {2, 3};
    private final String[] languageCodes = {"en", "de"};
    private final String outputPath = "test.md";
    private ThreadOrganizer threadOrganizer;
    private WebsiteCrawler[] mockedCrawlers;
    private final Logger logger = ErrorLogger.getInstance();

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        threadOrganizer = new ThreadOrganizer(websiteUrls, depthsOfRecursiveSearch, languageCodes, outputPath);
        mockedCrawlers = new WebsiteCrawler[]{mock(WebsiteCrawler.class), mock(WebsiteCrawler.class)};
        threadOrganizer.setCrawlers(mockedCrawlers);
    }

    @AfterEach
    public void tearDown() {
        threadOrganizer = null;
        logger.clearLog();
    }

    @Test
    void testStartConcurrentCrawling() {
        threadOrganizer = mock(ThreadOrganizer.class);
        doCallRealMethod().when(threadOrganizer).startConcurrentCrawling();

        threadOrganizer.startConcurrentCrawling();

        verify(threadOrganizer).initializeCrawlers();
        verify(threadOrganizer).startCrawlers();
        verify(threadOrganizer).waitForCrawlersToFinish();
        verify(threadOrganizer).getOutputFromCrawlers();
        verify(threadOrganizer).saveOutputToFile();
    }

    @Test
    void testInitializeCrawlers() {
        threadOrganizer.initializeCrawlers();

        for (int i = 0; i < websiteUrls.length; i++) {
            assertEquals(websiteUrls[i], threadOrganizer.getCrawlers()[i].getWebsiteUrl());
            assertEquals(depthsOfRecursiveSearch[i], threadOrganizer.getCrawlers()[i].getMaxDepthOfRecursiveSearch());
            assertEquals(languageCodes[i], threadOrganizer.getCrawlers()[i].getTargetLanguage());
        }
    }

    @Test
    void testStartCrawlers() {
        threadOrganizer.startCrawlers();

        for (WebsiteCrawler websiteCrawler : mockedCrawlers) {
            verify(websiteCrawler).start();
        }
    }

    @Test
    void testWaitForCrawlersToFinish() throws InterruptedException {
        threadOrganizer.waitForCrawlersToFinish();

        for (WebsiteCrawler websiteCrawler : mockedCrawlers) {
            verify(websiteCrawler).join();
        }
    }

    @Test
    void testWaitForCrawlersToFinishException() throws InterruptedException {
        doThrow(new InterruptedException()).when(mockedCrawlers[0]).join();

        threadOrganizer.waitForCrawlersToFinish();

        assertEquals("Error while waiting for crawlers: java.lang.InterruptedException", logger.getErrorLog().get(0));
    }

    @Test
    void testGetOutputFromCrawlers() {
        doReturn("Mocked Output 1!\n").when(mockedCrawlers[0]).getOutput();
        doReturn("Mocked Output 2!\n").when(mockedCrawlers[1]).getOutput();

        threadOrganizer.getOutputFromCrawlers();

        assertEquals("Mocked Output 1!\nMocked Output 2!\n", threadOrganizer.getOutput());
    }

    @Test
    void testSaveOutputToFile() throws Exception {
        MockedConstruction<FileWriter> mockedConstruction = mockConstruction(FileWriter.class);
        threadOrganizer.getOutputFromCrawlers();

        threadOrganizer.saveOutputToFile();

        verify(mockedConstruction.constructed().get(0)).write(threadOrganizer.getOutput());
        verify(mockedConstruction.constructed().get(0)).close();

        mockedConstruction.close();
    }

//    @Test // todo build hat persönliches problem dagegen (linux)
    void testSaveOutputToFileException() {
        String invalidFilePath = "C:\\Windows\\System32\\test.txt";
        threadOrganizer = new ThreadOrganizer(websiteUrls, depthsOfRecursiveSearch, languageCodes, invalidFilePath);

        threadOrganizer.saveOutputToFile();

        assertEquals("Error while closing file writer java.io.FileNotFoundException: C:\\Windows\\System32\\test.txt (Zugriff verweigert)", logger.getErrorLog().get(0));
    }
}
