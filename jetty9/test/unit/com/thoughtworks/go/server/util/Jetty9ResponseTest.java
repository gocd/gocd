package com.thoughtworks.go.server.util;

import org.eclipse.jetty.server.Response;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class Jetty9ResponseTest {
    private Jetty9Response jetty9Response;
    private Response response;

    @Before
    public void setUp() throws Exception {
        response = mock(Response.class);
        jetty9Response = new Jetty9Response(response);
    }

    @Test
    public void shouldGetResponseStatus() {
        when(response.getStatus()).thenReturn(200);
        assertThat(jetty9Response.getStatus(), is(200));
    }

    @Test
    public void shouldGetResponseContentCount() {
        when(response.getContentCount()).thenReturn(2000l);
        assertThat(jetty9Response.getContentCount(), is(2000l));
    }
}