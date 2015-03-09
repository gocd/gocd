package com.thoughtworks.go.server.util;

import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.server.Request;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class Jetty9RequestTest {
    private Jetty9Request jetty9Request;
    private Request request;

    @Before
    public void setUp() throws Exception {
        request = mock(Request.class);
        jetty9Request = new Jetty9Request(request);
        when(request.getUri()).thenReturn(new HttpURI("foo/bar/baz"));
        when(request.getRootURL()).thenReturn(new StringBuilder("http://junk/"));
    }

    @Test
    public void shouldGetUrl() {
        assertThat(jetty9Request.getUrl(), is("http://junk/foo/bar/baz"));
    }

    @Test
    public void shouldGetUriPath() {
        assertThat(jetty9Request.getUriPath(), is("foo/bar/baz"));
    }

    @Test
    public void shouldGetUriAsString() {
        assertThat(jetty9Request.getUriAsString(), is("foo/bar/baz"));
    }
}