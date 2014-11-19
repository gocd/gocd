package com.thoughtworks.go.plugin.api.response;

import org.junit.Test;

import java.util.Map;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class DefaultGoApiResponseTest {

    @Test
    public void shouldBeInstanceOfGoApiResponse() throws Exception {
        DefaultGoApiResponse response = new DefaultGoApiResponse(0);
        assertThat(response, instanceOf(GoApiResponse.class));
    }

    @Test
    public void shouldReturnUnmodifiableResponseHeaders() throws Exception {
        DefaultGoApiResponse response = new DefaultGoApiResponse(0);
        Map<String, String> headers = response.responseHeaders();
        try {
            headers.put("new-key", "new-value");
            fail("Should not allow modification of response headers");
        } catch (UnsupportedOperationException e) {
        }
        try {
            headers.remove("key");
            fail("Should not allow modification of response headers");
        } catch (UnsupportedOperationException e) {
        }
    }

    @Test
    public void shouldBeAbleToSetAndGetResponseBody() throws Exception {
        DefaultGoApiResponse response = new DefaultGoApiResponse(0);
        String responseBody = "response-body";
        response.setResponseBody(responseBody);
        assertThat(response.responseBody(), is(responseBody));
    }

    @Test
    public void shouldBeAbleToInitializeResponse() throws Exception {
        int responseCode = 0;
        GoApiResponse response = new DefaultGoApiResponse(responseCode);
        assertThat(response.responseCode(), is(responseCode));
    }

    @Test
    public void shouldReturnResponseForBadRequest() throws Exception {
        DefaultGoApiResponse response = DefaultGoApiResponse.badRequest("responseBody");
        assertThat(response.responseCode(), is(400));
        assertThat(response.responseBody(), is("responseBody"));
    }

    @Test
    public void shouldReturnResponseForIncompleteRequest() throws Exception {
        DefaultGoApiResponse response = DefaultGoApiResponse.incompleteRequest("responseBody");
        assertThat(response.responseCode(), is(412));
        assertThat(response.responseBody(), is("responseBody"));
    }

    @Test
    public void shouldReturnResponseForErrorRequest() throws Exception {
        DefaultGoApiResponse response = DefaultGoApiResponse.error("responseBody");
        assertThat(response.responseCode(), is(500));
        assertThat(response.responseBody(), is("responseBody"));
    }

    @Test
    public void shouldReturnResponseForSuccessRequest() throws Exception {
        DefaultGoApiResponse response = DefaultGoApiResponse.success("responseBody");
        assertThat(response.responseCode(), is(200));
        assertThat(response.responseBody(), is("responseBody"));
    }
}