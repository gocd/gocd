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

import static com.thoughtworks.go.util.CommaSeparatedString.append;
import static com.thoughtworks.go.util.CommaSeparatedString.remove;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * @understands the current persistent information related to an Agent
 */
public class Agent extends PersistentObject implements Validatable {
    private String hostname;
    private String ipaddress;
    private String uuid;
    private String elasticAgentId;
    private String elasticPluginId;
    private boolean disabled;
    private String environments;
    private String resources;
    private String cookie;
    private boolean deleted;

    private transient Boolean cachedIsFromLocalHost;
    private ConfigErrors errors = new ConfigErrors();
    public static final String IP_ADDRESS = "ipAddress";
    public static final String UUID = "uuid";

    public Agent() {
    }

    public static Agent newInstanceFrom(Agent original) {
        Agent copy = new Agent(original.getUuid(), original.getHostname(), original.getIpaddress(), original.getCookie());

        copy.setDisabled(original.isDisabled());
        copy.setElasticAgentId(original.getElasticAgentId());
        copy.setElasticPluginId(original.getElasticPluginId());
        copy.setEnvironments(original.getEnvironments());
        copy.setResources(original.getResourceConfigs());
        copy.setId(original.getId());

        if (original.getCookie() != null) {
            copy.setCookie(original.getCookie());
        }

        return copy;
    }

    public Agent(String uuid) {
        this(uuid, "", "");
    }

    public Agent(String uuid, String hostname, String ipaddress) {
        this(uuid, hostname, ipaddress, new ResourceConfigs());
    }

    public Agent(String uuid, String hostname, String ipaddress, ResourceConfigs resourceConfigs) {
        this.hostname = hostname;
        this.ipaddress = ipaddress;
        this.uuid = uuid;
        this.resources = StringUtils.join(resourceConfigs.resourceNames(), ",");
    }

    public Agent(String uuid, String hostname, String ipaddress, String cookie) {
        this(uuid, hostname, ipaddress);
        this.cookie = cookie;
    }

    public static Agent blankAgent(String uuid) {
        return new Agent(uuid, "Unknown", "Unknown", "Unknown");
    }

    public void setFieldValues(String cookie, String hostname, String ipaddress) {
        this.setCookie(cookie);
        this.setHostname(hostname);
        this.setIpaddress(ipaddress);
    }

    public void addResources(List<String> resourcesToAdd) {
        String commaSeparatedResourceNames = getResourceConfigs().getCommaSeparatedResourceNames();
        setResources(new ResourceConfigs(append(commaSeparatedResourceNames, resourcesToAdd)));
    }

    public void removeResources(List<String> resourcesToRemove) {
        String commaSeparatedResourceNames = getResourceConfigs().getCommaSeparatedResourceNames();
        setResources(new ResourceConfigs(remove(commaSeparatedResourceNames, resourcesToRemove)));
    }

    public void removeEnvironments(List<String> envsToRemove) {
        this.setEnvironments(remove(this.getEnvironments(), envsToRemove));
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
        ResourceConfigs resourceConfigs = getResources();
        if (isElastic() && !resourceConfigs.isEmpty()) {
            errors.add("elasticAgentId", "Elastic agents cannot have resources.");
        } else {
            resourceConfigs.validate(null);
            if (!resourceConfigs.errors().isEmpty()) {
                errors.addAll(resourceConfigs.errors());
            }
        }
    }

    private void validateIpAddress() {
        String address = getIpaddress();
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

    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }

    public boolean hasAllResources(Collection<ResourceConfig> required) {
        return this.getResources().containsAll(required);
    }

    public ResourceConfigs getResourceConfigs() {
        return new ResourceConfigs(resources);
    }

    public void addResourceConfig(ResourceConfig resourceConfig) {
        LinkedHashSet<String> resourcesList = new LinkedHashSet<>();
        if (this.getEnvironments() != null) {
            resourcesList.addAll(Arrays.asList(this.resources.split(",")));
        }
        resourcesList.add(resourceConfig.getName());
        this.setEnvironments(String.join(",", environments));
        this.resources = String.join(",", resourcesList);
    }

    public void removeResource(ResourceConfig resourceConfig) {
        this.getResources().remove(resourceConfig);
    }

    public void setResourceConfigs(ResourceConfigs resourceConfigs) {
        resources = resourceConfigs.getCommaSeparatedResourceNames();
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

    public Agent deepClone() {
        return new Cloner().deepClone(this);
    }

    public boolean isFromLocalHost() {
        if (cachedIsFromLocalHost == null) {
            cachedIsFromLocalHost = SystemUtil.isLocalhost(ipaddress);
        }
        return cachedIsFromLocalHost;
    }

    public boolean isElastic() {
        return isNotBlank(elasticAgentId) && isNotBlank(elasticPluginId);
    }

    @Override
    public String toString() {
        if (isElastic()) {
            return format("ElasticAgent [%s, %s, %s, %s, %s]", hostname, ipaddress, uuid, elasticAgentId, elasticPluginId);
        } else {
            return format("Agent [%s, %s, %s]", hostname, ipaddress, uuid);
        }
    }

    public AgentIdentifier getAgentIdentifier() {
        return new AgentIdentifier(this.getHostname(), getIpaddress(), getUuid());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Agent that = (Agent) o;
        return disabled == that.disabled &&
                Objects.equals(hostname, that.hostname) &&
                Objects.equals(ipaddress, that.ipaddress) &&
                Objects.equals(uuid, that.uuid) &&
                Objects.equals(elasticAgentId, that.elasticAgentId) &&
                Objects.equals(elasticPluginId, that.elasticPluginId) &&
                Objects.equals(environments, that.environments) &&
                Objects.equals(resources, that.resources) &&
                Objects.equals(cookie, that.cookie);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hostname, ipaddress, uuid, elasticAgentId, elasticPluginId, disabled, environments, resources, cookie);
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getIpaddress() {
        return ipaddress;
    }

    public void setIpaddress(String ipaddress) {
        this.ipaddress = ipaddress;
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

    public List<String> getEnvironmentsAsList() {
        return (this.environments == null ? new ArrayList<>() : Arrays.asList(this.environments.split(",")));
    }

    public void setEnvironments(String envs) {
        this.environments = envs;
    }

    public void addEnvironments(List<String> envsToAdd) {
        this.setEnvironments(append(this.getEnvironments(), envsToAdd));
    }

    public void addEnvironment(String env){
        this.addEnvironments(singletonList(env));
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

    public String getHostnameForDisplay() {
        return this.hostname;
    }
}
