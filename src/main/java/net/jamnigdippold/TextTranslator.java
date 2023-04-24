package net.jamnigdippold;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.jsoup.select.Elements;

import java.io.IOException;

public class TextTranslator {
    private OkHttpClient client = new OkHttpClient();
    private String sourceLanguage;
    private final String targetLanguage;

    public TextTranslator(String targetLanguage) {
        this.sourceLanguage = "auto";
        this.targetLanguage = targetLanguage;
    }

    protected void setSourceLanguage(Elements crawledHeadlineElements) {
        String headline = crawledHeadlineElements.get(0).text();
        sourceLanguage = getLanguageCodeFromHeadline(headline);
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
            return client.newCall(translationApiRequest).execute();
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
        String apiResponseBody;
        JsonNode node;

        apiResponseBody = apiResponse.body().string();
        node = new ObjectMapper().readTree(apiResponseBody);

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
        String apiResponseBody;
        JsonNode node;

        apiResponseBody = apiResponse.body().string();
        node = new ObjectMapper().readTree(apiResponseBody);

        return node.get("data").get("detectedSourceLanguage").get("code").asText();

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

    public String getTargetLanguage() {
        return targetLanguage;
    }

    public void setClient(OkHttpClient client) {
        this.client = client;
    }
}
