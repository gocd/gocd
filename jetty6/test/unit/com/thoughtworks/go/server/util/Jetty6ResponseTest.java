package com.thoughtworks.go.server.util;

import org.junit.Before;
import org.junit.Test;
import org.mortbay.jetty.Response;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class Jetty6ResponseTest {
    private Jetty6Response jetty6Response;
    private Response response;

    @Before
    public void setUp() throws Exception {
        response = mock(Response.class);
        jetty6Response = new Jetty6Response(response);
    }

    @Test
    public void shouldGetResponseStatus() {
        when(response.getStatus()).thenReturn(200);
        assertThat(jetty6Response.getStatus(), is(200));
    }

    @Test
    public void shouldGetResponseContentCount() {
        when(response.getContentCount()).thenReturn(2000l);
        assertThat(jetty6Response.getContentCount(), is(2000l));
    }
}