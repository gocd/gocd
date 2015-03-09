package com.thoughtworks.go.server.util;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class Jetty9ServletHelperTest {
    @Test
    public void shouldGetInstanceOfServletHelper(){
        ServletHelper.init(true);
        assertThat(ServletHelper.getInstance() instanceof Jetty9ServletHelper, is(true));
    }

    @Test
    public void shouldGetJetty6Request() {
        ServletRequest request = new Jetty9ServletHelper().getRequest(mock(Request.class));
        assertThat(request instanceof Jetty9Request, is(true));
    }

    @Test
    public void shouldGetJetty6Response() {
        ServletResponse response = new Jetty9ServletHelper().getResponse(mock(Response.class));
        assertThat(response instanceof Jetty9Response, is(true));
    }
}