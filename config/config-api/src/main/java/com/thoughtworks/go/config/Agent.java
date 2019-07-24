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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static com.thoughtworks.go.util.CommaSeparatedString.append;
import static com.thoughtworks.go.util.CommaSeparatedString.remove;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.*;
import static org.springframework.util.CollectionUtils.isEmpty;

/**
 * <code>Agent</code> is the entity object model class used by Hibernate to represent a record in <code>Agents</code> table.
 * This object model is used to perform crud operations on <code>Agents</code> table. Hence please do not remove the <i>default constructor</i>
 * and do not change anything related to <i>id</i> field as those are critical for hibernate to work fine
 */
public class Agent extends PersistentObject {
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

    public Agent(Agent anotherAgent) {
        setUuid(anotherAgent.getUuid());
        setHostname(anotherAgent.getHostname());
        setIpaddress(anotherAgent.getIpaddress());

        setDisabled(anotherAgent.isDisabled());
        setElasticAgentId(anotherAgent.getElasticAgentId());
        setElasticPluginId(anotherAgent.getElasticPluginId());
        setEnvironments(anotherAgent.getEnvironments());

        ResourceConfigs anotherAgentResourceConfigs = anotherAgent.getResourceConfigs();
        if (!isEmpty(anotherAgentResourceConfigs)) {
            setResources(anotherAgentResourceConfigs.getCommaSeparatedResourceNames());
        }

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
        this(uuid, hostname, ipaddress, new ResourceConfigs());
    }

    public Agent(String uuid, String hostname, String ipaddress, ResourceConfigs resourceConfigs) {
        this.hostname = hostname;
        this.ipaddress = ipaddress;
        this.uuid = uuid;
        this.resources = join(resourceConfigs.resourceNames(), ",");
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

    public void removeEnvironments(List<String> envsToRemove) {
        this.setEnvironments(remove(this.getEnvironments(), envsToRemove));
    }

    public void validate() {
        validateIpAddress();
        if (isBlank(uuid)) {
            addError(UUID, "UUID cannot be empty");
        }
        validateResources();
    }

    private void validateResources() {
        ResourceConfigs resourceConfigs = getResources();
        if (isElastic() && !resourceConfigs.isEmpty()) {
            errors.add("elasticAgentId", "Elastic agents cannot have resources.");
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

        if (isBlank(ipAddress)) {
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
        return asList(errors);
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

    public void removeResources(List<String> resourcesToRemove) {
        if (!isEmpty(resourcesToRemove)) {
            String resourcesBeforeAdd = getResourceConfigs().getCommaSeparatedResourceNames();
            String resourcesAfterAdd = remove(resourcesBeforeAdd, resourcesToRemove);
            setCommaSeparatedResourceNames(resourcesAfterAdd);
        }
    }

    public void addResource(String resource) {
        if (isNotBlank(resource)) {
            addResources(singletonList(resource));
        }
    }

    public void addResources(List<String> resourcesToAdd) {
        if (!isEmpty(resourcesToAdd)) {
            String resourcesBeforeAdd = getResourceConfigs().getCommaSeparatedResourceNames();
            String resourcesAfterAdd = append(resourcesBeforeAdd, resourcesToAdd);
            setCommaSeparatedResourceNames(resourcesAfterAdd);
        }
    }

    public void setResources(String commaSeparatedResources) {
        setCommaSeparatedResourceNames(commaSeparatedResources);
    }

    public void setResourcesFromList(List<String> resourceList) {
        String resourceNames = append(null, resourceList);
        setCommaSeparatedResourceNames(resourceNames);
    }

    private void setCommaSeparatedResourceNames(String resourceNames) {
        if (isBlank(resourceNames)) {
            this.resources = null;
            return;
        }

        this.resources = new ResourceConfigs(resourceNames).getCommaSeparatedResourceNames();
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
        return (this.environments == null ? new ArrayList<>() : asList(this.environments.split(",")));
    }

    public void setEnvironments(String envs) {
        this.environments = envs;
    }

    public void addEnvironments(List<String> envsToAdd) {
        this.setEnvironments(append(this.getEnvironments(), envsToAdd));
    }

    public void addEnvironment(String env) {
        this.addEnvironments(singletonList(env));
    }

    public ResourceConfigs getResources() {
        return new ResourceConfigs(resources == null ? "" : resources);
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
