package com.thoughtworks.go.server.util;

import org.junit.Test;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.Response;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class Jetty6ServletHelperTest {
    @Test
    public void shouldGetInstanceOfServletHelper(){
        ServletHelper.init(false);
        assertThat(ServletHelper.getInstance() instanceof Jetty6ServletHelper, is(true));
    }

    @Test
    public void shouldGetJetty6Request() {
        ServletRequest request = new Jetty6ServletHelper().getRequest(mock(Request.class));
        assertThat(request instanceof Jetty6Request, is(true));
    }

    @Test
    public void shouldGetJetty6Response() {
        ServletResponse response = new Jetty6ServletHelper().getResponse(mock(Response.class));
        assertThat(response instanceof Jetty6Response, is(true));
    }
}