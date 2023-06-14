package net.jamnigdippold;

import org.jsoup.Jsoup;

import java.io.IOException;

public class JsoupWrapper implements UrlValidator {
    @Override
    public boolean isBrokenLink(String crawledLink) {
        try {
            Jsoup.connect(crawledLink).get();
            return false;
        } catch (IOException | IllegalArgumentException exception) {
            return true;
        }
    }
}
