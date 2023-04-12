package net.jamnigdippold;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import javax.swing.*;
import java.io.*;
import java.security.Permission;
import java.util.Scanner;

public class MainTest {
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream defaultOut = System.out;
    private final PrintStream defaultErr = System.err;

    private MockedStatic<WebsiteCrawler> mockedCrawler;
    private MockedStatic<Main> mockedMain;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }

    @AfterEach
    public void tearDown() {
        System.setOut(defaultOut);
        System.setErr(defaultErr);
        System.setSecurityManager(null);
        closeMocks();
    }

    private void closeMocks() {
        if (mockedCrawler != null) {
            mockedCrawler.close();
            mockedCrawler = null;
        }
        if (mockedMain != null) {
            mockedMain.close();
            mockedMain = null;
        }
    }

    @Test
    public void testGetInputFromFileChooser() {
        mockJFileChooser(0, "Test.txt");

        Main.getOutputFileInput();

        Assertions.assertEquals("Test.txt.md", Main.outputPath);
    }

    @Test
    public void testGetWrongInputFromFileChooser() {
        mockJFileChooser(JFileChooser.ERROR_OPTION, "Test");
        mockSystemExit();

        Assertions.assertThrows(SecurityException.class, Main::getOutputFileInput, "-1");
        Assertions.assertEquals("ERROR: Unexpected error. Stopping program." + System.getProperty("line.separator"), errContent.toString());
    }

    @Test
    public void testAbortFileChooser() {
        mockJFileChooser(JFileChooser.CANCEL_OPTION, "Test");
        mockSystemExit();

        Assertions.assertThrows(SecurityException.class, Main::getOutputFileInput, "0");
        Assertions.assertEquals("ERROR: File choosing aborted. Stopping program." + System.getProperty("line.separator"), errContent.toString());
    }

    private void mockSystemExit() {
        System.setSecurityManager(new SecurityManager() {
            @Override
            public void checkPermission(Permission perm) {
            }

            @Override
            public void checkExit(int status) {
                super.checkExit(status);
                throw new SecurityException(status + "");
            }
        });
    }

    private void mockJFileChooser(int returnCode, String chosenPath) {
        mockedMain = Mockito.mockStatic(Main.class, Mockito.CALLS_REAL_METHODS);
        mockedMain.when(Main::createFileChooser).then(invocationOnMock -> {
            Main.fileChooser = Mockito.mock(JFileChooser.class);
            Mockito.when(Main.fileChooser.showSaveDialog(Mockito.any())).thenReturn(returnCode);
            Mockito.when(Main.fileChooser.getSelectedFile()).thenReturn(new File(chosenPath));
            return null;
        });
    }

    @Test
    public void testAddFileExtension() {
        Main.outputPath = "E:\\RealFolder\\output";

        Main.addFileExtension();

        Assertions.assertEquals("E:\\RealFolder\\output.md", Main.outputPath);

    }

    @Test
    public void testUnnecessaryAddFileExtension() {
        Main.outputPath = "E:\\RealFolder\\output.md";

        Main.addFileExtension();

        Assertions.assertEquals("E:\\RealFolder\\output.md", Main.outputPath);
    }

    @Test
    public void testGetLanguageInput() {
        mockInputScanner("en");
        Main.getLanguageInput();
        assertTestGetLanguage();
    }

    private void assertTestGetLanguage() {
        Assertions.assertEquals("Enter your language code [zB de]" + System.getProperty("line.separator"), outContent.toString());
        Assertions.assertEquals("", errContent.toString());
        Assertions.assertEquals("en", Main.languageCode);
    }

    @Test
    public void testGetLanguageInputError() {
        mockInputScanner("Not a language code\nef\nen");
        Main.getLanguageInput();
        assertTestGetLanguageError();
    }

    private void assertTestGetLanguageError() {
        Assertions.assertEquals("Enter your language code [zB de]" + System.getProperty("line.separator"), outContent.toString());
        Assertions.assertEquals("ERROR: Please enter a valid language code." + System.getProperty("line.separator") + "ERROR: Please enter a valid language code." + System.getProperty("line.separator"), errContent.toString());
        Assertions.assertEquals("en", Main.languageCode);
    }

    @Test
    public void testGetDepthInput() {
        mockInputScanner("3\n");

        Main.getDepthInput();

        assertTestGetDepthInput();
    }

    private void assertTestGetDepthInput() {
        Assertions.assertEquals("Enter the depth of search (how many additional Links should be analyzed)" + System.getProperty("line.separator"), outContent.toString());
        Assertions.assertEquals("", errContent.toString());
        Assertions.assertEquals(3, Main.depthOfRecursiveSearch);
    }

    @Test
    public void testGetDepthInputError() {
        mockInputScanner("Number\n-1\n3\n");

        Main.getDepthInput();

        assertTestGetDepthInputError();
    }

    private void assertTestGetDepthInputError() {
        Assertions.assertEquals("Enter the depth of search (how many additional Links should be analyzed)" + System.getProperty("line.separator"), outContent.toString());
        Assertions.assertEquals("ERROR: Please enter a valid number." + System.getProperty("line.separator") + "ERROR: Please enter a positive number." + System.getProperty("line.separator"), errContent.toString());
        Assertions.assertEquals(3, Main.depthOfRecursiveSearch);
    }

    @Test
    public void testGetWebsiteInput() {
        setUpTestGetWebsiteInput("https://www.google.com\n");

        Main.getWebsiteInput();

        assertTestGetWebsiteInput();
    }

    private void assertTestGetWebsiteInput() {
        Assertions.assertEquals("Enter the website url that should be crawled" + System.getProperty("line.separator"), outContent.toString());
        Assertions.assertEquals("", errContent.toString());
        Assertions.assertEquals("https://www.google.com", Main.websiteUrl);
        mockedCrawler.verify(() -> WebsiteCrawler.isBrokenLink("https://www.google.com"));
    }

    @Test
    public void testGetWebsiteInputError() {
        setUpTestGetWebsiteInput("wrong URL format\nhttps://www.notARealWebsite.com\nhttps://www.google.com\n");

        Main.getWebsiteInput();

        assertTestGetWebsiteInputError();
    }

    private void assertTestGetWebsiteInputError() {
        Assertions.assertEquals("Enter the website url that should be crawled" + System.getProperty("line.separator"), outContent.toString());
        Assertions.assertEquals("ERROR: Cannot connect to url, please enter a valid url" + System.getProperty("line.separator") + "ERROR: Cannot connect to url, please enter a valid url" + System.getProperty("line.separator"), errContent.toString());
        Assertions.assertEquals("https://www.google.com", Main.websiteUrl);
    }

    private void setUpTestGetWebsiteInput(String testInput) {
        mockIsBrokenLink();
        mockInputScanner(testInput);
    }

    private void mockInputScanner(String testInput) {
        Main.inputScanner = new Scanner(new ByteArrayInputStream(testInput.getBytes()));
    }

    private void mockIsBrokenLink() {
        mockedCrawler = Mockito.mockStatic(WebsiteCrawler.class);
        mockedCrawler.when(() -> WebsiteCrawler.isBrokenLink(Mockito.anyString())).thenReturn(true);
        mockedCrawler.when(() -> WebsiteCrawler.isBrokenLink("https://www.google.com")).thenReturn(false);
        mockedCrawler.when(() -> WebsiteCrawler.isBrokenLink("wrong URL format")).thenReturn(true);
        mockedCrawler.when(() -> WebsiteCrawler.isBrokenLink("https://www.notARealWebsite.com")).thenReturn(true);
    }
}
