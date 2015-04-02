package com.thoughtworks.go.plugin.infra.listeners;

import com.thoughtworks.go.plugin.infra.commons.PluginsList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

public class PluginsListListenerTest {
    @Mock
    private PluginsList pluginsList;
    private PluginsListListener pluginsListListener;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        pluginsListListener = new PluginsListListener(pluginsList);
    }

    @Test
    public void shouldUpdatePluginListOnHandle() throws Exception {
        pluginsListListener.handle();

        verify(pluginsList).updatePluginsList();
    }
}