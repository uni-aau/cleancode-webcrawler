package net.jamnigdippold;

public interface Translator {
    void setTargetLanguage(String targetLanguage);
    String detectLanguage(String input);
    String translate(String input);
}
