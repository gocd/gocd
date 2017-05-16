package com.thoughtworks.go.plugin.access.authorization;

import com.thoughtworks.go.plugin.api.info.PluginDescriptor;
import com.thoughtworks.go.plugin.domain.authorization.AuthorizationPluginInfo;
import com.thoughtworks.go.plugin.domain.authorization.Capabilities;
import com.thoughtworks.go.plugin.domain.authorization.SupportedAuthType;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AuthorizationMetadataStoreTest {

    private AuthorizationMetadataStore store;

    @Before
    public void setUp() throws Exception {
        store = new AuthorizationMetadataStore();
        AuthorizationPluginInfo plugin1 = pluginInfo("plugin-1", SupportedAuthType.Web);
        AuthorizationPluginInfo plugin2 = pluginInfo("plugin-2", SupportedAuthType.Password);
        AuthorizationPluginInfo plugin3 = pluginInfo("plugin-3", SupportedAuthType.Web);
        store.setPluginInfo(plugin1);
        store.setPluginInfo(plugin2);
        store.setPluginInfo(plugin3);

    }

    @Test
    public void shouldGetPluginsThatSupportWebBasedAuthorization() {
        Set<String> pluginsThatSupportsWebBasedAuthentication = store.getPluginsThatSupportsWebBasedAuthentication();
        assertThat(pluginsThatSupportsWebBasedAuthentication.size(), is(2));
        assertThat(pluginsThatSupportsWebBasedAuthentication.contains("plugin-1"), is(true));
        assertThat(pluginsThatSupportsWebBasedAuthentication.contains("plugin-3"), is(true));
    }

    @Test
    public void shouldGetPluginsThatSupportPasswordBasedAuthorization() {
        Set<String> pluginsThatSupportsWebBasedAuthentication = store.getPluginsThatSupportsPasswordBasedAuthentication();
        assertThat(pluginsThatSupportsWebBasedAuthentication.size(), is(1));
        assertThat(pluginsThatSupportsWebBasedAuthentication.contains("plugin-2"), is(true));
    }

    private AuthorizationPluginInfo pluginInfo(String pluginId, SupportedAuthType supportedAuthType) {
        AuthorizationPluginInfo pluginInfo = mock(AuthorizationPluginInfo.class);
        PluginDescriptor pluginDescriptor = mock(PluginDescriptor.class);
        when(pluginDescriptor.id()).thenReturn(pluginId);
        when(pluginInfo.getDescriptor()).thenReturn(pluginDescriptor);
        Capabilities capabilities = mock(Capabilities.class);
        when(capabilities.getSupportedAuthType()).thenReturn(supportedAuthType);
        when(pluginInfo.getCapabilities()).thenReturn(capabilities);
        return pluginInfo;
    }

}