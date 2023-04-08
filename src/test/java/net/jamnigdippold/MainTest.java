package net.jamnigdippold;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.*;
import java.util.Scanner;

public class MainTest {
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream defaultOut = System.out;
    private final PrintStream defaultErr = System.err;

    private MockedStatic<WebsiteCrawler> mockedCrawler;

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
        if (mockedCrawler != null)
            mockedCrawler.close();
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
        Assertions.assertEquals("Enter your language code [zB de]\r\n", outContent.toString());
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
        Assertions.assertEquals("Enter your language code [zB de]\r\n", outContent.toString());
        Assertions.assertEquals("ERROR: Please enter a valid language code.\r\nERROR: Please enter a valid language code.\r\n", errContent.toString());
        Assertions.assertEquals("en", Main.languageCode);
    }

    @Test
    public void testGetDepthInput() {
        mockInputScanner("3\n");

        Main.getDepthInput();

        assertTestGetDepthInput();
    }

    private void assertTestGetDepthInput() {
        Assertions.assertEquals("Enter the depth of search (how many additional Links should be analyzed)\r\n", outContent.toString());
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
        Assertions.assertEquals("Enter the depth of search (how many additional Links should be analyzed)\r\n", outContent.toString());
        Assertions.assertEquals("ERROR: Please enter a valid number.\r\nERROR: Please enter a positive number.\r\n", errContent.toString());
        Assertions.assertEquals(3, Main.depthOfRecursiveSearch);
    }

    @Test
    public void testGetWebsiteInput() {
        setUpTestGetWebsiteInput("https://www.google.com\n");

        Main.getWebsiteInput();

        assertTestGetWebsiteInput();
    }

    private void assertTestGetWebsiteInput() {
        Assertions.assertEquals("Enter the website url that should be crawled\r\n", outContent.toString());
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
        Assertions.assertEquals("Enter the website url that should be crawled\r\n", outContent.toString());
        Assertions.assertEquals("ERROR: Cannot connect to url, please enter a valid url\r\nERROR: Cannot connect to url, please enter a valid url\r\n", errContent.toString());
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
