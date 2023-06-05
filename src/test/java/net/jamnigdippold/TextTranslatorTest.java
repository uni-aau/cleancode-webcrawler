package net.jamnigdippold;

import okhttp3.*;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TextTranslatorTest {
    private static TextTranslator translator;
    private FormBody expectedBody;
    private Request expectedRequest;
    private final Logger logger = ErrorLogger.getInstance();
    @Mock
    Request mockedRequest;
    @Mock
    OkHttpWrapper mockedClient;
    @Mock
    Response mockedResponse;
    @Mock
    ResponseBody mockedResponseBody;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        translator = spy(new TextTranslator());
        translator.setTargetLanguage("de");
    }

    @AfterEach
    public void teardown() {
        logger.clearLog();
    }

    @Test
    void testTranslate() {
        String input = "Headline";
        String expectedTranslation = "Überschrift";
        doNothing().when(translator).setSourceLanguage(input);
        doReturn(expectedTranslation).when(translator).getTranslatedHeadline(input);

        String result = translator.translate(input);

        assertEquals(expectedTranslation, result);
        verify(translator).setSourceLanguage(input);
        verify(translator).getTranslatedHeadline(input);
    }

    @Test
    void testCorrectSettingOfSourceLanguageEnglish() throws IOException {
        String expectedSourceLanguage = "en";
        String expectedResponseOutput = "{\n\"status\": \"success\",\n\"data\": {\n\"translatedText\": \"Ueberschrift h1\",\n\"detectedSourceLanguage\": {\n\"code\": \"en\",\n\"name\": \"English\"\n}\n}\n}";
        String headlineText = "Heading h1";

        mockResponseExtraction(expectedResponseOutput);
        doReturn(mockedResponse).when(translator).executeAPIRequest("Heading h1");

        translator.setSourceLanguage(headlineText);

        assertEquals(expectedSourceLanguage, translator.getSourceLanguage());
    }

    @Test
    void testSetSourceLanguageNoHeadlines() throws IOException {
        String expectedSourceLanguage = "auto";
        String expectedResponseOutput = "{\n\"status\": \"success\",\n\"data\": {\n\"translatedText\": \"Ueberschrift h1\",\n\"detectedSourceLanguage\": {\n\"code\": \"en\",\n\"name\": \"English\"\n}\n}\n}";
        String headlineText = "";

        mockResponseExtraction(expectedResponseOutput);
        doReturn(mockedResponse).when(translator).executeAPIRequest("Heading h1");

        translator.setSourceLanguage(headlineText);

        assertEquals(expectedSourceLanguage, translator.getSourceLanguage());
    }

    @Test
    void testRequestBodyCreation() throws IOException {
        String sourceLanguage = "auto";
        String targetLanguage = "de";
        String headerText = "Headline 1";
        createBody(sourceLanguage, targetLanguage, headerText);

        RequestBody actualBody = translator.createNewRequestBody(headerText);

        assertEquals(expectedBody.contentType(), actualBody.contentType());
        assertEquals(expectedBody.contentLength(), actualBody.contentLength());
    }

    @Test
    void testTranslationRequestExecution() throws IOException {
        mockNewClientCall();

        assertNotNull(translator.executeTranslationApiRequest(mockedRequest));
    }

    @Test
    void testTranslationRequestExecutionError() throws IOException {
        when(mockedClient.executeRequest(any())).thenThrow(new IOException());

        translator.setClient(mockedClient);
        translator.executeTranslationApiRequest(mockedRequest);

        assertEquals("Error while executing translation request: java.io.IOException", logger.getErrorLog().get(0));
    }

    @Test
    void testTranslationExtraction() throws IOException {
        String expectedReturnValue = "Überschrift h1";
        String expectedResponseOutput = "{\n\"status\": \"success\",\n\"data\": {\n\"translatedText\": \"Überschrift h1\"\n}\n}";
        mockResponseExtraction(expectedResponseOutput);

        String actualReturnValue = translator.extractTranslatedText(mockedResponse);

        assertEquals(expectedReturnValue, actualReturnValue);
    }

    @Test
    void testTranslatedTextExtractionError() throws IOException {
        String expectedResponseOutput = "{\n\"status\": \"success\",\n\"data\": {\n\"translatedText\": \"Ueberschrift h1\"\n}\n}";
        mockResponseExtraction(expectedResponseOutput);
        doThrow(new IOException()).when(mockedResponseBody).string();

        translator.extractTranslatedText(mockedResponse);

        assertEquals("Error while trying to extract translated text: java.io.IOException", logger.getErrorLog().get(0));
    }

    @Test
    void testTranslatedTextExtractionFallback() throws IOException {
        String expectedResponseOutput = "{\n\"status\": \"error\",\n\"message\": \"source language cannot be the same as target language\"\n}";
        mockResponseExtraction(expectedResponseOutput);

        String actualReturnValue = translator.extractTranslatedText(mockedResponse);

        assertEquals("", actualReturnValue);
    }

    @Test
    void testLanguageCodeExtraction() throws IOException {
        String expectedLanguageCode = "en";
        String expectedResponseOutput = "{\n\"status\": \"success\",\n\"data\": {\n\"translatedText\": \"Ueberschrift h1\",\n\"detectedSourceLanguage\": {\n\"code\": \"en\",\n\"name\": \"English\"\n}\n}\n}";
        mockResponseExtraction(expectedResponseOutput);

        String actualLanguageCode = translator.extractLanguageCode(mockedResponse);

        assertEquals(expectedLanguageCode, actualLanguageCode);
    }

    @Test
    void testLanguageCodeExtractionError() throws IOException {
        String expectedResponseOutput = "{\n\"status\": \"success\",\n\"data\": {\n\"translatedText\": \"Ueberschrift h1\",\n\"detectedSourceLanguage\": {\n\"code\": \"en\",\n\"name\": \"English\"\n}\n}\n}";
        mockResponseExtraction(expectedResponseOutput);
        doThrow(new IOException()).when(mockedResponseBody).string();

        translator.extractLanguageCode(mockedResponse);

        assertEquals("Error while trying to extract language code: java.io.IOException", logger.getErrorLog().get(0));
    }

    @Test
    void testGetTranslatedHeadline() throws IOException {
        String expectedTranslatedHeadline = "Ueberschrift h1";
        String expectedResponseOutput = "{\n\"status\": \"success\",\n\"data\": {\n\"translatedText\": \"Ueberschrift h1\"\n}\n}";
        mockResponseExtraction(expectedResponseOutput);
        doReturn(mockedResponse).when(translator).executeAPIRequest("Heading h1");

        String actualTranslatedHeadline = translator.getTranslatedHeadline("Heading h1");

        assertEquals(expectedTranslatedHeadline, actualTranslatedHeadline);
    }

    @Test
    void testGetTranslatedHeadlineFallback() throws IOException {
        String expectedTranslatedHeadline = "Heading h1";
        String expectedResponseOutput = "{\n\"status\": \"error\",\n\"message\": \"source language cannot be the same as target language\"\n}";
        mockResponseExtraction(expectedResponseOutput);
        doReturn(mockedResponse).when(translator).executeAPIRequest("Heading h1");

        String actualTranslatedHeadline = translator.getTranslatedHeadline("Heading h1");

        assertEquals("", actualTranslatedHeadline); // todo adapt when fixing null handling
    }

    @Test
    void testGetLanguageCodeFromHeadline() throws IOException {
        String expectedLanguageCode = "en";
        String expectedResponseOutput = "{\n\"status\": \"success\",\n\"data\": {\n\"translatedText\": \"Ueberschrift h1\",\n\"detectedSourceLanguage\": {\n\"code\": \"en\",\n\"name\": \"English\"\n}\n}\n}";
        mockResponseExtraction(expectedResponseOutput);
        doReturn(mockedResponse).when(translator).executeAPIRequest("Heading h1");

        String actualLanguageCode = translator.getLanguageCodeFromHeadline("Heading h1");

        assertEquals(expectedLanguageCode, actualLanguageCode);
    }

    @Test
    void testGetSystemEnv() {
        String expectedKey = System.getenv("RAPIDAPI_API_KEY");
        String actualKey = translator.getApiKey();

        assertEquals(expectedKey, actualKey);
    }

    @Test
    void testExecuteAPIRequest() throws IOException {
        mockNewClientCall();
        mockGetAPIKey();
        String expectedResponseOutput = "{\n\"status\": \"success\",\n\"data\": {\n\"translatedText\": \"Ueberschrift h1\"\n}\n}";
        mockResponseExtraction(expectedResponseOutput);

        Response actualResponse = translator.executeAPIRequest("Heading h1");

        assertEquals(mockedResponse.body().string(), actualResponse.body().string());
    }

    @Test
    void testTranslationApiRequestCreation() {
        String sourceLanguage = "auto";
        String targetLanguage = "de";
        String headerText = "Headline 1";
        createBody(sourceLanguage, targetLanguage, headerText);
        createRequest();
        mockGetAPIKey();

        Request actualRequestOutput = translator.createTranslationApiRequest(expectedBody);

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
        when(mockedClient.executeRequest(any())).thenReturn(mockedResponse);
//        when(mockedCall.execute()).thenReturn(mockedResponse); // TODO
        translator.setClient(mockedClient);
    }

    private void mockResponseExtraction(String expectedResponseOutput) throws IOException {
        when(mockedResponse.body()).thenReturn(mockedResponseBody);
        when(mockedResponseBody.string()).thenReturn(expectedResponseOutput);
    }

    private void mockGetAPIKey() {
        doReturn("mocked-api-key").when(translator).getApiKey();
    }

    private Elements addElements() {
        Elements headlineElements = new Elements();
        Element headline = new Element("h1").text("Heading h1");
        headlineElements.add(headline);

        return headlineElements;
    }
}
