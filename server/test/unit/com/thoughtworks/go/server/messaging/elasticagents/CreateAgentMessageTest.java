package com.thoughtworks.go.server.messaging.elasticagents;

import com.thoughtworks.go.config.JobAgentConfig;
import com.thoughtworks.go.domain.config.ConfigurationKey;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.config.ConfigurationValue;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class CreateAgentMessageTest {
    @Test
    public void shouldGetPluginId() {
        List<ConfigurationProperty> properties = Arrays.asList(new ConfigurationProperty(new ConfigurationKey("key"), new ConfigurationValue("value")));
        JobAgentConfig jobAgentConfig = new JobAgentConfig("plugin-id", properties);
        CreateAgentMessage message = new CreateAgentMessage("key", null, "env", jobAgentConfig);
        assertThat(message.getPluginId(), is(jobAgentConfig.getPluginId()));
        Map<String, String> configurationAsMap = jobAgentConfig.getConfigurationAsMap(true);
        assertThat(message.getConfiguration(), is(configurationAsMap));
    }
}