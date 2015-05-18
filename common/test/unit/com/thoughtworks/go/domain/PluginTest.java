package com.thoughtworks.go.domain;

import com.google.gson.GsonBuilder;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class PluginTest {
    private Plugin plugin;

    @Before
    public void setUp() throws Exception {
        Map<String, String> configuration = new HashMap<String, String>();
        configuration.put("k1", "v1");
        configuration.put("k2", "v2");
        plugin = new Plugin("plugin-id", new GsonBuilder().create().toJson(configuration));
    }

    @Test
    public void shouldGetAllConfigurationKeys() {
        assertEquals(new HashSet<String>(asList("k1", "k2")), plugin.getAllConfigurationKeys());
    }

    @Test
    public void shouldGetValueForConfigurationKey() throws Exception {
        assertThat(plugin.getConfigurationValue("k1"), is("v1"));
    }
}
