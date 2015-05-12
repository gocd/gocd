package com.thoughtworks.go.plugin.access.common.settings;

import com.thoughtworks.go.plugin.api.config.Property;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.*;

public class PluginSettingsMetadataStoreTest {
    @Before
    public void setUp() {
        PluginSettingsMetadataStore.getInstance().clear();
    }

    @After
    public void tearDown() {
        PluginSettingsMetadataStore.getInstance().clear();
    }

    @Test
    public void shouldPopulateDataCorrectly() {
        String existingPluginId = "plugin-id";
        PluginSettingsConfiguration configuration = new PluginSettingsConfiguration();
        String template = "template-value";
        PluginSettingsMetadataStore.getInstance().addMetadataFor(existingPluginId, configuration, template);

        assertThat(PluginSettingsMetadataStore.getInstance().configuration(existingPluginId), is(configuration));
        assertThat(PluginSettingsMetadataStore.getInstance().template(existingPluginId), is(template));

        String nonExistingPluginId = "some-plugin-which-does-not-exist";
        assertThat(PluginSettingsMetadataStore.getInstance().configuration(nonExistingPluginId), is(nullValue()));
        assertThat(PluginSettingsMetadataStore.getInstance().template(nonExistingPluginId), is(nullValue()));
    }

    @Test
    public void shouldRemoveDataCorrectly() {
        String pluginId = "plugin-id";
        PluginSettingsMetadataStore.getInstance().addMetadataFor(pluginId, new PluginSettingsConfiguration(), "template-value");
        assertThat(PluginSettingsMetadataStore.getInstance().hasPlugin(pluginId), is(true));

        PluginSettingsMetadataStore.getInstance().removeMetadataFor(pluginId);
        assertThat(PluginSettingsMetadataStore.getInstance().hasPlugin(pluginId), is(false));
    }

    @Test
    public void shouldBeAbleToCheckIfPluginExists() {
        PluginSettingsConfiguration configuration = new PluginSettingsConfiguration();
        String template = "template-value";
        PluginSettingsMetadataStore.getInstance().addMetadataFor("plugin-id", configuration, template);

        assertThat(PluginSettingsMetadataStore.getInstance().hasPlugin("plugin-id"), is(true));
        assertThat(PluginSettingsMetadataStore.getInstance().hasPlugin("some-plugin-which-does-not-exist"), is(false));
    }

    @Test
    public void shouldCheckIfPluginSettingsConfigurationHasOption() {
        PluginSettingsConfiguration configuration = new PluginSettingsConfiguration();
        PluginSettingsProperty p1 = createProperty("k1", true);
        PluginSettingsProperty p2 = createProperty("k2", false);
        configuration.add(p1);
        configuration.add(p2);

        PluginSettingsMetadataStore.getInstance().addMetadataFor("plugin-id", configuration, "template-value");

        assertTrue(PluginSettingsMetadataStore.getInstance().hasOption("plugin-id", "k1", Property.REQUIRED));
        assertFalse(PluginSettingsMetadataStore.getInstance().hasOption("plugin-id", "k2", Property.REQUIRED));
    }

    private PluginSettingsProperty createProperty(String key, boolean isRequired) {
        PluginSettingsProperty property = new PluginSettingsProperty(key);
        property.with(Property.REQUIRED, isRequired);
        return property;
    }
}
