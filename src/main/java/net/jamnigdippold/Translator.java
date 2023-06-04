package net.jamnigdippold;

public interface Translator {
    void setTargetLanguage(String targetLanguage);
    String translate(String input);
}
