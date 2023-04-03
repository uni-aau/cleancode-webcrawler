package net.jamnigdippold;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WebsiteCrawlerTest {
    @Mock
    private static Connection mockedConnection;
    @Mock
    private static Document mockedDocument;
    private static WebsiteCrawler webCrawler;
    static String htmlMock = "<html><body><h1>Heading h1</h1></body></html>";

    @BeforeEach
    void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);
        mockedDocument = Jsoup.parse(htmlMock);
        when(mockedConnection.get()).thenReturn(mockedDocument);
        when(Jsoup.connect(anyString())).thenReturn(mockedConnection);

        webCrawler = new WebsiteCrawler("https://example.com", 1, "de"); // Todo muss noch auf die jeweiligen Sachen angepasst werden
        webCrawler.establishConnection();
//        webCrawler.setWebsiteDocumentConnection(Jsoup.parse(htmlMock));
    }


    @AfterAll
    static void tearDown() {
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
