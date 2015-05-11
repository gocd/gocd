package com.thoughtworks.go.domain;

import com.google.gson.GsonBuilder;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class PluginTest {
    @Test
    public void shouldAnswerRequiresUpdateTrueWhenConfigurationDataIsUpdated() throws Exception {
        Map<String, String> configuration = new HashMap<String, String>();
        configuration.put("k1", "v1");
        configuration.put("k2", "v2");
        String configurationJSON = new GsonBuilder().create().toJson(configuration);
        Plugin plugin = new Plugin("plugin-id", configurationJSON);

        Map<String, String> updatedConfiguration = new HashMap<String, String>();
        updatedConfiguration.put("k1", "v1");
        updatedConfiguration.put("k2", "v3");
        assertThat(plugin.requiresUpdate(updatedConfiguration), is(true));
    }

    @Test
    public void shouldAnswerRequiresUpdateFalseWhenConfigurationDataIsSame() throws Exception {
        Map<String, String> configuration = new HashMap<String, String>();
        configuration.put("k1", "v1");
        configuration.put("k2", "v2");
        String configurationJSON = new GsonBuilder().create().toJson(configuration);
        Plugin plugin = new Plugin("plugin-id", configurationJSON);

        Map<String, String> updatedConfiguration = new HashMap<String, String>();
        updatedConfiguration.put("k1", "v1");
        updatedConfiguration.put("k2", "v2");
        assertThat(plugin.requiresUpdate(updatedConfiguration), is(false));
    }

    @Test
    public void shouldAnswerRequiresUpdateFalseWhenConfigurationDataIsEmpty() throws Exception {
        Plugin plugin = new Plugin("plugin-id", null);
        assertThat(plugin.requiresUpdate(null), is(false));
    }
}
