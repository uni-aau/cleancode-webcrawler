package net.jamnigdippold;

import org.jsoup.nodes.Document;

import java.io.IOException;

public interface DocumentFetcher {
    Document getConnection(String crawledLink) throws IOException;
}
