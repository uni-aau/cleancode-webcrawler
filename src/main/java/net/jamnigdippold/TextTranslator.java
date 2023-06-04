package net.jamnigdippold;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.io.IOException;

public class TextTranslator implements Translator {
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
        return System.getenv("RAPIDAPI_API_KEY");
    }

    protected Response executeTranslationApiRequest(Request translationApiRequest) {
        try {
            return httpClient.executeRequest(translationApiRequest);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected String extractTranslatedText(Response apiResponse) {
        try {
            return extractTranslation(apiResponse);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected String extractTranslation(Response apiResponse) throws IOException {
        JsonNode node;

        node = createNode(apiResponse);

        if (checkNodeSuccessStatus(node)) {
            return node.get("data").get("translatedText").asText();
        } else {
            return null;
        }
    }

    private boolean checkNodeSuccessStatus(JsonNode node) {
        return node.get("status").asText().equals("success");
    }

    protected String extractLanguageCode(Response apiResponse) {
        try {
            return tryToExtractLanguageCode(apiResponse);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    protected String tryToExtractLanguageCode(Response apiResponse) throws IOException {
        JsonNode node;

        node = createNode(apiResponse);

        return node.get("data").get("detectedSourceLanguage").get("code").asText();
    }

    private JsonNode createNode(Response apiResponse) throws IOException {
        String apiResponseBody;

        apiResponseBody = apiResponse.body().string();

        return new ObjectMapper().readTree(apiResponseBody);
    }

    protected String getTranslatedHeadline(String crawledHeadlineText) {
        Response apiResponse = executeAPIRequest(crawledHeadlineText);
        String translatedString = extractTranslatedText(apiResponse);
        if (translatedString == null)
            translatedString = crawledHeadlineText;

        return translatedString;
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

    public String getTargetLanguage() { // TODO necessary?
        return targetLanguage;
    }

    public void setClient(HttpClient client) {
        this.httpClient = client;
    }
}
