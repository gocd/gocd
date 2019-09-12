/*
 * Copyright 2019 ThoughtWorks, Inc.
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
package com.thoughtworks.go.config;

import com.rits.cloning.Cloner;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.IpAddress;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.util.SystemUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * @understands the current persistent information related to an Agent
 */
@ConfigTag("agent")
public class AgentConfig implements Validatable {
    @ConfigAttribute(value = "hostname", allowNull = true)
    private String hostName;
    @ConfigAttribute(value = "ipaddress", allowNull = true)
    private String ipAddress;
    @ConfigAttribute(value = "uuid", allowNull = true)
    private String uuid;
    @ConfigAttribute(value = "isDisabled")
    private Boolean isDisabled = false;

    @ConfigAttribute(value = "elasticAgentId", allowNull = true, optional = true)
    private String elasticAgentId;

    @ConfigAttribute(value = "elasticPluginId", allowNull = true, optional = true)
    private String elasticPluginId;

    @ConfigSubtag
    private ResourceConfigs resourceConfigs = new ResourceConfigs();

    private transient Boolean cachedIsFromLocalHost;
    private ConfigErrors errors = new ConfigErrors();
    public static final String IP_ADDRESS = "ipAddress";
    public static final String UUID = "uuid";

    public AgentConfig() {
    }

    public AgentConfig(String uuid) {
        this(uuid, "", "");
    }

    public AgentConfig(String uuid, String hostName, String ipAddress) {
        this(uuid, hostName, ipAddress, new ResourceConfigs());
    }

    public AgentConfig(String uuid, String hostName, String ipAddress, ResourceConfigs resourceConfigs) {
        this.hostName = hostName;
        this.ipAddress = ipAddress;
        this.uuid = uuid;
        this.resourceConfigs = resourceConfigs;
    }

    public boolean validateTree(ValidationContext validationContext) {
        validate(validationContext);
        boolean isValid = errors().isEmpty();
        isValid = resourceConfigs.validateTree(validationContext) && isValid;
        return isValid;
    }
    @Override
    public void validate(ValidationContext validationContext) {
        validateIpAddress();
        if(StringUtils.isBlank(uuid)) {
            addError(UUID, "UUID cannot be empty");
        }
        validateResources();
    }

    private void validateResources() {
        if (isElastic() && !resourceConfigs.isEmpty()) {
            errors.add("elasticAgentId", "Elastic agents cannot have resources.");
        }
    }

    private void validateIpAddress() {
        String address = getIpAddress();
        if (address == null) {
            return;
        }
        if (StringUtils.isBlank(address)) {
            addError(IP_ADDRESS, "IpAddress cannot be empty if it is present.");
            return;
        }
        try {
            IpAddress.create(address);
        } catch (Exception e) {
            addError(IP_ADDRESS, String.format("'%s' is an invalid IP address.", address));
        }
    }

    @Override
    public void addError(String fieldName, String msg) {
        errors.add(fieldName, msg);
    }

    @Override
    public ConfigErrors errors() {
        return errors;
    }

    public boolean hasAllResources(Collection<ResourceConfig> required) {
        return this.resourceConfigs.containsAll(required);
    }

    public ResourceConfigs getResourceConfigs() {
        return resourceConfigs;
    }

    public void addResourceConfig(ResourceConfig resourceConfig) {
        this.resourceConfigs.add(resourceConfig);
    }

    public void removeResource(ResourceConfig resourceConfig) {
        resourceConfigs.remove(resourceConfig);
    }

    public void setResourceConfigs(ResourceConfigs resourceConfigs) {
        this.resourceConfigs = resourceConfigs;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public boolean isDisabled() {
        return Boolean.TRUE.equals(isDisabled);
    }

    public boolean isEnabled() {
        return !isDisabled();
    }

    public void setDisabled(Boolean disabled) {
        isDisabled = disabled;
    }

    public void enable() {
        disable(Boolean.FALSE);
    }

    public void disable() {
        disable(Boolean.TRUE);
    }

    public void disable(Boolean aBoolean) {
        setDisabled(aBoolean);
    }

    public boolean isNull() {
        return false;
    }

    public AgentConfig deepClone() {
        return new Cloner().deepClone(this);
    }

    public boolean isFromLocalHost() {
        if (cachedIsFromLocalHost == null) {
            cachedIsFromLocalHost = SystemUtil.isLocalhost(ipAddress);
        }
        return cachedIsFromLocalHost.booleanValue();
    }

    public String getIpAddress() {
        return this.ipAddress;
    }

    public String getHostname() {
        return this.hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getHostNameForDisplay() {
        return this.hostName;
    }

    public String getUuid() {
        return uuid;
    }

    public String getElasticAgentId() {
        return elasticAgentId;
    }

    public void setElasticAgentId(String elasticAgentId) {
        this.elasticAgentId = elasticAgentId;
    }

    public String getElasticPluginId() {
        return elasticPluginId;
    }

    public void setElasticPluginId(String elasticPluginId) {
        this.elasticPluginId = elasticPluginId;
    }

    public boolean isElastic() {
        return isNotBlank(elasticAgentId) && isNotBlank(elasticPluginId);
    }

    @Deprecated
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String toString() {
        if (isElastic()) {
            return format("ElasticAgent [%s, %s, %s, %s, %s]", hostName, ipAddress, uuid, elasticAgentId, elasticPluginId);
        } else {
            return format("Agent [%s, %s, %s]", hostName, ipAddress, uuid);
        }
    }

    public AgentIdentifier getAgentIdentifier() {
        return new AgentIdentifier(this.getHostname(), getIpAddress(), getUuid());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AgentConfig agentConfig = (AgentConfig) o;

        if (hostName != null ? !hostName.equals(agentConfig.hostName) : agentConfig.hostName != null) {
            return false;
        }
        if (ipAddress != null ? !ipAddress.equals(agentConfig.ipAddress) : agentConfig.ipAddress != null) {
            return false;
        }
        if (uuid != null ? !uuid.equals(agentConfig.uuid) : agentConfig.uuid != null) {
            return false;
        }
        if (isDisabled != null ? !isDisabled.equals(agentConfig.isDisabled) : agentConfig.isDisabled != null) {
            return false;
        }
        if (elasticAgentId != null ? !elasticAgentId.equals(agentConfig.elasticAgentId) : agentConfig.elasticAgentId != null) {
            return false;
        }
        if (elasticPluginId != null ? !elasticPluginId.equals(agentConfig.elasticPluginId) : agentConfig.elasticPluginId != null) {
            return false;
        }
        return resourceConfigs != null ? resourceConfigs.equals(agentConfig.resourceConfigs) : agentConfig.resourceConfigs == null;

    }

    @Override
    public int hashCode() {
        int result = hostName != null ? hostName.hashCode() : 0;
        result = 31 * result + (ipAddress != null ? ipAddress.hashCode() : 0);
        result = 31 * result + (uuid != null ? uuid.hashCode() : 0);
        result = 31 * result + (isDisabled != null ? isDisabled.hashCode() : 0);
        result = 31 * result + (elasticAgentId != null ? elasticAgentId.hashCode() : 0);
        result = 31 * result + (elasticPluginId != null ? elasticPluginId.hashCode() : 0);
        result = 31 * result + (resourceConfigs != null ? resourceConfigs.hashCode() : 0);
        return result;
    }
}
