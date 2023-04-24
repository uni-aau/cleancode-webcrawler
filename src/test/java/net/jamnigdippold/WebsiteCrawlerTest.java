package net.jamnigdippold;

import okhttp3.FormBody;
import okhttp3.Request;
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

import java.io.*;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WebsiteCrawlerTest {
    private static Document mockedDocument;
    private static WebsiteCrawler webCrawler;
    private final String testFilePath = "testFile.txt";
    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
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
        System.setOut(new PrintStream(outputStream));
    }

    private void mockEstablishConnection() {
        String htmlMock = "<html><expectedBody><h1>Heading h1</h1><a href=\"http://example.com\">Link</a> <a href=\"./relativeUrl\"></a></expectedBody></html>";
        MockitoAnnotations.openMocks(this);
        mockedDocument = Jsoup.parse(htmlMock);

        webCrawler = spy(new WebsiteCrawler("https://example.com", 1, "de", testFilePath));

        doAnswer(invocationOnMock -> {
            webCrawler.setWebsiteDocumentConnection(mockedDocument);
            return null;
        }).when(webCrawler).establishConnection();
        webCrawler.establishConnection();
    }

    @AfterEach
    public void tearDown() {
        webCrawler.setCurrentDepthOfRecursiveSearch(0);
        webCrawler.closeWriter();
        webCrawler = null;
        System.setOut(System.out);
        new File(testFilePath).delete();
    }

    @Test
    void testCreateFileWriterError() {
        assertThrows(RuntimeException.class, () -> webCrawler.createFileWriter(""));
    }

    @Test
    void testHeadlineTextOutput() {
        crawledHeadlines = addElements();

        webCrawler.crawlHeadlines();

        assertEquals(crawledHeadlines.text(), webCrawler.getCrawledHeadlineElements().text());
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
        FileWriter writer = mock(FileWriter.class);

        WebsiteCrawler newCrawler = new WebsiteCrawler(url, maxDepthOfRecursiveSearch, targetLanguage, currentDepthOfRecursiveSearch, writer);

        assertEquals(url, newCrawler.getWebsiteUrl());
        assertEquals(maxDepthOfRecursiveSearch, newCrawler.getMaxDepthOfRecursiveSearch());
        assertEquals(targetLanguage, newCrawler.getTargetLanguage());
        assertEquals(currentDepthOfRecursiveSearch, newCrawler.getCurrentDepthOfRecursiveSearch());
    }

    @Test
    void testStartCrawling() {
        webCrawler = mock(WebsiteCrawler.class);
        doCallRealMethod().when(webCrawler).startCrawling();

        webCrawler.startCrawling();

        verify(webCrawler).establishConnection();
        verify(webCrawler).crawlHeadlines();
        verify(webCrawler).setSourceLanguage();
        verify(webCrawler).printInput();
        verify(webCrawler).printCrawledHeadlines();
        verify(webCrawler).crawlWebsiteLinks();
        verify(webCrawler).recursivelyPrintCrawledWebsites();
        verify(webCrawler).closeWriter();
    }

    @Test
    void testRecursiveWebsiteCrawlingBrokenLink() throws IOException {
        String link = "https://looksRealButIsNot";
        crawledLinks.add(link);
        String expectedOutputMessage = "<br>--> broken link <a>" + link + "</a>\n\n";
        mockJsoup();

        webCrawler.setCrawledLinks(crawledLinks);
        webCrawler.recursivelyPrintCrawledWebsites();

        assertEquals(expectedOutputMessage, outputStream.toString());
        assertEqualFileContent(expectedOutputMessage, testFilePath);

        mockedJsoup.close();
    }

    @Test
    void testRecursiveWebsiteCrawlingTooHighDepth() throws IOException {
        String link = "https://example.com";
        crawledLinks.add(link);
        String expectedOutputMessage = "<br>------> link to <a>" + link + "</a>\n\n";
        mockJsoup();

        webCrawler.setCrawledLinks(crawledLinks);
        webCrawler.setCurrentDepthOfRecursiveSearch(2);
        webCrawler.recursivelyPrintCrawledWebsites();

        assertEquals(expectedOutputMessage, outputStream.toString());
        assertEqualFileContent(expectedOutputMessage, testFilePath);

        mockedJsoup.close();
    }

    @Test
    void testRecursiveWebsiteCrawlingAtHigherDepth() throws IOException {
        mockJsoup();
        mockCrawlerCreation();
        ArrayList<String> crawledLinks = new ArrayList<>();
        crawledLinks.add("https://example.com");
        webCrawler.setCrawledLinks(crawledLinks);
        webCrawler.setMaxDepthOfRecursiveSearch(2);

        webCrawler.recursivelyPrintCrawledWebsites();

        assertEquals("<br>--> link to <a>https://example.com</a>\n" +
                "\n" +
                "<br>----> link to <a>https://example.com</a>\n" +
                "\n" +
                "<br>------> link to <a>https://example.com</a>\n" +
                "\n", outputStream.toString());

        mockedJsoup.close();
        mockedCrawlerConstruction.close();
    }

    void mockCrawlerCreation() {
        mockedCrawlerConstruction = mockConstruction(WebsiteCrawler.class,
                (mock, context) -> {
                    setMockedMethodeToCallRealMethods(mock);

                    doAnswer(invocationOnMock -> {
                        System.out.print((String) invocationOnMock.getArgument(0));
                        return null;
                    }).when(mock).printString(anyString());

                    doAnswer(invocationOnMock -> {
                        ArrayList<String> crawledLinks = new ArrayList<>();
                        crawledLinks.add("https://example.com");
                        mock.setCrawledLinks(crawledLinks);
                        mock.setMaxDepthOfRecursiveSearch((int) context.arguments().get(1));
                        mock.setCurrentDepthOfRecursiveSearch((int) context.arguments().get(3));
                        mock.recursivelyPrintCrawledWebsites();
                        return null;
                    }).when(mock).startCrawling();
                });
    }

    void setMockedMethodeToCallRealMethods(WebsiteCrawler mock) {
        doCallRealMethod().when(mock).setCrawledLinks(any());
        doCallRealMethod().when(mock).setCurrentDepthOfRecursiveSearch(anyInt());
        doCallRealMethod().when(mock).setMaxDepthOfRecursiveSearch(anyInt());
        doCallRealMethod().when(mock).convertRelativeUrlToAbsoluteURL(anyString());
        doCallRealMethod().when(mock).printCrawledLink(anyString(), anyBoolean());
        doCallRealMethod().when(mock).printDepthIndicator();
        doCallRealMethod().when(mock).recursivelyPrintCrawledWebsites();
    }

    @Test
    void testConversionAbsoluteToRelativeUrl() {
        String relativeUrl = "./relativeUrl";
        String absoluteUrl = "https://example.com/relativeUrl";

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
    void testStringPrinting() throws IOException {
        String printMessage = "https://example.com";

        webCrawler.printString(printMessage);

        assertEquals(printMessage, outputStream.toString());
        assertEqualFileContent(printMessage, testFilePath);
    }


    @Test
    void testStringPrintingError() {
        String printMessage = "https://example.com";

        webCrawler.closeWriter();

        assertThrows(RuntimeException.class, () -> webCrawler.printString(printMessage));
    }

    @Test
    void testPrintCrawledHeadlinesZeroDepth() throws IOException {
        String expectedPrintMessage = "# Überschrift h1\n\n";
        mockHeadingTranslation();

        crawledHeadlines = addElements();
        webCrawler.setCrawledHeadlineElements(crawledHeadlines);
        webCrawler.printCrawledHeadlines();

        assertEquals(expectedPrintMessage, outputStream.toString());
        assertEqualFileContent(expectedPrintMessage, testFilePath);
    }

    @Test
    void testPrintCrawledHeadlinesOneDepth() throws IOException {
        String expectedPrintMessage = "# --> Überschrift h1\n\n";
        mockHeadingTranslation();

        crawledHeadlines = addElements();
        webCrawler.setCrawledHeadlineElements(crawledHeadlines);
        webCrawler.setCurrentDepthOfRecursiveSearch(1);
        webCrawler.printCrawledHeadlines();

        assertEquals(expectedPrintMessage, outputStream.toString());
        assertEqualFileContent(expectedPrintMessage, testFilePath);
    }

    void mockHeadingTranslation() {
        webCrawler.setTranslator(translator);
        doReturn("Überschrift h1").when(translator).getTranslatedHeadline("Heading h1");
    }

    @Test
    void testIsBrokenLinkMalformedURL() throws IOException {
        mockJsoup();

        boolean isBrokenLink = WebsiteCrawler.isBrokenLink("Not a real URL");

        assertTrue(isBrokenLink);
        mockedJsoup.close();
    }

    @Test
    void testIsBrokenLinkUnreachableURL() throws IOException {
        mockJsoup();

        boolean isBrokenLink = WebsiteCrawler.isBrokenLink("https://looksRealButIsNot");

        assertTrue(isBrokenLink);
        mockedJsoup.close();
    }

    @Test
    void testIsBrokenLinkSuccess() throws IOException {
        mockJsoup();

        boolean isBrokenLink = WebsiteCrawler.isBrokenLink("https://example.com");

        assertFalse(isBrokenLink);
        mockedJsoup.close();
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

/*    @Test
    void testGetSystemEnv() {
        String expectedKey = System.getenv("RAPIDAPI_API_KEY");
        String actualKey = webCrawler.getApiKey();

        assertEquals(expectedKey, actualKey);
    }*/

    @Test
    void testPrintHeaderLevel() throws IOException {
        String expectedPrintMessage = "# ";
        Element crawledHeadlineElement = new Element("h1").text("Heading h1");

        webCrawler.printHeaderLevel(crawledHeadlineElement);

        assertEquals(expectedPrintMessage, outputStream.toString());
        assertEqualFileContent("# ", testFilePath);
    }

    @Test
    void testPrintZeroDepth() throws IOException {
        String expectedOutputMessage = "> ";

        webCrawler.setCurrentDepthOfRecursiveSearch(0);
        webCrawler.printDepthIndicator();

        assertEquals(expectedOutputMessage, outputStream.toString());
        assertEqualFileContent(expectedOutputMessage, testFilePath);
    }

    @Test
    void testPrintHigherDepth() throws IOException {
        String expectedOutputMessage = "------> ";

        webCrawler.setCurrentDepthOfRecursiveSearch(3);
        webCrawler.printDepthIndicator();

        assertEquals(expectedOutputMessage, outputStream.toString());
        assertEqualFileContent(expectedOutputMessage, testFilePath);
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
        webCrawler.printInput();

        assertEquals(expectedOutputMessage, outputStream.toString());
        assertEqualFileContent(expectedOutputMessage, testFilePath);
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

        webCrawler.printCrawledLink(crawledTestLink, isBrokenLink);

        assertEquals(expectedOutputMessage, outputStream.toString());
        assertEqualFileContent(expectedOutputMessage, testFilePath);
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

        webCrawler.printCrawledLink(crawledTestLink, isBrokenLink);

        assertEquals(expectedOutputMessage, outputStream.toString());
        assertEqualFileContent(expectedOutputMessage, testFilePath);
    }

    @Test
    void testSuccessfulFileWriterClosure() {
        webCrawler.setCurrentDepthOfRecursiveSearch(0);
        webCrawler.closeWriter();

        assertThrows(RuntimeException.class, () -> webCrawler.printString("test"));
    }

    @Test
    void testUnsuccessfulFileWriterClosure() {
        webCrawler.setCurrentDepthOfRecursiveSearch(1);
        webCrawler.closeWriter();

        assertDoesNotThrow(() -> webCrawler.printString("test"));
    }

    @Test
    void testFileWriterClosureError() throws IOException {
        FileWriter actualFileWriter = webCrawler.getFileWriter();
        FileWriter mockFileWriter = mock(FileWriter.class);
        webCrawler.setCurrentDepthOfRecursiveSearch(0);

        webCrawler.setFileWriter(mockFileWriter);
        doThrow(new IOException()).when(mockFileWriter).close();

        assertThrows(RuntimeException.class, () -> webCrawler.closeWriter());
        webCrawler.setFileWriter(actualFileWriter);
    }

    private Elements addElements() {
        Elements headlineElements = new Elements();
        Element headline = new Element("h1").text("Heading h1");
        headlineElements.add(headline);

        return headlineElements;
    }

    private void assertEqualFileContent(String expectedValue, String path) throws IOException {
        webCrawler.flushWriter();
        String content = new String(Files.readAllBytes(Paths.get(path)));

        assertEquals(expectedValue, content);
    }
}
