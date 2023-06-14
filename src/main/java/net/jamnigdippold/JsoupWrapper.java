package net.jamnigdippold;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;

public class JsoupWrapper implements DocumentFetcher {
    @Override
    public Document getConnection(String crawledLink) throws IOException {
        return Jsoup.connect(crawledLink).get();
    }
}
