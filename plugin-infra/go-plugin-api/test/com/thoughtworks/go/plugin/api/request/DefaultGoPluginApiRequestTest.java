package com.thoughtworks.go.plugin.api.request;

import org.junit.Test;

import java.util.Map;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class DefaultGoPluginApiRequestTest {

    @Test
    public void shouldBeInstanceOfGoPluginApiRequest(){
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
        Map<String, Object> requestParameters = request.requestParameters();
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
        assertThat(request.extension(),is(extension));
        assertThat(request.extensionVersion(),is(version));
        assertThat(request.requestName(),is(requestName));
    }
}