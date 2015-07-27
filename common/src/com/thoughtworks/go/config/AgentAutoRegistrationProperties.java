/*
 * Copyright 2015 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.thoughtworks.go.config;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.PropertiesConfigurationLayout;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.Properties;

public class AgentAutoRegistrationProperties {
    private static final Logger LOG = Logger.getLogger(AgentAutoRegistrationProperties.class);

    public static final String AGENT_AUTO_REGISTER_KEY = "agent.auto.register.key";
    public static final String AGENT_AUTO_REGISTER_RESOURCES = "agent.auto.register.resources";
    public static final String AGENT_AUTO_REGISTER_ENVIRONMENTS = "agent.auto.register.environments";
    public static final String AGENT_AUTO_REGISTER_HOSTNAME = "agent.auto.register.hostname";

    private final File configFile;
    private final Properties properties;

    public AgentAutoRegistrationProperties(File config) {
        this(config, new Properties());
    }

    AgentAutoRegistrationProperties(File config, Properties properties) {
        this.configFile = config;
        this.properties = properties;
    }

    public boolean exist() {
        return configFile.exists();
    }

    public String agentAutoRegisterKey() {
        return getProperty(AGENT_AUTO_REGISTER_KEY);
    }

    public String agentAutoRegisterResources() {
        return getProperty(AGENT_AUTO_REGISTER_RESOURCES);
    }

    public String agentAutoRegisterEnvironments() {
        return getProperty(AGENT_AUTO_REGISTER_ENVIRONMENTS);
    }

    public String agentAutoRegisterHostname() {
        return getProperty(AGENT_AUTO_REGISTER_HOSTNAME);
    }

    public void scrubRegistrationProperties() {
        if (!exist()) {
            return;
        }
        try {
            PropertiesConfiguration config = new PropertiesConfiguration();
            config.setIOFactory(new FilteringOutputWriterFactory());
            PropertiesConfigurationLayout layout = new PropertiesConfigurationLayout(config);
            layout.setLineSeparator("\n");
            layout.load(reader());
            layout.save(new FileWriter(this.configFile));
            loadProperties();
        } catch (ConfigurationException | IOException e) {
            LOG.warn("[Agent Auto Registration] Unable to scrub registration key.", e);
        }
    }

    private String getProperty(String property) {
        return properties().getProperty(property, "");
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
        return new StringReader(FileUtils.readFileToString(configFile));
    }

    private class FilteringOutputWriterFactory extends PropertiesConfiguration.DefaultIOFactory {
        class FilteringPropertiesWriter extends PropertiesConfiguration.PropertiesWriter {
            public FilteringPropertiesWriter(Writer out, char delimiter) {
                super(out, delimiter);
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
        public PropertiesConfiguration.PropertiesWriter createPropertiesWriter(Writer out, char delimiter) {
            return new FilteringPropertiesWriter(out, delimiter);
        }
    }
}
