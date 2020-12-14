/*
 * Copyright 2020 ThoughtWorks, Inc.
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

import com.google.gson.*;
import com.thoughtworks.go.domain.MaterialInstance;
import com.thoughtworks.go.domain.builder.pluggableTask.PluggableTaskBuilder;
import com.thoughtworks.go.domain.materials.dependency.DependencyMaterialInstance;
import com.thoughtworks.go.domain.materials.git.GitMaterialInstance;
import com.thoughtworks.go.domain.materials.mercurial.HgMaterialInstance;
import com.thoughtworks.go.domain.materials.packagematerial.PackageMaterialInstance;
import com.thoughtworks.go.domain.materials.perforce.P4MaterialInstance;
import com.thoughtworks.go.domain.materials.scm.PluggableSCMMaterialInstance;
import com.thoughtworks.go.domain.materials.svn.SvnMaterialInstance;
import com.thoughtworks.go.remote.adapter.RuntimeTypeAdapterFactory;
import com.thoughtworks.go.config.materials.PackageMaterial;
import com.thoughtworks.go.config.materials.PluggableSCMMaterial;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.config.materials.mercurial.HgMaterial;
import com.thoughtworks.go.config.materials.perforce.P4Material;
import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.JobResult;
import com.thoughtworks.go.domain.JobState;
import com.thoughtworks.go.domain.builder.*;
import com.thoughtworks.go.domain.config.ConfigurationKey;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.config.ConfigurationValue;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.remote.AgentInstruction;
import com.thoughtworks.go.remote.BuildRepositoryRemote;
import com.thoughtworks.go.remote.request.GetCookieRequest;
import com.thoughtworks.go.remote.request.ReportCompleteStatusRequest;
import com.thoughtworks.go.remote.request.ReportCurrentStatusRequest;
import com.thoughtworks.go.remote.work.*;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.server.service.ElasticAgentRuntimeInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;

import static java.lang.Boolean.parseBoolean;

@Component
public class NewRemote implements BuildRepositoryRemote {
    private BuildRepositoryRemote buildRepositoryRemote;
    private GoHttpClientHttpInvokerRequestExecutor httpClientHttpInvokerRequestExecutor;
    private final Gson gson;

    @Autowired
    public NewRemote(BuildRepositoryRemote buildRepositoryRemote, GoHttpClientHttpInvokerRequestExecutor httpClientHttpInvokerRequestExecutor) {
        this.buildRepositoryRemote = buildRepositoryRemote;
        this.httpClientHttpInvokerRequestExecutor = httpClientHttpInvokerRequestExecutor;
        gson = new GsonBuilder()
                .registerTypeAdapter(ConfigurationProperty.class, new ConfigurationPropertyAdapter())
                .registerTypeAdapterFactory(builderAdapter())
                .registerTypeAdapterFactory(materialAdapter())
                .registerTypeAdapterFactory(workAdapter())
                .registerTypeAdapterFactory(materialInstanceAdapter())
                .registerTypeAdapterFactory(agentRuntimeInfoAdapter())
                .create();
    }

    @Override
    public AgentInstruction ping(AgentRuntimeInfo info) {
        try {
            String instruction = this.httpClientHttpInvokerRequestExecutor.doPost("ping", gson.toJson(info, AgentRuntimeInfo.class));
            return gson.fromJson(instruction, AgentInstruction.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Work getWork(AgentRuntimeInfo runtimeInfo) {
        try {
            String work = this.httpClientHttpInvokerRequestExecutor.doPost("get_work", gson.toJson(runtimeInfo, AgentRuntimeInfo.class));
            return gson.fromJson(work, Work.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void reportCurrentStatus(AgentRuntimeInfo agentRuntimeInfo, JobIdentifier jobIdentifier, JobState jobState) {
        try {
            this.httpClientHttpInvokerRequestExecutor.doPost("report_current_status",
                    gson.toJson(new ReportCurrentStatusRequest(agentRuntimeInfo, jobIdentifier, jobState)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void reportCompleting(AgentRuntimeInfo agentRuntimeInfo, JobIdentifier jobIdentifier, JobResult result) {
        try {
            this.httpClientHttpInvokerRequestExecutor.doPost("report_completing",
                    gson.toJson(new ReportCompleteStatusRequest(agentRuntimeInfo, jobIdentifier, result)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void reportCompleted(AgentRuntimeInfo agentRuntimeInfo, JobIdentifier jobIdentifier, JobResult result) {
        try {
            this.httpClientHttpInvokerRequestExecutor.doPost("report_completed",
                    gson.toJson(new ReportCompleteStatusRequest(agentRuntimeInfo, jobIdentifier, result)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isIgnored(JobIdentifier jobIdentifier) {
        try {
            String isIgnored = this.httpClientHttpInvokerRequestExecutor.doPost("is_ignored",
                    gson.toJson(jobIdentifier));
            return parseBoolean(isIgnored);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getCookie(AgentIdentifier identifier, String location) {
        Map<String, Object> request = Map.of(
                "agent_identifier", identifier,
                "location", location);
        try {
            return this.httpClientHttpInvokerRequestExecutor.doPost("get_cookie",
                    gson.toJson(new GetCookieRequest(identifier, location)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void consumeLine(String line, JobIdentifier jobIdentifier) {
        this.buildRepositoryRemote.consumeLine(line, jobIdentifier);
    }

    @Override
    public void taggedConsumeLine(String tag, String line, JobIdentifier jobIdentifier) {
        this.buildRepositoryRemote.taggedConsumeLine(tag, line, jobIdentifier);
    }

    private static RuntimeTypeAdapterFactory<Builder> builderAdapter() {
        return RuntimeTypeAdapterFactory.of(Builder.class, "type")
                .registerSubtype(BuilderForKillAllChildTask.class, "BuilderForKillAllChildTask")
                .registerSubtype(CommandBuilder.class, "CommandBuilder")
                .registerSubtype(CommandBuilderWithArgList.class, "CommandBuilderWithArgList")
                .registerSubtype(FetchArtifactBuilder.class, "FetchArtifactBuilder")
                .registerSubtype(FetchPluggableArtifactBuilder.class, "FetchPluggableArtifactBuilder")
                .registerSubtype(NullBuilder.class, "NullBuilder")
                .registerSubtype(PluggableTaskBuilder.class, "PluggableTaskBuilder");
    }

    private RuntimeTypeAdapterFactory<Work> workAdapter() {
        return RuntimeTypeAdapterFactory.of(Work.class, "type")
                .registerSubtype(NoWork.class, "NoWork")
                .registerSubtype(BuildWork.class, "BuildWork")
                .registerSubtype(DeniedAgentWork.class, "DeniedAgentWork")
                .registerSubtype(UnregisteredAgentWork.class, "UnregisteredAgentWork");
    }

    private RuntimeTypeAdapterFactory<Material> materialAdapter() {
        return RuntimeTypeAdapterFactory.of(Material.class, "type")
                .registerSubtype(DependencyMaterial.class, "DependencyMaterial")
                .registerSubtype(GitMaterial.class, "GitMaterial")
                .registerSubtype(HgMaterial.class, "HgMaterial")
                .registerSubtype(P4Material.class, "P4Material")
                .registerSubtype(PackageMaterial.class, "PackageMaterial")
                .registerSubtype(PluggableSCMMaterial.class, "PluggableSCMMaterial")
                .registerSubtype(SvnMaterial.class, "SvnMaterial");
    }

    private static RuntimeTypeAdapterFactory<MaterialInstance> materialInstanceAdapter() {
        return RuntimeTypeAdapterFactory.of(MaterialInstance.class, "type")
                .registerSubtype(DependencyMaterialInstance.class, "DependencyMaterial")
                .registerSubtype(GitMaterialInstance.class, "GitMaterial")
                .registerSubtype(HgMaterialInstance.class, "HgMaterial")
                .registerSubtype(P4MaterialInstance.class, "P4Material")
                .registerSubtype(PackageMaterialInstance.class, "PackageMaterial")
                .registerSubtype(PluggableSCMMaterialInstance.class, "PluggableSCMMaterial")
                .registerSubtype(SvnMaterialInstance.class, "SvnMaterial");
    }

    private static RuntimeTypeAdapterFactory<AgentRuntimeInfo> agentRuntimeInfoAdapter() {
        return RuntimeTypeAdapterFactory.of(AgentRuntimeInfo.class, "type")
                .registerSubtype(AgentRuntimeInfo.class, "AgentRuntimeInfo")
                .registerSubtype(ElasticAgentRuntimeInfo.class, "ElasticAgentRuntimeInfo");
    }

    private static class ConfigurationPropertyAdapter implements JsonSerializer<ConfigurationProperty>,
            JsonDeserializer<ConfigurationProperty> {
        @Override
        public JsonElement serialize(ConfigurationProperty src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject serialized = new JsonObject();
            serialized.add("key", new JsonPrimitive(src.getConfigKeyName()));
            serialized.add("value", new JsonPrimitive(src.getValue()));

            return serialized;
        }

        @Override
        public ConfigurationProperty deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            return new ConfigurationProperty(new ConfigurationKey(json.getAsJsonObject().get("key").getAsString()),
                    new ConfigurationValue(json.getAsJsonObject().get("value").getAsString()));
        }
    }
}
