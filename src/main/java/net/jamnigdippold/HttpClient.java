package net.jamnigdippold;

import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

public interface HttpClient {
    Response executeRequest(Request request) throws IOException;
}
