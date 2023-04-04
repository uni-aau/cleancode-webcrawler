package net.jamnigdippold;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class WebsiteCrawlerTest {

    private static Document mockedDocument;
    private static WebsiteCrawler webCrawler;
    static String htmlMock = "<html><body><h1>Heading h1</h1></body></html>";

    @BeforeEach
    public void setUp(){
        setUpJsoupMock();
        webCrawler.establishConnection();
    }

    private void setUpJsoupMock() {
        MockitoAnnotations.openMocks(this);
        mockedDocument = Jsoup.parse(htmlMock);
        webCrawler = spy(new WebsiteCrawler("https://example.com", 1, "de"));// Todo muss noch auf die jeweiligen Sachen angepasst werden

        doAnswer(invocationOnMock -> {
            webCrawler.setWebsiteDocumentConnection(mockedDocument);
            return null;
        }).when(webCrawler).establishConnection();
    }

    @AfterAll
    public static void tearDown() {
        webCrawler = null;
    }

    @Test
    public void testHeadlineTextOutput() {
        Elements elements = new Elements();
        Element elem1 = new Element("h1").text("Heading h1");
        elements.add(elem1);

        webCrawler.crawlHeadlines();

        assertEquals(elements.text(), webCrawler.getCrawledHeadlineElements().text());
    }


}
