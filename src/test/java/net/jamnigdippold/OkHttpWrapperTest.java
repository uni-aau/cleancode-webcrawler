package net.jamnigdippold;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.*;

public class OkHttpWrapperTest {
    @Test
    void testExecuteRequest() throws IOException {
        OkHttpWrapper wrapper = spy(OkHttpWrapper.class);
        OkHttpClient client = mock(OkHttpClient.class);
        Call call = mock(Call.class);
        Request request = mock(Request.class);
        Response response = mock(Response.class);

        doReturn(call).when(client).newCall(request);
        doReturn(response).when(call).execute();
        wrapper.setClient(client);

        Response actualResponse = wrapper.executeRequest(request);

        verify(client).newCall(request);
        verify(call).execute();
        Assertions.assertEquals(response, actualResponse);
    }
}
