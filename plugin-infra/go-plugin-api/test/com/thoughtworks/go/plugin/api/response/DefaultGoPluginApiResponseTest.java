package com.thoughtworks.go.plugin.api.response;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class DefaultGoPluginApiResponseTest {

    @Test
    public void shouldReturnResponseForBadRequest() throws Exception {
        DefaultGoPluginApiResponse response = DefaultGoPluginApiResponse.badRequest("responseBody");
        assertThat(response.responseCode(), is(400));
        assertThat(response.responseBody(), is("responseBody"));
    }

    @Test
    public void shouldReturnResponseForIncompleteRequest() throws Exception {
        DefaultGoPluginApiResponse response = DefaultGoPluginApiResponse.incompleteRequest("responseBody");
        assertThat(response.responseCode(), is(412));
        assertThat(response.responseBody(), is("responseBody"));
    }

    @Test
    public void shouldReturnResponseForErrorRequest() throws Exception {
        DefaultGoPluginApiResponse response = DefaultGoPluginApiResponse.error("responseBody");
        assertThat(response.responseCode(), is(500));
        assertThat(response.responseBody(), is("responseBody"));
    }

    @Test
    public void shouldReturnResponseForSuccessRequest() throws Exception {
        DefaultGoPluginApiResponse response = DefaultGoPluginApiResponse.success("responseBody");
        assertThat(response.responseCode(), is(200));
        assertThat(response.responseBody(), is("responseBody"));
    }

}