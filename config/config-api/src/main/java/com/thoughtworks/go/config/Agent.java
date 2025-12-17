/*
 * Copyright Thoughtworks, Inc.
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

import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.IpAddress;
import com.thoughtworks.go.domain.PersistentObject;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.util.ClonerFactory;
import org.jetbrains.annotations.TestOnly;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import static com.thoughtworks.go.config.JobConfig.RESOURCES;
import static com.thoughtworks.go.util.CommaSeparatedString.*;
import static com.thoughtworks.go.util.SystemUtil.isLocalhost;
import static java.lang.String.format;
import static java.util.stream.Collectors.toSet;

/**
 * <code>Agent</code> is the entity object model class used by Hibernate to represent a record in <code>Agents</code> table.
 * This object model is used to perform crud operations on <code>Agents</code> table. Hence please do not remove the <i>default constructor</i>
 * and do not change anything related to <i>id</i> field as those are critical for hibernate to work fine
 */
public class Agent extends PersistentObject {
    public static final String IP_ADDRESS = "ipAddress";
    public static final String UUID = "uuid";
    private static final String DEFAULT_VALUE = "";

    private String hostname;
    private String ipaddress;
    private String uuid;
    private String elasticAgentId;
    private String elasticPluginId;
    private boolean disabled;
    private String environments = DEFAULT_VALUE;
    private String resources = DEFAULT_VALUE;
    private String cookie;
    private boolean deleted;

    private final ConfigErrors errors = new ConfigErrors();
    private transient Boolean cachedIsFromLocalHost;

    public Agent() {
    }

    public Agent(Agent anotherAgent) {
        setUuid(anotherAgent.getUuid());
        setHostname(anotherAgent.getHostname());
        setIpaddress(anotherAgent.getIpaddress());

        setDisabled(anotherAgent.isDisabled());
        setElasticAgentId(anotherAgent.getElasticAgentId());
        setElasticPluginId(anotherAgent.getElasticPluginId());
        setEnvironments(anotherAgent.getEnvironments());

        setResources(anotherAgent.getResources());

        setId(anotherAgent.getId());
        setDeleted(anotherAgent.isDeleted());

        if (anotherAgent.getCookie() != null) {
            setCookie(anotherAgent.getCookie());
        }
    }

    public Agent(String uuid) {
        this(uuid, "", "");
    }

    public Agent(String uuid, String hostname, String ipaddress) {
        this.hostname = hostname;
        this.ipaddress = ipaddress;
        this.uuid = uuid;
    }

    @TestOnly
    public Agent(String uuid, String hostname, String ipaddress, List<String> resources) {
        this(uuid, hostname, ipaddress);
        setResourcesFrom(resources);
    }

    public Agent(String uuid, String hostname, String ipaddress, String cookie) {
        this(uuid, hostname, ipaddress);
        this.cookie = cookie;
    }

    public static Agent blankAgent(String uuid) {
        return new Agent(uuid, "Unknown", "Unknown", "Unknown");
    }


    public void validate() {
        validateIpAddress();
        if (uuid == null || uuid.isBlank()) {
            addError(UUID, "UUID cannot be empty");
        }
        validateResources();
    }

    public boolean cookieAssigned() {
        return cookie != null;
    }

    private void validateResources() {
        ResourceConfigs resourceConfigs = new ResourceConfigs(getResources());
        if (isElastic() && !resourceConfigs.isEmpty()) {
            errors.add(RESOURCES, "Elastic agents cannot have resources.");
            return;
        }

        resourceConfigs.validate(null);
        if (resourceConfigs.hasErrors()) {
            errors.addAll(resourceConfigs.errors());
        }
    }

    private void validateIpAddress() {
        String ipAddress = getIpaddress();

        if (ipAddress == null) {
            return;
        }

        if (ipAddress.isBlank()) {
            addError(IP_ADDRESS, "IpAddress cannot be empty if it is present.");
            return;
        }

        try {
            IpAddress.create(ipAddress);
        } catch (Exception e) {
            addError(IP_ADDRESS, format("'%s' is an invalid IP address.", ipAddress));
        }
    }

    public void addError(String fieldName, String msg) {
        errors.add(fieldName, msg);
    }

    public ConfigErrors errors() {
        return errors;
    }

    public List<ConfigErrors> errorsAsList() {
        return List.of(errors);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public boolean hasAllResources(Collection<String> requiredResources) {
        Set<String> agentResources = this.getResourcesAsStream().map(String::toLowerCase).collect(toSet());
        return requiredResources.stream().map(String::toLowerCase).allMatch(agentResources::contains);
    }

    public void removeResources(List<String> resourcesToRemove) {
        if (resourcesToRemove != null && !resourcesToRemove.isEmpty()) {
            this.resources = remove(this.resources, resourcesToRemove);
        }
    }

    public void addResource(String resource) {
        if (resource != null && !resource.isBlank()) {
            addResources(List.of(resource));
        }
    }

    public void addResources(List<String> resourcesToAdd) {
        if (resourcesToAdd != null && !resourcesToAdd.isEmpty()) {
            this.resources = append(this.resources, resourcesToAdd);
        }
    }

    public void setResources(String commaSeparatedResources) {
        this.resources = normalizeToNull(commaSeparatedResources);
    }

    @TestOnly
    public void setResourcesFrom(List<String> resourceList) {
        this.resources = append("", resourceList);
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
        return ClonerFactory.instance().deepClone(this);
    }

    public boolean isFromLocalHost() {
        if (cachedIsFromLocalHost == null) {
            cachedIsFromLocalHost = isLocalhost(ipaddress);
        }
        return cachedIsFromLocalHost;
    }

    public boolean isElastic() {
        return elasticAgentId != null && !elasticAgentId.isBlank() && elasticPluginId != null && !elasticPluginId.isBlank();
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
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
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

    public Stream<String> getEnvironmentsAsStream() {
        return commaSeparatedStrToTrimmed(environments);
    }

    public void setEnvironments(String commaSeparatedEnvs) {
        this.environments = normalizeToNull(commaSeparatedEnvs);
    }

    @TestOnly
    public void setEnvironmentsFrom(List<String> envList) {
        this.environments = append(null, envList);
    }

    public void addEnvironment(String env) {
        this.addEnvironments(env == null || env.isBlank() ? null : List.of(env));
    }

    public void addEnvironments(List<String> envsToAdd) {
        this.environments = append(this.environments, envsToAdd);
    }

    public void removeEnvironments(List<String> envsToRemove) {
        this.environments = remove(this.environments, envsToRemove);
    }

    public void removeEnvironment(String env) {
        this.environments = remove(this.environments, env == null ? null : List.of(env));
    }

    public String getResources() {
        return resources;
    }

    public String getResourcesNormalized() {
        return normalizeToNull(resources);
    }

    public Stream<String> getResourcesAsStream() {
        return commaSeparatedStrToTrimmed(this.resources);
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

    public boolean isDeleted() {
        return deleted;
    }
}
