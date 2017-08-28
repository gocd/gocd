package com.thoughtworks.go.plugin.api.request;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class DefaultGoPluginApiRequestTest {

    @Test
    public void shouldBeInstanceOfGoPluginApiRequest() {
        DefaultGoPluginApiRequest request = new DefaultGoPluginApiRequest("extension", "1.0", "request-name");
        assertThat(request, instanceOf(GoPluginApiRequest.class));
    }


    @Test
    public void shouldReturnUnmodifiableRequestHeaders() throws Exception {
        DefaultGoPluginApiRequest request = new DefaultGoPluginApiRequest("extension", "1.0", "request-name");
        Map<String, String> requestHeaders = request.requestHeaders();
        try {
            requestHeaders.put("new-key", "new-value");
            fail("Should not allow modification of request headers");
        } catch (UnsupportedOperationException e) {
        }
        try {
            requestHeaders.remove("key");
            fail("Should not allow modification of request headers");
        } catch (UnsupportedOperationException e) {
        }
    }

    @Test
    public void shouldReturnUnmodifiableRequestParams() throws Exception {
        DefaultGoPluginApiRequest request = new DefaultGoPluginApiRequest("extension", "1.0", "request-name");
        Map<String, String> requestParameters = request.requestParameters();
        try {
            requestParameters.put("new-key", "new-value");
            fail("Should not allow modification of request params");
        } catch (UnsupportedOperationException e) {
        }
        try {
            requestParameters.remove("key");
            fail("Should not allow modification of request params");
        } catch (UnsupportedOperationException e) {
        }
    }

    @Test
    public void shouldBeAbleToSetAndGetRequestBody() throws Exception {
        DefaultGoPluginApiRequest request = new DefaultGoPluginApiRequest("extension", "1.0", "request-name");
        String requestBody = "request-body";
        request.setRequestBody(requestBody);
        assertThat(request.requestBody(), is(requestBody));
    }

    @Test
    public void shouldBeAbleToInitializePluginResponse() throws Exception {
        String extension = "extension";
        String version = "1.0";
        String requestName = "request-name";
        DefaultGoPluginApiRequest request = new DefaultGoPluginApiRequest(extension, version, requestName);
        assertThat(request.extension(), is(extension));
        assertThat(request.extensionVersion(), is(version));
        assertThat(request.requestName(), is(requestName));
    }

    @Test
    public void shouldAbleToSetRequestHeaders() throws Exception {
        final DefaultGoPluginApiRequest request = new DefaultGoPluginApiRequest("extension", "1.0", "request-name");

        final Map<String, String> headers = new HashMap();
        headers.put("HEADER-1", "HEADER-VALUE-1");
        headers.put("HEADER-2", "HEADER-VALUE-2");

        request.setRequestHeaders(headers);

        assertThat(request.requestHeaders(), hasEntry("HEADER-1", "HEADER-VALUE-1"));
        assertThat(request.requestHeaders(), hasEntry("HEADER-2", "HEADER-VALUE-2"));
    }
}