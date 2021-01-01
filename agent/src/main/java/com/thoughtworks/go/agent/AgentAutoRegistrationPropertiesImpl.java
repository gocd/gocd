/*
 * Copyright 2021 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.thoughtworks.go.agent;

import com.thoughtworks.go.config.AgentAutoRegistrationProperties;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.PropertiesConfigurationLayout;
import org.apache.commons.configuration2.convert.ListDelimiterHandler;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Properties;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class AgentAutoRegistrationPropertiesImpl implements AgentAutoRegistrationProperties {
    private static final Logger LOG = LoggerFactory.getLogger(AgentAutoRegistrationPropertiesImpl.class);

    public static final String AGENT_AUTO_REGISTER_KEY = "agent.auto.register.key";
    public static final String AGENT_AUTO_REGISTER_RESOURCES = "agent.auto.register.resources";
    public static final String AGENT_AUTO_REGISTER_ENVIRONMENTS = "agent.auto.register.environments";
    public static final String AGENT_AUTO_REGISTER_HOSTNAME = "agent.auto.register.hostname";
    public static final String AGENT_AUTO_REGISTER_ELASTIC_PLUGIN_ID = "agent.auto.register.elasticAgent.pluginId";
    public static final String AGENT_AUTO_REGISTER_ELASTIC_AGENT_ID = "agent.auto.register.elasticAgent.agentId";

    private final File configFile;
    private final Properties properties;

    public AgentAutoRegistrationPropertiesImpl(File config) {
        this(config, new Properties());
    }

    public AgentAutoRegistrationPropertiesImpl(File config, Properties properties) {
        this.configFile = config;
        this.properties = properties;
    }

    private boolean exist() {
        return configFile.exists();
    }

    @Override
    public boolean isElastic() {
        return exist() && !isBlank(agentAutoRegisterElasticPluginId());
    }

    @Override
    public String agentAutoRegisterKey() {
        return getProperty(AGENT_AUTO_REGISTER_KEY, "");
    }

    @Override
    public String agentAutoRegisterResources() {
        return getProperty(AGENT_AUTO_REGISTER_RESOURCES, "");
    }

    @Override
    public String agentAutoRegisterEnvironments() {
        return getProperty(AGENT_AUTO_REGISTER_ENVIRONMENTS, "");
    }

    @Override
    public String agentAutoRegisterElasticPluginId() {
        return getProperty(AGENT_AUTO_REGISTER_ELASTIC_PLUGIN_ID, "");
    }

    @Override
    public String agentAutoRegisterElasticAgentId() {
        return getProperty(AGENT_AUTO_REGISTER_ELASTIC_AGENT_ID, "");
    }

    @Override
    public String agentAutoRegisterHostname() {
        return getProperty(AGENT_AUTO_REGISTER_HOSTNAME, "");
    }

    @Override
    public void scrubRegistrationProperties() {
        if (!exist()) {
            return;
        }
        try {
            PropertiesConfiguration config = new PropertiesConfiguration();
            config.setIOFactory(new FilteringOutputWriterFactory());
            PropertiesConfigurationLayout layout = new PropertiesConfigurationLayout();
            layout.setLineSeparator("\n");
            layout.load(config, reader());
            try (FileWriter out = new FileWriter(this.configFile)) {
                layout.save(config, out);
            }
            loadProperties();
        } catch (ConfigurationException | IOException e) {
            LOG.warn("[Agent Auto Registration] Unable to scrub registration key.", e);
        }
    }

    private String getProperty(String property, String defaultValue) {
        return properties().getProperty(property, defaultValue);
    }

    private Properties properties() {
        if (this.properties.isEmpty()) {
            loadProperties();
        }
        return this.properties;
    }

    private void loadProperties() {
        try {
            this.properties.clear();
            this.properties.load(reader());
        } catch (IOException e) {
            LOG.debug("[Agent Auto Registration] Unable to load agent auto register properties file. This agent will not auto-register.", e);
        }
    }

    private StringReader reader() throws IOException {
        return new StringReader(FileUtils.readFileToString(configFile, UTF_8));
    }

    private class FilteringOutputWriterFactory extends PropertiesConfiguration.DefaultIOFactory {
        class FilteringPropertiesWriter extends PropertiesConfiguration.PropertiesWriter {
            FilteringPropertiesWriter(Writer writer, ListDelimiterHandler delHandler) {
                super(writer, delHandler);
            }

            @Override
            public void writeProperty(String key, Object value, boolean forceSingleLine) throws IOException {
                if (key.equals(AGENT_AUTO_REGISTER_KEY)) {
                    scrubWithMessage("The autoregister key has been intentionally removed by Go as a security measure.");
                }

                if (key.equals(AGENT_AUTO_REGISTER_RESOURCES) || key.equals(AGENT_AUTO_REGISTER_ENVIRONMENTS) || key.equals(AGENT_AUTO_REGISTER_HOSTNAME)) {
                    scrubWithMessage("This property has been removed by Go after attempting to auto-register with the Go server.");
                }
                super.writeProperty(key, value, forceSingleLine);
            }

            private void scrubWithMessage(String message) throws IOException {
                write("# ");
                writeln(message);
                write("# ");
            }
        }

        @Override
        public PropertiesConfiguration.PropertiesWriter createPropertiesWriter(Writer out, ListDelimiterHandler handler) {
            return new FilteringPropertiesWriter(out, handler);
        }

    }
}
