package com.thoughtworks.go.plugin.access.scm;

import com.thoughtworks.go.plugin.api.GoPlugin;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

public class SCMMetadataLoaderTest {
    private SCMMetadataLoader metadataLoader;
    private GoPluginDescriptor pluginDescriptor;
    private SCMExtension scmExtension;
    private PluginManager pluginManager;

    @Before
    public void setUp() throws Exception {
        pluginDescriptor = new GoPluginDescriptor("plugin-id", "1.0", null, null, null, true);
        pluginManager = mock(PluginManager.class);
        scmExtension = mock(SCMExtension.class);
        metadataLoader = new SCMMetadataLoader(scmExtension, pluginManager);

        SCMMetadataStore.getInstance().removeMetadata(pluginDescriptor.id());
    }

    @Test
    public void shouldFetchSCMMetadataForPluginsWhichImplementSCMExtensionPoint() {
        SCMPropertyConfiguration scmPropertyConfiguration = new SCMPropertyConfiguration();
        scmPropertyConfiguration.add(new SCMProperty("k1").with(SCMProperty.REQUIRED, true).with(SCMProperty.PART_OF_IDENTITY, false));

        when(scmExtension.getSCMConfiguration(pluginDescriptor.id())).thenReturn(scmPropertyConfiguration);

        metadataLoader.fetchSCMMetaData(pluginDescriptor);

        SCMConfigurations metadata = SCMMetadataStore.getInstance().getMetadata(pluginDescriptor.id());
        assertThat(metadata.size(), is(1));
        SCMConfiguration scmConfiguration = metadata.get("k1");
        assertThat(scmConfiguration.getKey(), is("k1"));
        assertThat(scmConfiguration.getOption(SCMProperty.REQUIRED), is(true));
        assertThat(scmConfiguration.getOption(SCMProperty.PART_OF_IDENTITY), is(false));
    }

    @Test
    public void shouldThrowExceptionWhenNullSCMConfigurationReturned() {
        when(scmExtension.getSCMConfiguration(pluginDescriptor.id())).thenReturn(null);
        try {
            metadataLoader.fetchSCMMetaData(pluginDescriptor);
        } catch (Exception e) {
            assertThat(e.getMessage(), is("Plugin[plugin-id] returned null SCM configuration"));
        }
        assertThat(SCMMetadataStore.getInstance().getMetadata(pluginDescriptor.id()), nullValue());
    }

    @Test
    public void shouldRegisterAsPluginFrameworkStartListener() throws Exception {
        metadataLoader = new SCMMetadataLoader(scmExtension, pluginManager);

        verify(pluginManager).addPluginChangeListener(metadataLoader, GoPlugin.class);
    }

    @Test
    public void shouldFetchMetadataOnPluginLoadedCallback() throws Exception {
        SCMMetadataLoader spy = spy(metadataLoader);
        doNothing().when(spy).fetchSCMMetaData(pluginDescriptor);
        when(scmExtension.isSCMPlugin(pluginDescriptor.id())).thenReturn(true);

        spy.pluginLoaded(pluginDescriptor);

        verify(spy).fetchSCMMetaData(pluginDescriptor);
    }

    @Test
    public void shouldNotTryToFetchMetadataOnPluginLoadedCallback() throws Exception {
        SCMMetadataLoader spy = spy(metadataLoader);
        when(scmExtension.isSCMPlugin(pluginDescriptor.id())).thenReturn(false);

        spy.pluginLoaded(pluginDescriptor);

        verify(spy, never()).fetchSCMMetaData(pluginDescriptor);
    }

    @Test
    public void shouldRemoveMetadataOnPluginUnLoadedCallback() throws Exception {
        SCMMetadataStore.getInstance().addMetadataFor(pluginDescriptor.id(), new SCMConfigurations());
        when(scmExtension.isSCMPlugin(pluginDescriptor.id())).thenReturn(true);

        metadataLoader.pluginUnLoaded(pluginDescriptor);

        assertThat(SCMMetadataStore.getInstance().getMetadata(pluginDescriptor.id()), is(nullValue()));
    }

    @Test
    public void shouldNotTryRemoveMetadataOnPluginUnLoadedCallback() throws Exception {
        SCMConfigurations scmConfigurations = new SCMConfigurations();
        SCMMetadataStore.getInstance().addMetadataFor(pluginDescriptor.id(), scmConfigurations);
        when(scmExtension.isSCMPlugin(pluginDescriptor.id())).thenReturn(false);

        metadataLoader.pluginUnLoaded(pluginDescriptor);

        assertThat(SCMMetadataStore.getInstance().getMetadata(pluginDescriptor.id()), is(scmConfigurations));
    }
}