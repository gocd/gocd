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
import com.thoughtworks.go.domain.PersistentObject;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.util.SystemUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * @understands the current persistent information related to an Agent
 */
@ConfigTag("agent")
public class AgentConfig extends PersistentObject implements Validatable {
    @ConfigAttribute(value = "hostname", allowNull = true)
    private String hostName;
    @ConfigAttribute(value = "ipaddress", allowNull = true)
    private String ipAddress;
    @ConfigAttribute(value = "uuid", allowNull = true)
    private String uuid;

    @ConfigAttribute(value = "elasticAgentId", allowNull = true, optional = true)
    private String elasticAgentId;

    @ConfigAttribute(value = "elasticPluginId", allowNull = true, optional = true)
    private String elasticPluginId;

    private boolean disabled;
    private String environments;
    private String resources;
    private String cookie;
    private boolean deleted;

//    @ConfigSubtag
//    private ResourceConfigs resourceConfigs = new ResourceConfigs();

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
        this.resources = StringUtils.join(resourceConfigs.resourceNames(), ",");
    }

    public AgentConfig(String uuid, String hostName, String ipAddress, String cookie) {
        this(uuid, hostName, ipAddress);
        this.cookie = cookie;
    }

    public boolean validateTree(ValidationContext validationContext) {
        validate(validationContext);
        boolean isValid = errors().isEmpty();
        isValid = getResources().validateTree(validationContext) && isValid;
        return isValid;
    }
    @Override
    public void validate(ValidationContext validationContext) {
        validateIpAddress();
        if (StringUtils.isBlank(uuid)) {
            addError(UUID, "UUID cannot be empty");
        }
        validateResources();
    }

    private void validateResources() {
        if (isElastic() && !getResources().isEmpty()) {
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
        return this.getResources().containsAll(required);
    }

    public ResourceConfigs getResourceConfigs() {
        return new ResourceConfigs(resources);
    }

    public void addResourceConfig(ResourceConfig resourceConfig) {
        this.getResources().add(resourceConfig);
    }

    public void removeResource(ResourceConfig resourceConfig) {
        this.getResources().remove(resourceConfig);
    }

    public void setResourceConfigs(ResourceConfigs resourceConfigs) {
        resources = resourceConfigs.toString();
    }

    public boolean isEnabled() {
        return !isDisabled();
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

    public boolean isElastic() {
        return isNotBlank(elasticAgentId) && isNotBlank(elasticPluginId);
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
        return new AgentIdentifier(this.getHostName(), getIpAddress(), getUuid());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AgentConfig that = (AgentConfig) o;
        return disabled == that.disabled &&
                Objects.equals(hostName, that.hostName) &&
                Objects.equals(ipAddress, that.ipAddress) &&
                Objects.equals(uuid, that.uuid) &&
                Objects.equals(elasticAgentId, that.elasticAgentId) &&
                Objects.equals(elasticPluginId, that.elasticPluginId) &&
                Objects.equals(environments, that.environments) &&
                Objects.equals(resources, that.resources) &&
                Objects.equals(cookie, that.cookie);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hostName, ipAddress, uuid, elasticAgentId, elasticPluginId, disabled, environments, resources, cookie);
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
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

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public String getEnvironments() {
        return environments;
    }

    public List<String> getEnvironmentsAsList(){
        return (this.environments == null ? new ArrayList<>() : Arrays.asList(this.environments.split(",")));
    }

    public void setEnvironments(String environments) {
        this.environments = environments;
    }

    public void addEnvironments(List<String> environmentsToAdd) {
        LinkedHashSet<String> environments = new LinkedHashSet<>();
        if (this.getEnvironments() != null) {
            environments.addAll(Arrays.asList(this.getEnvironments().split(",")));
        }
        environments.addAll(environmentsToAdd);
        this.setEnvironments(String.join(",", environments));
    }

    public ResourceConfigs getResources() {
        return new ResourceConfigs(resources == null ? "" : resources);
    }

    public void setResources(ResourceConfigs resourceConfigs) {
        this.resources = StringUtils.join(resourceConfigs.resourceNames(), ",");
    }

    public String getCookie() {
        return cookie;
    }

    public void setCookie(String cookie) {
        this.cookie = cookie;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
}
