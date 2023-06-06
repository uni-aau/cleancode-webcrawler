package net.jamnigdippold;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.io.IOException;

public class TextTranslator implements Translator {
    private static final Logger logger = ErrorLogger.getInstance();
    private HttpClient httpClient;
    private String sourceLanguage = "auto";
    private String targetLanguage;

    public TextTranslator() {
        this.httpClient = new OkHttpWrapper();
    }


    @Override
    public void setTargetLanguage(String targetLanguage) {
        this.targetLanguage = targetLanguage;
    }

    @Override
    public String translate(String input) {
        setSourceLanguage(input);
        return getTranslatedHeadline(input);
    }

    @Override
    public String detectLanguage(String input) {
        return getLanguageCodeFromHeadline(input);
    }

    protected void setSourceLanguage(String headlineText) {
        if (sourceLanguage.equals("auto") && !headlineText.equals("")) {
            sourceLanguage = getLanguageCodeFromHeadline(headlineText);
        }
    }

    protected RequestBody createNewRequestBody(String headerText) {
        return new FormBody.Builder()
                .add("source_language", sourceLanguage)
                .add("target_language", targetLanguage)
                .add("text", headerText)
                .build();
    }

    protected Request createTranslationApiRequest(okhttp3.RequestBody body) {
        String apiKey = getApiKey();
        return new Request.Builder()
                .url("https://text-translator2.p.rapidapi.com/translate")
                .post(body)
                .addHeader("content-type", "application/x-www-form-urlencoded")
                .addHeader("X-RapidAPI-Key", apiKey)
                .addHeader("X-RapidAPI-Host", "text-translator2.p.rapidapi.com")
                .build();
    }

    protected String getApiKey() {
        String key = getApiKeyFromSystem();
        if (key == null) {
            logger.logError("No API-Key found in System environment!");
            return "invalid key";
        }
        return key;
    }

    protected String getApiKeyFromSystem() {
        return System.getenv("RAPIDAPI_API_KEY");
    }

    protected Response executeTranslationApiRequest(Request translationApiRequest) {
        try {
            return httpClient.executeRequest(translationApiRequest);
        } catch (IOException e) {
            logger.logError("Error while executing translation request: " + e);
            return generateDefaultResponse(translationApiRequest);
        }
    }

    protected Response generateDefaultResponse(Request translationApiRequest) {
        ResponseBody responseBody = ResponseBody.create(MediaType.parse("application/json"), "{\"status\":\"failure\"}");
        return new Response.Builder()
                .code(444)
                .message("no response")
                .body(responseBody)
                .request(translationApiRequest)
                .protocol(Protocol.HTTP_2)
                .build();
    }

    protected String extractTranslatedText(Response apiResponse, String input) {
        try {
            return extractTranslation(apiResponse, input);
        } catch (IOException e) {
            logger.logError("Error while trying to extract translated text: " + e);
        } catch (NullPointerException e) {
            logger.logError("Error while trying to extract translated text, the Json format is incorrect: " + e);
        }
        return input;
    }

    protected String extractTranslation(Response apiResponse, String input) throws IOException, NullPointerException {
        JsonNode node;

        node = createNode(apiResponse);

        if (checkNodeSuccessStatus(node)) {
            return node.get("data").get("translatedText").asText();
        } else {
            return input;
        }
    }

    private boolean checkNodeSuccessStatus(JsonNode node) {
        try {
            if (node.get("status") == null) {
                logger.logError("Error while checking the success status of node: API-Response:" + node);
                return false;
            } else
                return node.get("status").asText().equals("success");
        } catch (NullPointerException e) {
            logger.logError("Error while checking the success status of node: " + e);
        }
        return false;
    }

    protected String extractLanguageCode(Response apiResponse) {
        try {
            return tryToExtractLanguageCode(apiResponse);
        } catch (IOException e) {
            logger.logError("Error while trying to extract language code: " + e);
        } catch (NullPointerException e) {
            logger.logError("Error while trying to extract language code, the Json format is incorrect: " + e);
        }
        return "auto";
    }

    protected String tryToExtractLanguageCode(Response apiResponse) throws IOException, NullPointerException {
        JsonNode node = createNode(apiResponse);
        if (checkNodeSuccessStatus(node)) {
            return node.get("data").get("detectedSourceLanguage").get("code").asText();
        } else {
            return "auto";
        }
    }

    private JsonNode createNode(Response apiResponse) throws IOException {
        String apiResponseBody;

        apiResponseBody = apiResponse.body().string();

        return new ObjectMapper().readTree(apiResponseBody);
    }

    protected String getTranslatedHeadline(String crawledHeadlineText) {
        Response apiResponse = executeAPIRequest(crawledHeadlineText);
        return extractTranslatedText(apiResponse, crawledHeadlineText);
    }

    protected String getLanguageCodeFromHeadline(String crawledHeadlineText) {
        Response apiResponse = executeAPIRequest(crawledHeadlineText);

        return extractLanguageCode(apiResponse);
    }

    protected Response executeAPIRequest(String crawledHeadlineText) {
        RequestBody body = createNewRequestBody(crawledHeadlineText);
        Request request = createTranslationApiRequest(body);

        return executeTranslationApiRequest(request);
    }

    public String getSourceLanguage() {
        return sourceLanguage;
    }

    public String getTargetLanguage() {
        return targetLanguage;
    }

    public void setClient(HttpClient client) {
        this.httpClient = client;
    }
}
