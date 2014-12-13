package com.thoughtworks.go.plugin.infra.commons;

import org.apache.commons.httpclient.HttpStatus;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class PluginUploadResponseTest {

    @Test
    public void shouldCreateASuccessResponse() {

        PluginUploadResponse response = PluginUploadResponse.create(true, "success message", null);

        assertThat(response.success(), is("success message"));
        assertThat(response.errors().size(), is(0));
    }

    @Test
    public void shouldCreateAResponseWithErrors() {
        Map<Integer, String> errors = new HashMap<Integer, String>();
        int errorCode = HttpStatus.SC_INTERNAL_SERVER_ERROR;
        String errorMessage = new FileNotFoundException().getMessage();
        errors.put(errorCode, errorMessage);

        PluginUploadResponse response = PluginUploadResponse.create(false, null, errors);

        assertThat(response.success(), is(""));
        assertThat(response.errors().get(errorCode), is(errorMessage));
    }
}
