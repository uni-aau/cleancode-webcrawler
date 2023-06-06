package net.jamnigdippold;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

public class OkHttpWrapper implements HttpClient {
    private OkHttpClient client;

    public OkHttpWrapper() {
        this.client = new OkHttpClient();
    }

    @Override
    public Response executeRequest(Request request) throws IOException {
        return client.newCall(request).execute();
    }

    protected void setClient(OkHttpClient client) {
        this.client = client;
    }
}
