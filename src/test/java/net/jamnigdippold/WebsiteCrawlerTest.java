package net.jamnigdippold;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Rule;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
//import org.powermock.reflect.Whitebox;

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

    @BeforeEach
    public void setUp() {
        setUpJsoupMock();
        webCrawler.establishConnection();
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
        Elements elements = new Elements();
        Element elem1 = new Element("h1").text("Heading h1");
        elements.add(elem1);

        webCrawler.crawlHeadlines();

        assertEquals(elements.text(), webCrawler.getCrawledHeadlineElements().text());
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
        webCrawler.setFileWriter(mockFileWriter);

        webCrawler.printString(printMessage);

        assertEquals(printMessage, outputStream.toString()); //TODO
        verify(mockFileWriter, times(1)).write(printMessage);
    }

    @Test
    public void testStringPrintingError() throws IOException {
        String printMessage = "https://example.com";

        doThrow(new IOException()).when(mockFileWriter).write(anyString());
        webCrawler.setFileWriter(mockFileWriter);

        assertThrows(RuntimeException.class, () -> webCrawler.printString(printMessage));
    }
}
