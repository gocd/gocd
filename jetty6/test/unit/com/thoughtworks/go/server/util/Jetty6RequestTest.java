package com.thoughtworks.go.server.util;

import org.junit.Before;
import org.junit.Test;
import org.mortbay.jetty.HttpURI;
import org.mortbay.jetty.Request;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class Jetty6RequestTest {
    private Jetty6Request jettyRequest;
    private Request request;

    @Before
    public void setUp() throws Exception {
        request = mock(Request.class);
        jettyRequest = new Jetty6Request(request);
        when(request.getUri()).thenReturn(new HttpURI("foo/bar/baz"));
        when(request.getRootURL()).thenReturn(new StringBuffer("http://junk/"));
    }

    @Test
    public void shouldGetUrl() {
        assertThat(jettyRequest.getUrl(), is("http://junk/foo/bar/baz"));
    }

    @Test
    public void shouldGetUriPath() {
        assertThat(jettyRequest.getUriPath(), is("foo/bar/baz"));
    }

    @Test
    public void shouldGetUriAsString() {
        assertThat(jettyRequest.getUriAsString(), is("foo/bar/baz"));
    }

}