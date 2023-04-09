package net.jamnigdippold;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class WebsiteCrawlerTest {

    private static Document mockedDocument;
    @Mock
    FileWriter mockFileWriter;
    private static WebsiteCrawler webCrawler;
    static String htmlMock = "<html><body><h1>Heading h1</h1><a href=\"http://example.com\">Link</a> <a href=\"./relativeUrl\"></a></body></html>";

    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private final PrintStream originalOutput = System.out;
    private Elements crawledHeadlines;

    @BeforeEach
    public void setUp() {
        setUpJsoupMock();
        webCrawler.establishConnection();
        webCrawler.setFileWriter(mockFileWriter);
        System.setOut(new PrintStream(outputStream));
    }

    private void setUpJsoupMock() {
        MockitoAnnotations.openMocks(this);
        mockedDocument = Jsoup.parse(htmlMock);
        webCrawler = spy(new WebsiteCrawler("https://example.com", 1, "de", "main"));// Todo muss noch auf die jeweiligen Sachen angepasst werden

        doAnswer(invocationOnMock -> {
            webCrawler.setWebsiteDocumentConnection(mockedDocument);
            return null;
        }).when(webCrawler).establishConnection();
    }

    @AfterEach
    public void tearDown() {
        webCrawler = null;
        System.setOut(originalOutput);
    }

    @Test
    public void testHeadlineTextOutput() {
        crawledHeadlines = addElements();

        webCrawler.crawlHeadlines();

        assertEquals(crawledHeadlines.text(), webCrawler.getCrawledHeadlineElements().text());
    }

    @Test
    public void testWebsiteLinksCrawlingOutput() {
        List<String> crawledLinks = new ArrayList<>();
        crawledLinks.add("http://example.com");
        crawledLinks.add("./relativeUrl");

        webCrawler.crawlWebsiteLinks();

        assertEquals(crawledLinks, webCrawler.getCrawledLinks());
    }

    @Test
    public void testConversionAbsoluteToRelativeUrl() {
        String relativeUrl = "./relativeUrl";
        String absoluteUrl = "https://example.com/relativeUrl";

        String webCrawlerConversionOutput = webCrawler.convertRelativeUrlToAbsoluteURL(relativeUrl);

        assertEquals(absoluteUrl, webCrawlerConversionOutput);
    }

    @Test
    public void testNoConversionAbsoluteToRelativeUrl() {
        String absoluteUrl = "https://example.com/relativeUrl";

        String webCrawlerConversionOutput = webCrawler.convertRelativeUrlToAbsoluteURL(absoluteUrl);

        assertEquals(absoluteUrl, webCrawlerConversionOutput);
    }

    @Test
    public void testStringPrinting() throws IOException {
        String printMessage = "https://example.com";

        webCrawler.printString(printMessage);

        assertEquals(printMessage, outputStream.toString()); //TODO
        verify(mockFileWriter, times(1)).write(printMessage);
    }

    @Test
    public void testStringPrintingError() throws IOException {
        String printMessage = "https://example.com";

        doThrow(new IOException()).when(mockFileWriter).write(anyString());

        assertThrows(RuntimeException.class, () -> webCrawler.printString(printMessage));
    }

    // Todo rework + HigherDepth
    @Test
    public void testPrintCrawledHeadlinesZeroDepth() throws IOException {
        String expectedHeaderLevel = "# ";
//        String expectedDepth = "> ";
        String expectedHeadlineTranslation = "Überschrift H1\n"; // Todo muss gemocked werden
        String expectedPrintMessage = expectedHeaderLevel + expectedHeadlineTranslation + "\n";

        crawledHeadlines = addElements();
        webCrawler.setCrawledHeadlineElements(crawledHeadlines);
        webCrawler.setCurrentDepthOfRecursiveSearch(0);
        webCrawler.printCrawledHeadlines();

        assertEquals(expectedPrintMessage, outputStream.toString());

        verify(mockFileWriter, times(1)).write("#");
        verify(mockFileWriter, times(1)).write(expectedHeadlineTranslation);
    }

    @Test
    public void testPrintHeaderLevel() throws IOException {
        String expectedPrintMessage = "# ";
        Element crawledHeadlineElement = new Element("h1").text("Heading h1");

        webCrawler.printHeaderLevel(crawledHeadlineElement);

        assertEquals(expectedPrintMessage, outputStream.toString());

        verify(mockFileWriter, times(1)).write("#");
        verify(mockFileWriter, times(1)).write(" ");
    }

    @Test
    public void testPrintZeroDepth() throws IOException {
        String expectedOutputMessage = "> ";

        webCrawler.setCurrentDepthOfRecursiveSearch(0);
        webCrawler.printDepthIndicator();

        assertEquals(expectedOutputMessage, outputStream.toString());

        verify(mockFileWriter, times(1)).write("> ");
    }

    @Test
    public void testPrintHigherDepth() throws IOException {
        String expectedOutputMessage = "------> ";

        webCrawler.setCurrentDepthOfRecursiveSearch(3);
        webCrawler.printDepthIndicator();

        assertEquals(expectedOutputMessage, outputStream.toString());

        verify(mockFileWriter, times(3)).write("--");
        verify(mockFileWriter, times(1)).write("> ");
    }

    @Test
    public void testPrintWebcrawlerInput() throws IOException {
        String websiteUrlInput = "input: <a>https://example.com</a>\n";
        String depthInput = "<br>depth: 1\n";
        String sourceLanguageInput = "<br>source language: en\n";
        String targetLanguageInput = "<br>Target language: de\n";
        String summaryInput = "<br>summary:\n";
        String expectedOutputMessage = websiteUrlInput + depthInput + sourceLanguageInput + targetLanguageInput + summaryInput;

        webCrawler.setCurrentDepthOfRecursiveSearch(0);
        webCrawler.printInput();

        assertEquals(expectedOutputMessage, outputStream.toString());

        verify(mockFileWriter, times(1)).write(websiteUrlInput);
        verify(mockFileWriter, times(1)).write(depthInput);
        verify(mockFileWriter, times(1)).write(sourceLanguageInput);
        verify(mockFileWriter, times(1)).write(targetLanguageInput);
        verify(mockFileWriter, times(1)).write(summaryInput);
    }

    @Test
    public void testPrintCrawledWorkingLink() throws IOException {
        String lineBreakMessage = "<br>--";
        String depthIndicatorMessage = "> ";
        String firstLinkPart = "link to <a>";
        String crawledTestLink = "http://example.com";
        String secondLinkPart = "</a>\n\n";
        String expectedOutputMessage = lineBreakMessage + depthIndicatorMessage + firstLinkPart + crawledTestLink + secondLinkPart;
        boolean isBrokenLink = false;

        webCrawler.printCrawledLink(crawledTestLink, isBrokenLink);

        assertEquals(expectedOutputMessage, outputStream.toString());

        verify(mockFileWriter, times(1)).write(lineBreakMessage);
        verify(mockFileWriter, times(1)).write(depthIndicatorMessage);
        verify(mockFileWriter, times(1)).write(firstLinkPart);
        verify(mockFileWriter, times(1)).write(crawledTestLink);
        verify(mockFileWriter, times(1)).write(secondLinkPart);
    }

    @Test
    public void testPrintCrawledBrokenLink() throws IOException {
        String lineBreakMessage = "<br>--";
        String depthIndicatorMessage = "> ";
        String firstLinkPart = "broken link <a>";
        String crawledTestLink = "http://example.com";
        String secondLinkPart = "</a>\n\n";
        boolean isBrokenLink = true;
        String expectedOutputMessage = lineBreakMessage + depthIndicatorMessage + firstLinkPart + crawledTestLink + secondLinkPart;

        webCrawler.printCrawledLink(crawledTestLink, isBrokenLink);

        assertEquals(expectedOutputMessage, outputStream.toString());

        verify(mockFileWriter, times(1)).write(lineBreakMessage);
        verify(mockFileWriter, times(1)).write(depthIndicatorMessage);
        verify(mockFileWriter, times(1)).write(firstLinkPart);
        verify(mockFileWriter, times(1)).write(crawledTestLink);
        verify(mockFileWriter, times(1)).write(secondLinkPart);
    }

    @Test
    public void testSuccessfulFileWriterClosure() throws IOException {
        webCrawler.setCurrentDepthOfRecursiveSearch(0);
        webCrawler.closeWriter();

        verify(mockFileWriter, times(1)).close();
    }

    @Test
    public void testUnsuccessfulFileWriterClosure() throws IOException {
        webCrawler.setCurrentDepthOfRecursiveSearch(1);
        webCrawler.closeWriter();

        verify(mockFileWriter, times(0)).close();
    }

    @Test
    public void testFileWriterClosureError() throws IOException {
        doThrow(new IOException()).when(mockFileWriter).close();

        assertThrows(RuntimeException.class, () -> webCrawler.closeWriter());
    }

    private Elements addElements() {
        Elements headlineElements = new Elements();
        Element headline = new Element("h1").text("Heading h1");
        headlineElements.add(headline);
        return headlineElements;
    }
}
