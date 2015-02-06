package com.thoughtworks.go.server;

import org.eclipse.jetty.webapp.WebAppContext;
import org.junit.Test;
import java.io.IOException;
import static org.mockito.Mockito.*;

public class AssetsContextHandlerInitializerTest {
    @Test
    public void shouldInitializeHandlerOnWebappContextLifeCycleStarted() throws IOException {
        AssetsContextHandler handler = mock(AssetsContextHandler.class);
        WebAppContext webAppContext = mock(WebAppContext.class);
        AssetsContextHandlerInitializer initializer = new AssetsContextHandlerInitializer(handler, webAppContext);
        initializer.lifeCycleStarted(null);
        verify(handler, times(1)).init(webAppContext);
    }

    @Test
    public void shouldNotInitializeHandlerOnOtherWebappContextLifeCycleEvents() throws IOException {
        AssetsContextHandler handler = mock(AssetsContextHandler.class);
        WebAppContext webAppContext = mock(WebAppContext.class);
        AssetsContextHandlerInitializer initializer = new AssetsContextHandlerInitializer(handler, webAppContext);
        initializer.lifeCycleStarting(null);
        verify(handler, never()).init(webAppContext);
    }
}