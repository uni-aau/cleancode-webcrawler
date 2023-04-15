package net.jamnigdippold;

import okhttp3.*;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.io.*;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.whenNew;

class WebsiteCrawlerTest {

    private static Document mockedDocument;
    @Mock
    Request mockRequest;
    @Mock
    OkHttpClient mockClient;
    @Mock
    Response mockResponse;
    @Mock
    ResponseBody mockResponseBody;
    @Mock
    Call mockCall;
    MockedStatic<Jsoup> mockedJsoup;
    private FormBody expectedBody;
    private Request expectedRequest;
    private static WebsiteCrawler webCrawler;
    private final String testFilePath = "testFile.txt";
    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private Elements crawledHeadlines;

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
        assertThrows(RuntimeException.class, ()-> webCrawler.createFileWriter(""));
    }

    @Test
    void testHeadlineTextOutput() {
        crawledHeadlines = addElements();

        webCrawler.crawlHeadlines();

        assertEquals(crawledHeadlines.text(), webCrawler.getCrawledHeadlineElements().text());
    }

    @Test
    void testWebsiteLinksCrawlingOutput() {
        List<String> crawledLinks = new ArrayList<>();
        crawledLinks.add("http://example.com");
        crawledLinks.add("./relativeUrl");

        webCrawler.crawlWebsiteLinks();

        assertEquals(crawledLinks, webCrawler.getCrawledLinks());
    }

    @Test
    void testRecursiveWebsiteCrawlingBrokenLink() throws IOException {
        String link = "https://looksRealButIsNot";
        String expectedOutputMessage = "<br>--> broken link <a>" + link + "</a>\n\n";
        ArrayList<String> crawledLinks = new ArrayList<>();
        crawledLinks.add(link);
        mockJsoup();

        webCrawler.setCrawledLinks(crawledLinks);
        webCrawler.recursivelyCrawlLinkedWebsites();

        assertEquals(expectedOutputMessage, outputStream.toString());
        assertEqualFileContent(expectedOutputMessage, testFilePath);

        mockedJsoup.close();
    }

    @Test
    void testRecursiveWebsiteCrawlingTooHighDepth() throws IOException {
        String link = "https://example.com";
        String expectedOutputMessage = "<br>------> link to <a>" + link + "</a>\n\n";
        ArrayList<String> crawledLinks = new ArrayList<>();
        crawledLinks.add(link);
        mockJsoup();

        webCrawler.setCrawledLinks(crawledLinks);
        webCrawler.setCurrentDepthOfRecursiveSearch(2);
        webCrawler.recursivelyCrawlLinkedWebsites();

        assertEquals(expectedOutputMessage, outputStream.toString());
        assertEqualFileContent(expectedOutputMessage, testFilePath);
        mockedJsoup.close();
    }

    @Test
    void testRecursiveWebsiteCrawling() throws IOException {
        ArrayList<String> crawledLinks = new ArrayList<>();
        crawledLinks.add("https://example.com");
        mockJsoup();

        webCrawler.setCrawledLinks(crawledLinks);
        webCrawler.setCurrentDepthOfRecursiveSearch(0);
        webCrawler.setWebsiteDocumentConnection(mockedDocument);

        // TODO

        mockedJsoup.close();
    }

    @Test
    void testSetSourceLanguage() throws IOException {
        String expectedSourceLanguage = "en";
        String expectedResponseOutput = "{\n\"status\": \"success\",\n\"data\": {\n\"translatedText\": \"Ueberschrift h1\",\n\"detectedSourceLanguage\": {\n\"code\": \"en\",\n\"name\": \"English\"\n}\n}\n}";
        crawledHeadlines = addElements();
        mockResponseExtraction(expectedResponseOutput);
        doReturn(mockResponse).when(webCrawler).executeAPIRequest("Heading h1");

        webCrawler.setCrawledHeadlineElements(crawledHeadlines);
        webCrawler.setSourceLanguage();

        assertEquals(expectedSourceLanguage, webCrawler.getSourceLanguage());
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
        String expectedHeaderLevel = "# ";
        String expectedHeadlineTranslation = "Überschrift h1";
        String expectedPrintMessage = expectedHeaderLevel + expectedHeadlineTranslation + "\n\n";
        mockHeadingTranslation();

        crawledHeadlines = addElements();
        webCrawler.setCrawledHeadlineElements(crawledHeadlines);
        webCrawler.printCrawledHeadlines();

        assertEquals(expectedPrintMessage, outputStream.toString());
        assertEqualFileContent(expectedPrintMessage, testFilePath);
    }

    @Test
    void testPrintCrawledHeadlinesOneDepth() throws IOException {
        String expectedHeaderLevel = "# ";
        String expectedDepthLevel = "--> ";
        String expectedHeadlineTranslation = "Überschrift h1";
        String expectedPrintMessage = expectedHeaderLevel + expectedDepthLevel + expectedHeadlineTranslation + "\n\n";
        mockHeadingTranslation();

        crawledHeadlines = addElements();
        webCrawler.setCrawledHeadlineElements(crawledHeadlines);
        webCrawler.setCurrentDepthOfRecursiveSearch(1);
        webCrawler.printCrawledHeadlines();

        assertEquals(expectedPrintMessage, outputStream.toString());
        assertEqualFileContent(expectedPrintMessage, testFilePath);
    }

    void mockHeadingTranslation() {
        doReturn("Überschrift h1").when(webCrawler).getTranslatedHeadline("Heading h1");
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

    @Test
    void testGetSystemEnv() {
        String expectedKey = System.getenv("RAPIDAPI_API_KEY");
        String returnedKey = webCrawler.getApiKey();

        assertEquals(expectedKey, returnedKey);
    }

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

    @Test
    void testRequestBodyCreation() throws IOException {
        String sourceLanguage = "auto";
        String targetLanguage = "de";
        String headerText = "Headline 1";
        createBody(sourceLanguage, targetLanguage, headerText);

        RequestBody actualBody = webCrawler.createNewRequestBody(headerText);

        assertEquals(expectedBody.contentType(), actualBody.contentType());
        assertEquals(expectedBody.contentLength(), actualBody.contentLength());
    }

    // Todo verbessern?
    @Test
    void testTranslationRequestExecution() throws IOException {
        mockNewClientCall();

        assertNotNull(webCrawler.executeTranslationApiRequest(mockRequest));
    }

    @Test
    void testTranslationRequestExecutionError() throws IOException {
        mockNewClientCall();
        doThrow(new IOException()).when(mockCall).execute();

        assertThrows(RuntimeException.class, () -> webCrawler.executeTranslationApiRequest(mockRequest));
    }

    @Test
    void testTranslationExtraction() throws IOException {
        String expectedReturnValue = "Ueberschrift h1";
        String expectedResponseOutput = "{\n\"status\": \"success\",\n\"data\": {\n\"translatedText\": \"Ueberschrift h1\"\n}\n}";
        mockResponseExtraction(expectedResponseOutput);

        String actualReturnValue = webCrawler.extractTranslatedText(mockResponse);

        assertEquals(expectedReturnValue, actualReturnValue);
    }

    @Test
    void testTranslatedTextExtractionError() throws IOException {
        String expectedResponseOutput = "{\n\"status\": \"success\",\n\"data\": {\n\"translatedText\": \"Ueberschrift h1\"\n}\n}";
        mockResponseExtraction(expectedResponseOutput);
        doThrow(new IOException()).when(mockResponseBody).string();

        assertThrows(RuntimeException.class, () -> webCrawler.extractTranslatedText(mockResponse));
    }

    @Test
    void testTranslatedTextExtractionFallback() throws IOException {
        String expectedResponseOutput = "{\n\"status\": \"error\",\n\"message\": \"source language cannot be the same as target language\"\n}";
        mockResponseExtraction(expectedResponseOutput);

        String actualReturnValue = webCrawler.extractTranslatedText(mockResponse);

        assertNull(actualReturnValue);
    }

    @Test
    void testLanguageCodeExtraction() throws IOException {
        String expectedLanguageCode = "en";
        String expectedResponseOutput = "{\n\"status\": \"success\",\n\"data\": {\n\"translatedText\": \"Ueberschrift h1\",\n\"detectedSourceLanguage\": {\n\"code\": \"en\",\n\"name\": \"English\"\n}\n}\n}";
        mockResponseExtraction(expectedResponseOutput);

        String actualLanguageCode = webCrawler.extractLanguageCode(mockResponse);

        assertEquals(expectedLanguageCode, actualLanguageCode);
    }

    @Test
    void testLanguageCodeExtractionError() throws IOException {
        String expectedResponseOutput = "{\n\"status\": \"success\",\n\"data\": {\n\"translatedText\": \"Ueberschrift h1\",\n\"detectedSourceLanguage\": {\n\"code\": \"en\",\n\"name\": \"English\"\n}\n}\n}";
        mockResponseExtraction(expectedResponseOutput);
        doThrow(new IOException()).when(mockResponseBody).string();

        assertThrows(RuntimeException.class, () -> webCrawler.extractLanguageCode(mockResponse));
    }

    @Test
    void testGetTranslatedHeadline() throws IOException {
        String expectedTranslatedHeadline = "Ueberschrift h1";
        String expectedResponseOutput = "{\n\"status\": \"success\",\n\"data\": {\n\"translatedText\": \"Ueberschrift h1\"\n}\n}";
        mockResponseExtraction(expectedResponseOutput);
        doReturn(mockResponse).when(webCrawler).executeAPIRequest("Heading h1");
        String actualTranslatedHeadline = webCrawler.getTranslatedHeadline("Heading h1");

        assertEquals(expectedTranslatedHeadline, actualTranslatedHeadline);
    }

    @Test
    void testGetTranslatedHeadlineFallback() throws IOException {
        String expectedTranslatedHeadline = "Heading h1";
        String expectedResponseOutput = "{\n\"status\": \"error\",\n\"message\": \"source language cannot be the same as target language\"\n}";
        mockResponseExtraction(expectedResponseOutput);
        doReturn(mockResponse).when(webCrawler).executeAPIRequest("Heading h1");
        String actualTranslatedHeadline = webCrawler.getTranslatedHeadline("Heading h1");

        assertEquals(expectedTranslatedHeadline, actualTranslatedHeadline);
    }

    @Test
    void testGetLanguageCodeFromHeadline() throws IOException {
        String expectedLanguageCode = "en";
        String expectedResponseOutput = "{\n\"status\": \"success\",\n\"data\": {\n\"translatedText\": \"Ueberschrift h1\",\n\"detectedSourceLanguage\": {\n\"code\": \"en\",\n\"name\": \"English\"\n}\n}\n}";
        mockResponseExtraction(expectedResponseOutput);
        doReturn(mockResponse).when(webCrawler).executeAPIRequest("Heading h1");
        String actualLanguageCode = webCrawler.getLanguageCodeFromHeadline("Heading h1");

        assertEquals(expectedLanguageCode, actualLanguageCode);
    }

    @Test
    void testExecuteAPIRequest() throws IOException {
        mockNewClientCall();
        mockGetAPIKey();
        String expectedResponseOutput = "{\n\"status\": \"success\",\n\"data\": {\n\"translatedText\": \"Ueberschrift h1\"\n}\n}";
        mockResponseExtraction(expectedResponseOutput);
        Response actualResponse = webCrawler.executeAPIRequest("Heading h1");
        assertEquals(mockResponse.body().string(), actualResponse.body().string());
    }

    @Test
    void testTranslationApiRequestCreation() {
        String sourceLanguage = "auto";
        String targetLanguage = "de";
        String headerText = "Headline 1";
        createBody(sourceLanguage, targetLanguage, headerText);
        createRequest();
        mockGetAPIKey();

        Request actualRequestOutput = webCrawler.createTranslationApiRequest(expectedBody);

        assertEquals(expectedRequest.body(), actualRequestOutput.body());
        assertEquals(expectedRequest.url(), actualRequestOutput.url());
        assertEquals(expectedRequest.headers(), actualRequestOutput.headers());
    }

    private void createRequest() {
        String mockApiKey = "mocked-api-key";
        expectedRequest = new Request.Builder()
                .url("https://text-translator2.p.rapidapi.com/translate")
                .post(expectedBody)
                .addHeader("content-type", "application/x-www-form-urlencoded")
                .addHeader("X-RapidAPI-Key", mockApiKey)
                .addHeader("X-RapidAPI-Host", "text-translator2.p.rapidapi.com")
                .build();
    }

    private void createBody(String sourceLanguage, String targetLanguage, String headerText) {
        expectedBody = new FormBody.Builder()
                .add("source_language", sourceLanguage)
                .add("target_language", targetLanguage)
                .add("text", headerText)
                .build();
    }

    private void mockNewClientCall() throws IOException {
        when(mockClient.newCall(any())).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(mockResponse);
        webCrawler.setClient(mockClient);
    }

    private void mockGetAPIKey() {
        doReturn("mocked-api-key").when(webCrawler).getApiKey();
    }

    private void mockResponseExtraction(String expectedResponseOutput) throws IOException {
        when(mockResponse.body()).thenReturn(mockResponseBody);
        when(mockResponseBody.string()).thenReturn(expectedResponseOutput);
    }

    private Elements addElements() {
        Elements headlineElements = new Elements();
        Element headline = new Element("h1").text("Heading h1");
        headlineElements.add(headline);
        return headlineElements;
    }

    private void assertEqualFileContent(String expected, String path) throws IOException {
        webCrawler.flushWriter();
        String content = new String(Files.readAllBytes(Paths.get(path)));
        assertEquals(expected, content);
    }
}
