/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
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
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.config;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import org.apache.log4j.Logger;

public class AgentRegistrationPropertiesReader {

    private static final Logger LOG = Logger.getLogger(AgentRegistrationPropertiesReader.class);
    public static final String AGENT_AUTO_REGISTER_KEY = "agent.auto.register.key";
    public static final String AGENT_AUTO_REGISTER_RESOURCES = "agent.auto.register.resources";
    public static final String AGENT_AUTO_REGISTER_ENVIRONMENTS = "agent.auto.register.environments";

    private Properties agentAutoRegisterProperties;

    public AgentRegistrationPropertiesReader(File propertiesFile) {
        agentAutoRegisterProperties = new Properties();
        loadProperties(propertiesFile);
    }

    public AgentRegistrationPropertiesReader(Properties properties) {
        this.agentAutoRegisterProperties = properties;
    }

    private void loadProperties(File propertiesFile) {
        FileInputStream inStream = null;
        try {
            inStream = new FileInputStream(propertiesFile);
            agentAutoRegisterProperties.load(inStream);
            LOG.info(String.format("[Agent Auto Registration] Loaded agent auto register properties file."));
            inStream.close();
        }
        catch (Exception e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("[Agent Auto Registration] Unable to load agent auto register properties file. This agent will not auto-register");
            }
        }
    }

    public String getAgentAutoRegisterKey() {
        return agentAutoRegisterProperties.getProperty(AGENT_AUTO_REGISTER_KEY, "");
    }

    public String getAgentAutoRegisterResources() {
        return agentAutoRegisterProperties.getProperty(AGENT_AUTO_REGISTER_RESOURCES, "");
    }

    public String getAgentAutoRegisterEnvironments() {
        return agentAutoRegisterProperties.getProperty(AGENT_AUTO_REGISTER_ENVIRONMENTS, "");
    }

}
