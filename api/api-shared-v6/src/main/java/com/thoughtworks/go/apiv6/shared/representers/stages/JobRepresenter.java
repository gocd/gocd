/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.apiv6.shared.representers.stages;

import com.thoughtworks.go.api.base.OutputListWriter;
import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.ErrorGetter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.apiv6.shared.representers.EnvironmentVariableRepresenter;
import com.thoughtworks.go.apiv6.shared.representers.stages.tasks.TaskRepresenter;
import com.thoughtworks.go.config.*;
import org.bouncycastle.crypto.InvalidCipherTextException;

import java.util.Collection;
import java.util.HashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class JobRepresenter {
    private static JsonReader jsonReader;

    public static void toJSON(OutputWriter jsonWriter, JobConfig jobConfig) {
        if (!jobConfig.errors().isEmpty()) {
            jsonWriter.addChild("errors", errorWriter -> {
                HashMap<String, String> errorMapping = new HashMap<>();
                errorMapping.put("runType", "run_instance_count");

                new ErrorGetter(errorMapping).toJSON(errorWriter, jobConfig);
            });
        }

        jsonWriter.addIfNotNull("name", jobConfig.name());
        addRunInstanceCount(jsonWriter, jobConfig);
        addTimeout(jsonWriter, jobConfig);
        jsonWriter.addIfNotNull("elastic_profile_id", jobConfig.getElasticProfileId());
        jsonWriter.addChildList("environment_variables", envVarsWriter -> EnvironmentVariableRepresenter.toJSON(envVarsWriter, jobConfig.getVariables()));
        jsonWriter.addChildList("resources", getResourceNames(jobConfig));
        jsonWriter.addChildList("tasks", getTasks(jobConfig));
        jsonWriter.addChildList("tabs", getTabs(jobConfig));
        jsonWriter.addChildList("artifacts", getArtifacts(jobConfig));
        if (jobConfig.getProperties().isEmpty()) {
            jsonWriter.renderNull("properties");
        } else {
            jsonWriter.addChildList("properties", getArtifactProperties(jobConfig));
        }
    }

    private static Consumer<OutputListWriter> getArtifactProperties(JobConfig jobConfig) {
        return jobPropertiesWriter -> {
            jobConfig.getProperties().forEach(artifactPropertyConfig -> {
                jobPropertiesWriter.addChild(artifactPropertyWriter -> PropertyConfigRepresenter.toJSON(artifactPropertyWriter, artifactPropertyConfig));
            });
        };
    }

    private static Consumer<OutputListWriter> getArtifacts(JobConfig jobConfig) {
        return artifactsWriter -> {
            jobConfig.artifactConfigs().forEach(artifactConfig -> {
                artifactsWriter.addChild(artifactWriter -> ArtifactRepresenter.toJSON(artifactWriter, artifactConfig));
            });
        };
    }

    private static Consumer<OutputListWriter> getTabs(JobConfig jobConfig) {
        return tabsWriter -> {
            jobConfig.getTabs().forEach(tab -> {
                tabsWriter.addChild(tabWriter -> TabConfigRepresenter.toJSON(tabWriter, tab));
            });
        };
    }

    private static Consumer<OutputListWriter> getTasks(JobConfig jobConfig) {
        return tasksWriter -> {
            jobConfig.getTasks().forEach(task -> {
                tasksWriter.addChild(taskWriter -> TaskRepresenter.toJSON(taskWriter, task));
            });
        };
    }

    private static Collection<String> getResourceNames(JobConfig jobConfig) {
        return jobConfig.resourceConfigs().stream().map(ResourceConfig::getName).collect(Collectors.toList());
    }

    private static void addTimeout(OutputWriter outputWriter, JobConfig jobConfig) {
        if ("0".equals(jobConfig.getTimeout())) {
            outputWriter.add("timeout", "never");
        } else if (jobConfig.getTimeout() != null && !jobConfig.getTimeout().isEmpty()) {
            outputWriter.add("timeout", Integer.parseInt(jobConfig.getTimeout()));
        } else {
            outputWriter.add("timeout", (String) null);
        }
    }

    private static void addRunInstanceCount(OutputWriter outputWriter, JobConfig jobConfig) {
        if (jobConfig.isRunOnAllAgents()) {
            outputWriter.add("run_instance_count", "all");
        } else if (jobConfig.getRunInstanceCount() != null && !jobConfig.getRunInstanceCount().isEmpty()) {
            outputWriter.add("run_instance_count", jobConfig.getRunInstanceCountValue());
        } else {
            outputWriter.add("run_instance_count", (String) null);
        }
    }

    public static JobConfig fromJSON(JsonReader jsonReader) {
        JobRepresenter.jsonReader = jsonReader;
        JobConfig jobConfig = new JobConfig();
        if (jsonReader == null) {
            return jobConfig;
        }
        jsonReader.readCaseInsensitiveStringIfPresent("name", jobConfig::setName);
        setRunInstanceCount(jobConfig);
        setTimeout(jobConfig);
        jsonReader.readStringIfPresent("elastic_profile_id", jobConfig::setElasticProfileId);
        setArtifacts(jobConfig);
        setEnvironmentVariables(jobConfig);
        setResources(jobConfig);
        setTabs(jobConfig);
        setProperties(jobConfig);
        setTasks(jobConfig);

        return jobConfig;
    }

    private static void setTasks(JobConfig jobConfig) {
        Tasks allTasks = new Tasks();
        jsonReader.readArrayIfPresent("tasks", tasks -> {
            tasks.forEach(task -> {
                allTasks.add(TaskRepresenter.fromJSON(new JsonReader(task.getAsJsonObject())));
            });
        });

        jobConfig.setTasks(allTasks);
    }

    private static void setProperties(JobConfig jobConfig) {
        ArtifactPropertiesConfig artifactPropertyConfigs = new ArtifactPropertiesConfig();
        jsonReader.readArrayIfPresent("properties", properties -> {
            properties.forEach(property -> {
                artifactPropertyConfigs.add(PropertyConfigRepresenter.fromJSON(new JsonReader(property.getAsJsonObject())));
            });
        });

        jobConfig.setProperties(artifactPropertyConfigs);
    }

    private static void setTabs(JobConfig jobConfig) {
        Tabs tabsConfig = new Tabs();
        jsonReader.readArrayIfPresent("tabs", tabs -> {
            tabs.forEach(tab -> {
                tabsConfig.add(TabConfigRepresenter.fromJSON(new JsonReader(tab.getAsJsonObject())));
            });
        });
        jobConfig.setTabs(tabsConfig);
    }

    private static void setResources(JobConfig jobConfig) {
        ResourceConfigs resourceConfigs = new ResourceConfigs();
        jsonReader.readArrayIfPresent("resources", resources -> {
            resources.forEach(resource -> {
                resourceConfigs.add(new ResourceConfig(resource.getAsString()));
            });
        });

        jobConfig.setResourceConfigs(resourceConfigs);
    }

    private static void setEnvironmentVariables(JobConfig jobConfig) {
        EnvironmentVariablesConfig environmentVariableConfigs = new EnvironmentVariablesConfig();
        jsonReader.readArrayIfPresent("environment_variables", variables -> {
            variables.forEach(variable -> {
                try {
                    environmentVariableConfigs.add(EnvironmentVariableRepresenter.fromJSON(new JsonReader(variable.getAsJsonObject())));
                } catch (InvalidCipherTextException e) {
                    e.printStackTrace();
                }
            });
        });
        jobConfig.setVariables(environmentVariableConfigs);
    }

    private static void setArtifacts(JobConfig jobConfig) {
        ArtifactConfigs artifactConfigs = new ArtifactConfigs();
        jsonReader.readArrayIfPresent("artifacts", artifacts -> {
            artifacts.forEach(artifact -> {
                artifactConfigs.add(ArtifactRepresenter.fromJSON(new JsonReader(artifact.getAsJsonObject())));
            });
        });
        jobConfig.setArtifactConfigs(artifactConfigs);
    }

    private static void setTimeout(JobConfig jobConfig) {
        String timeout = null;
        if (jsonReader.hasJsonObject("timeout")) {
            timeout = jsonReader.getString("timeout");
        }

        if ("never".equalsIgnoreCase(timeout)) {
            jobConfig.setTimeout("0");
        } else if (!"null".equalsIgnoreCase(timeout)) {
            jobConfig.setTimeout(timeout);
        }
    }

    private static void setRunInstanceCount(JobConfig jobConfig) {
        String runInstanceCount = null;
        if (jsonReader.hasJsonObject("run_instance_count")) {
            runInstanceCount = jsonReader.getString("run_instance_count");
        }
        if ("all".equalsIgnoreCase(runInstanceCount)) {
            jobConfig.setRunOnAllAgents(true);
        } else if (!"null".equalsIgnoreCase(runInstanceCount)) {
            jobConfig.setRunInstanceCount(runInstanceCount);
        }
    }
}
