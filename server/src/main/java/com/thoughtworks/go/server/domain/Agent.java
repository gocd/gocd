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
package com.thoughtworks.go.server.domain;

import com.thoughtworks.go.config.AgentConfig;
import com.thoughtworks.go.config.ResourceConfigs;
import com.thoughtworks.go.config.Validatable;
import com.thoughtworks.go.config.ValidationContext;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.IpAddress;
import com.thoughtworks.go.domain.PersistentObject;
import com.thoughtworks.go.remote.AgentIdentifier;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Getter
@Setter
public class Agent extends PersistentObject implements Validatable {
    private String uuid;
    private String cookie;
    private String hostname;
    private String ipaddress;
    private String elasticAgentId;
    private String elasticPluginId;
    private boolean disabled;
    private String environments;
    private String resources;
    private boolean deleted;

    private ConfigErrors errors = new ConfigErrors();
    public static final String IP_ADDRESS = "ipaddress";
    public static final String UUID = "uuid";

    public Agent(String uuid, String cookie, String hostname, String ipaddress) {
        this.uuid = uuid;
        this.cookie = cookie;
        this.hostname = hostname;
        this.ipaddress = ipaddress;
    }

    public static Agent blankAgent(String uuid) {
        return new Agent(uuid, "Unknown", "Unknown", "Unknown");
    }

    public static Agent fromConfig(AgentConfig config) {
        Agent agent = new Agent(config.getUuid(), null, config.getHostname(), config.getIpAddress());
        agent.setDisabled(config.isDisabled());
        agent.setElasticAgentId(config.getElasticAgentId());
        agent.setElasticPluginId(config.getElasticPluginId());
        agent.setEnvironments(config.getEnvironments());
        agent.setResources(config.getResourceConfigs());
        agent.setId(config.getId());
        if (config.getCookie() != null) {
            agent.setCookie(config.getCookie());
        }

        return agent;
    }

    @Deprecated //for hibernate
    private Agent() {
    }

    public void update(String cookie, String hostname, String ipaddress) {
        this.setCookie(cookie);
        this.setHostname(hostname);
        this.setIpaddress(ipaddress);
    }

    public AgentIdentifier getAgentIdentifier() {
        return new AgentIdentifier(this.getHostname(), getIpaddress(), getUuid());
    }

    public boolean isElastic() {
        return isNotBlank(elasticAgentId) && isNotBlank(elasticPluginId);
    }

    public void addResources(List<String> resourcesToAdd) {
        LinkedHashSet<String> resources = new LinkedHashSet<>();
        if (getResources() != null) {
            resources.addAll(getResources().stream().map(resource -> resource.getName()).collect(Collectors.toList()));
        }
        resources.addAll(resourcesToAdd);
        setResources(new ResourceConfigs(String.join(",", resources)));
    }

    public void removeResources(List<String> resourcesToRemove) {
        if (getResources() != null) {
            List<String> resources = getResources()
                    .stream()
                    .map(resource -> resource.getName())
                    .filter(resource -> !resourcesToRemove.contains(resource))
                    .collect(Collectors.toList());

            setResources(new ResourceConfigs(String.join(",", resources)));
        }
    }

    public void addEnvironments(List<String> environmentsToAdd) {
        LinkedHashSet<String> environments = new LinkedHashSet<>();
        if (this.getEnvironments() != null) {
            environments.addAll(Arrays.asList(this.getEnvironments().split(",")));
        }
        environments.addAll(environmentsToAdd);
        this.setEnvironments(String.join(",", environments));
    }

    public void removeEnvironments(List<String> environmentsToRemove) {
        if (this.getEnvironments() != null) {
            List<String> environments = Arrays.stream(this.getEnvironments().split(","))
                    .filter(environment -> !environmentsToRemove.contains(environment))
                    .collect(Collectors.toList());

            this.setEnvironments(String.join(",", environments));
        }
    }

    public ResourceConfigs getResources() {
        return new ResourceConfigs(resources == null ? "" : resources);
    }

    public void setResources(ResourceConfigs resourceConfigs) {
        this.resources = StringUtils.join(resourceConfigs.resourceNames(), ",");
    }

    public void validate(ValidationContext validationContext) {
        validateIpAddress();
        if (StringUtils.isBlank(uuid)) {
            addError(UUID, "UUID cannot be empty");
        }
        validateResources();
    }

    private void validateResources() {
        if (isElastic() && !resources.isEmpty()) {
            errors.add("elasticAgentId", "Elastic agents cannot have resources.");
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

    public void addError(String fieldName, String msg) {
        errors.add(fieldName, msg);
    }

    public ConfigErrors errors() {
        return errors;
    }

    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }
}
