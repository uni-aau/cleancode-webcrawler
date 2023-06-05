package net.jamnigdippold;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WebsiteCrawlerTest {
    private static Document mockedDocument;
    private static WebsiteCrawler webCrawler;
    private final ArrayList<String> crawledLinks = new ArrayList<>();
    private Elements crawledHeadlines;
    @Mock
    TextTranslator translator;
    MockedStatic<Jsoup> mockedJsoup;
    MockedConstruction<WebsiteCrawler> mockedCrawlerConstruction;

    @BeforeEach
    public void setUp() {
        mockEstablishConnection();
        webCrawler.establishConnection();
    }

    private void mockEstablishConnection() {
        String htmlMock = "<html><expectedBody><h1>Heading h1</h1><a href=\"http://example.com\">Link</a> <a href=\"./relativeUrl\"></a></expectedBody></html>";
        MockitoAnnotations.openMocks(this);
        mockedDocument = Jsoup.parse(htmlMock);

        webCrawler = spy(new WebsiteCrawler("https://example.com", 1, "de"));

        doAnswer(invocationOnMock -> {
            webCrawler.setWebsiteDocumentConnection(mockedDocument);
            return null;
        }).when(webCrawler).establishConnection();
    }

    @AfterEach
    public void tearDown() {
        webCrawler = null;
        if (mockedJsoup != null)
            mockedJsoup.close();
        if (mockedCrawlerConstruction != null)
            mockedCrawlerConstruction.close();
    }

    @Test
    void testEstablishConnection() throws IOException {
        mockJsoup();
        doCallRealMethod().when(webCrawler).establishConnection();

        webCrawler.establishConnection();

        mockedJsoup.verify(() -> Jsoup.connect(any()));


    }

    @Test
    void testEstablishConnectionError() throws IOException {
        mockJsoup();
        doCallRealMethod().when(webCrawler).establishConnection();

        webCrawler.setWebsiteUrl("Not a real URL");

        assertThrows(RuntimeException.class, () -> webCrawler.establishConnection());


    }

    @Test
    void testHeadlineTextOutput() {
        crawledHeadlines = addElements();

        webCrawler.crawlHeadlines();

        assertEquals(crawledHeadlines.text(), webCrawler.getCrawledHeadlines().text());
    }

    @Test
    void testWebsiteLinksCrawlingOutput() {
        crawledLinks.add("http://example.com");
        crawledLinks.add("./relativeUrl");

        webCrawler.crawlWebsiteLinks();

        assertEquals(crawledLinks, webCrawler.getCrawledLinks());
    }

    @Test
    void testRecursiveConstructor() {
        String url = "http://example.com";
        int maxDepthOfRecursiveSearch = 3;
        String targetLanguage = "en";
        int currentDepthOfRecursiveSearch = 1;

        WebsiteCrawler newCrawler = new WebsiteCrawler(url, maxDepthOfRecursiveSearch, targetLanguage, currentDepthOfRecursiveSearch);

        assertEquals(url, newCrawler.getWebsiteUrl());
        assertEquals(maxDepthOfRecursiveSearch, newCrawler.getMaxDepthOfRecursiveSearch());
        assertEquals(targetLanguage, newCrawler.getTargetLanguage());
        assertEquals(currentDepthOfRecursiveSearch, newCrawler.getCurrentDepthOfRecursiveSearch());
    }

    @Test
    void testMethodCallSequenceOnCrawlingStart() {
        webCrawler = mock(WebsiteCrawler.class);
        doCallRealMethod().when(webCrawler).startCrawling();

        webCrawler.startCrawling();

        verify(webCrawler).establishConnection();
        verify(webCrawler).crawlHeadlines();
        verify(webCrawler).initializeTranslator();
        // verify(webCrawler).setSourceLanguage();
        verify(webCrawler).outputInput();
        verify(webCrawler).outputCrawledHeadlines();
        verify(webCrawler).crawlWebsiteLinks();
        verify(webCrawler).recursivelyCrawlLinkedWebsites();
    }

    @Test
    void testRecursiveWebsiteCrawlingBrokenLink() throws IOException {
        String link = "https://looksRealButIsNot";
        crawledLinks.add(link);
        String expectedOutputMessage = "<br>--> broken link <a>" + link + "</a>\n\n";
        mockJsoup();

        webCrawler.setCrawledLinks(crawledLinks);
        webCrawler.setUpOutput();

        webCrawler.recursivelyCrawlLinkedWebsites();

        assertEquals(expectedOutputMessage, webCrawler.getOutput());


    }

    @Test
    void testRecursiveWebsiteCrawlingTooHighDepth() throws IOException {
        String link = "https://example.com";
        crawledLinks.add(link);
        String expectedOutputMessage = "<br>------> link to <a>" + link + "</a>\n\n";
        mockJsoup();

        webCrawler.setCrawledLinks(crawledLinks);
        webCrawler.setCurrentDepthOfRecursiveSearch(2);
        webCrawler.recursivelyCrawlLinkedWebsites();

        assertEquals(expectedOutputMessage, webCrawler.getOutput());
    }

    @Test
    void testRecursiveWebsiteCrawlingAtHigherDepth() throws IOException {
        mockJsoup();
        mockCrawlerCreation();
        ArrayList<String> crawledLinks = new ArrayList<>();
        crawledLinks.add("https://example.com");

        webCrawler.setCrawledLinks(crawledLinks);
        webCrawler.setMaxDepthOfRecursiveSearch(2);
        webCrawler.recursivelyCrawlLinkedWebsites();

        assertEquals("<br>--> link to <a>https://example.com</a>\n" +
                "\n" +
                "<br>----> link to <a>https://example.com</a>\n" +
                "\n" +
                "<br>------> link to <a>https://example.com</a>\n" +
                "\n", webCrawler.getOutput());
    }

    void mockCrawlerCreation() {
        mockedCrawlerConstruction = mockConstruction(WebsiteCrawler.class,
                (mock, context) -> {
                    setMockedMethodeToCallRealMethods(mock);
                    mock.initializeValues((String) context.arguments().get(0), (int) context.arguments().get(1), (String) context.arguments().get(2), (int) context.arguments().get(3));
                    doAnswer(invocationOnMock -> {
                        ArrayList<String> crawledLinks = new ArrayList<>();
                        crawledLinks.add("https://example.com");
                        mock.setCrawledLinks(crawledLinks);
                        mock.outputInput();

                        mock.recursivelyCrawlLinkedWebsites();
                        return null;
                    }).when(mock).startCrawling();
                    doAnswer(invocationOnMock -> {
                        mock.run();
                        return null;
                    }).when(mock).start();
                });
    }

    void setMockedMethodeToCallRealMethods(WebsiteCrawler mock) {
        doCallRealMethod().when(mock).initializeValues(anyString(), anyInt(), anyString(), anyInt());
        doCallRealMethod().when(mock).setCrawledLinks(any());
        doCallRealMethod().when(mock).setCurrentDepthOfRecursiveSearch(anyInt());
        doCallRealMethod().when(mock).setMaxDepthOfRecursiveSearch(anyInt());
        doCallRealMethod().when(mock).convertRelativeUrlToAbsoluteURL(anyString());
        doCallRealMethod().when(mock).outputCrawledLink(anyString(), anyBoolean());
        doCallRealMethod().when(mock).outputDepthIndicator(anyInt());
        doCallRealMethod().when(mock).recursivelyCrawlLinkedWebsites();
        doCallRealMethod().when(mock).waitForCrawlerThreads();
        doCallRealMethod().when(mock).appendOutputFromRecursiveCrawlers();
        doCallRealMethod().when(mock).getOutput();
        doCallRealMethod().when(mock).startNewCrawler(any());
        doCallRealMethod().when(mock).outputInput();
        doCallRealMethod().when(mock).run();
    }

    @Test
    void testWaitForCrawlerThreadsException() throws InterruptedException {
        WebsiteCrawler mockedCrawler = mock(WebsiteCrawler.class);
        doThrow(InterruptedException.class).when(mockedCrawler).join();
        webCrawler.setRecursiveCrawlers(new ArrayList<>(List.of(mockedCrawler)));

        assertThrows(RuntimeException.class, webCrawler::waitForCrawlerThreads);
    }

    @Test
    void testConversionAbsoluteToRelativeUrl() {
        String relativeUrl = "./relativeUrl";
        String absoluteUrl = "https://example.com/relativeUrl";
        webCrawler.setWebsiteUrl("https://example.com");

        String webCrawlerConversionOutput = webCrawler.convertRelativeUrlToAbsoluteURL(relativeUrl);

        assertEquals(absoluteUrl, webCrawlerConversionOutput);
    }

    @Test
    void testNoConversionAbsoluteToRelativeUrl() {
        String absoluteUrl = "https://example.com/relativeUrl";

        String webCrawlerConversionOutput = webCrawler.convertRelativeUrlToAbsoluteURL(absoluteUrl);

        assertEquals(absoluteUrl, webCrawlerConversionOutput);
    }

    @Test
    void testTranslatorInitialization() {
        webCrawler.setTargetLanguage("de");

        webCrawler.initializeTranslator();

        assertEquals("de", ((TextTranslator) webCrawler.getTranslator()).getTargetLanguage()); // Todo casting?
    }


    @Test
    void testPrintCrawledHeadlinesZeroDepth() throws IOException {
        String expectedPrintMessage = "# Überschrift h1\n\n";
        mockHeadingTranslation();

        crawledHeadlines = addElements();
        webCrawler.setCrawledHeadlines(crawledHeadlines);
        webCrawler.setUpOutput();
        webCrawler.outputCrawledHeadlines();

        assertEquals(expectedPrintMessage, webCrawler.getOutput());
    }

    @Test
    void testPrintCrawledHeadlinesOneDepth() throws IOException {
        String expectedPrintMessage = "# --> Überschrift h1\n\n";
        mockHeadingTranslation();

        crawledHeadlines = addElements();
        webCrawler.setCrawledHeadlines(crawledHeadlines);
        webCrawler.setCurrentDepthOfRecursiveSearch(1);
        webCrawler.setUpOutput();
        webCrawler.outputCrawledHeadlines();

        assertEquals(expectedPrintMessage, webCrawler.getOutput());
    }

    void mockHeadingTranslation() {
        webCrawler.setTranslator(translator);
        doReturn("Überschrift h1").when(translator).translate("Heading h1");
    }

    void mockGetSourceLanguage() {
        webCrawler.setTranslator(translator);
        doReturn("de").when(translator).getSourceLanguage();
    }

    // @Test TODO: figure out SourceLanguage
    void testSetSourceLanguage() {
        mockGetSourceLanguage();

//        webCrawler.setSourceLanguage();

        assertEquals("de", webCrawler.getSourceLanguage());
//        verify(translator).setTranslationSourceLanguage(any());
        verify(translator).getSourceLanguage();
    }

    @Test
    void testIsBrokenLinkMalformedURL() throws IOException {
        mockJsoup();

        boolean isBrokenLink = WebsiteCrawler.isBrokenLink("Not a real URL");

        assertTrue(isBrokenLink);

    }

    @Test
    void testIsBrokenLinkUnreachableURL() throws IOException {
        mockJsoup();

        boolean isBrokenLink = WebsiteCrawler.isBrokenLink("https://looksRealButIsNot");

        assertTrue(isBrokenLink);

    }

    @Test
    void testIsBrokenLinkSuccess() throws IOException {
        mockJsoup();

        boolean isBrokenLink = WebsiteCrawler.isBrokenLink("https://example.com");

        assertFalse(isBrokenLink);

    }

    void mockJsoup() throws IOException {
        Connection mockedConnection = mock(Connection.class);
        when(mockedConnection.get()).thenAnswer(invocationOnMock -> {
            if (mockedConnection.toString().equals("Not a real URL")) {
                throw new MalformedURLException();
            }
            if (mockedConnection.toString().equals("https://looksRealButIsNot")) {
                throw new IOException();
            }
            return null;
        });

        mockedJsoup = mockStatic(Jsoup.class);
        mockedJsoup.when(() -> Jsoup.connect(any())).thenAnswer(invocationOnMock -> {
            when(mockedConnection.toString()).thenReturn(invocationOnMock.getArgument(0));
            return mockedConnection;
        });
    }


    @Test
    void testPrintHeaderLevel() throws IOException {
        String expectedPrintMessage = "# ";
        Element crawledHeadline = new Element("h1").text("Heading h1");

        webCrawler.outputHeaderLevel(crawledHeadline);

        assertEquals(expectedPrintMessage, webCrawler.getOutput());
    }

    @Test
    void testPrintZeroDepth() throws IOException {
        String expectedOutputMessage = "> ";
        webCrawler.setUpOutput();

        webCrawler.outputDepthIndicator(0);

        assertEquals(expectedOutputMessage, webCrawler.getOutput());
    }

    @Test
    void testPrintHigherDepth() throws IOException {
        String expectedOutputMessage = "------> ";

        webCrawler.outputDepthIndicator(3);

        assertEquals(expectedOutputMessage, webCrawler.getOutput());
    }

    @Test
    void testPrintWebcrawlerInput() throws IOException {
        String websiteUrlInput = "input: <a>https://example.com</a>\n";
        String depthInput = "<br>depth: 1\n";
        String sourceLanguageInput = "<br>source language: auto\n";
        String targetLanguageInput = "<br>Target language: de\n";
        String summaryInput = "<br>summary:\n";
        String expectedOutputMessage = websiteUrlInput + depthInput + sourceLanguageInput + targetLanguageInput + summaryInput;

        webCrawler.setCurrentDepthOfRecursiveSearch(0);

        webCrawler.outputInput();

        assertEquals(expectedOutputMessage, webCrawler.getOutput());
    }

    @Test
    void testPrintCrawledWorkingLink() throws IOException {
        String lineBreakMessage = "<br>--";
        String depthIndicatorMessage = "> ";
        String firstLinkPart = "link to <a>";
        String crawledTestLink = "http://example.com";
        String secondLinkPart = "</a>\n\n";
        String expectedOutputMessage = lineBreakMessage + depthIndicatorMessage + firstLinkPart + crawledTestLink + secondLinkPart;
        boolean isBrokenLink = false;

        webCrawler.outputCrawledLink(crawledTestLink, isBrokenLink);

        assertEquals(expectedOutputMessage, webCrawler.getOutput());
    }

    @Test
    void testPrintCrawledBrokenLink() throws IOException {
        String lineBreakMessage = "<br>--";
        String depthIndicatorMessage = "> ";
        String firstLinkPart = "broken link <a>";
        String crawledTestLink = "http://example.com";
        String secondLinkPart = "</a>\n\n";
        boolean isBrokenLink = true;
        String expectedOutputMessage = lineBreakMessage + depthIndicatorMessage + firstLinkPart + crawledTestLink + secondLinkPart;
        webCrawler.setUpOutput();

        webCrawler.outputCrawledLink(crawledTestLink, isBrokenLink);

        assertEquals(expectedOutputMessage, webCrawler.getOutput());
    }

    private Elements addElements() {
        Elements headlineElements = new Elements();
        Element headline = new Element("h1").text("Heading h1");
        headlineElements.add(headline);

        return headlineElements;
    }

}
