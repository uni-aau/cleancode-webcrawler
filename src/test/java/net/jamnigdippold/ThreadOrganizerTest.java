package net.jamnigdippold;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockitoAnnotations;

import java.io.FileWriter;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class ThreadOrganizerTest {
    private final String[] websiteUrls = {"http://example.com", "http://exampl2.com"};
    private final int[] maxDepthsOfRecursiveSearch = {2, 3};
    private final String[] languageCodes = {"en", "de"};
    private final String outputPath = "test.md";
    private ThreadOrganizer threadOrganizer;
    private CrawlerLauncher launcher;
    private final Logger logger = ErrorLogger.getInstance();

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        threadOrganizer = new ThreadOrganizer(websiteUrls, maxDepthsOfRecursiveSearch, languageCodes, outputPath);
        launcher = mock(CrawlerLauncher.class);
        threadOrganizer.setLauncher(launcher);
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

        verify(threadOrganizer).startCrawlers();
        verify(threadOrganizer).getOutputFromCrawlers();
        verify(threadOrganizer).appendLoggingErrors();
        verify(threadOrganizer).saveOutputToFile();
    }

    @Test
    void testStartCrawlers() {
        threadOrganizer.startCrawlers();

        for (int i = 0; i < websiteUrls.length; i++) {
            verify(launcher).startNewCrawler(websiteUrls[i], maxDepthsOfRecursiveSearch[i], languageCodes[i], 0);
        }
    }

    @Test
    void testGetOutputFromCrawlers() {
        doReturn("All outputs").when(launcher).getOutputFromCrawlers();

        threadOrganizer.getOutputFromCrawlers();

        assertEquals("All outputs", threadOrganizer.getOutput());
        verify(launcher).waitForCrawlerThreadsToFinish();
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

    @Test
    void testAppendLoggingErrorsToOutput() {
        String expectedOutput = "# <br> ------- ERRORS ------- <br>\n- No errors thrown while executing program <br>\n";

        threadOrganizer.appendLoggingErrors();

        assertEquals(expectedOutput, threadOrganizer.getOutput());
    }

    @Test
    void testSaveOutputToFileException() {
        MockedConstruction<FileWriter> mockedConstruction = mockConstruction(FileWriter.class, (mock, context) -> {
            doThrow(new IOException("java.io.FileNotFoundException in line 100")).when(mock).write(anyString());
        });

        threadOrganizer.saveOutputToFile();

        assertEquals("Error while closing file writer: java.io.IOException: java.io.FileNotFoundException in line 100", logger.getErrorLog().get(0));
        mockedConstruction.close();
    }
}
