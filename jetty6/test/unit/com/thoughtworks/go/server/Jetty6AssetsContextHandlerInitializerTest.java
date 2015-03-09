package com.thoughtworks.go.server;

import org.junit.Test;
import org.mortbay.jetty.webapp.WebAppContext;

import java.io.IOException;

import static org.mockito.Mockito.*;

public class Jetty6AssetsContextHandlerInitializerTest {
    @Test
    public void shouldInitializeHandlerOnWebappContextLifeCycleStarted() throws IOException {
        Jetty6AssetsContextHandler handler = mock(Jetty6AssetsContextHandler.class);
        WebAppContext webAppContext = mock(WebAppContext.class);
        Jetty6AssetsContextHandlerInitializer initializer = new Jetty6AssetsContextHandlerInitializer(handler, webAppContext);
        initializer.lifeCycleStarted(null);
        verify(handler, times(1)).init(webAppContext);
    }

    @Test
    public void shouldNotInitializeHandlerOnOtherWebappContextLifeCycleEvents() throws IOException {
        Jetty6AssetsContextHandler handler = mock(Jetty6AssetsContextHandler.class);
        WebAppContext webAppContext = mock(WebAppContext.class);
        Jetty6AssetsContextHandlerInitializer initializer = new Jetty6AssetsContextHandlerInitializer(handler, webAppContext);
        initializer.lifeCycleStarting(null);
        verify(handler, never()).init(webAppContext);
    }


}