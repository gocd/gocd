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
package com.thoughtworks.go.config.remote;


import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.config.preprocessor.SkipParameterResolution;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.PipelineGroups;
import com.thoughtworks.go.domain.scm.SCM;
import com.thoughtworks.go.domain.scm.SCMs;
import lombok.EqualsAndHashCode;

import static com.thoughtworks.go.config.exceptions.EntityType.*;
import static com.thoughtworks.go.config.rules.SupportedEntity.*;

/**
 * A config fragment that can be conceptually thought of as an external snippet of
 * cruise-config.xml that can be merged into the main cruise-config.xml configuration.
 * <p>
 * More accurately, this is essentially a subtree of configuration domain objects, and
 * not actual XML. A {@link PartialConfig} is typically the parsed and deserialized
 * and parsed output of a {@link ConfigRepoConfig}'s definition files, which are written
 * in whatever the specified language or syntax declared its associated plugin.
 */
@ConfigTag("cruise")
@EqualsAndHashCode
public class PartialConfig implements Validatable, ConfigOriginTraceable {
    @ConfigSubtag(label = "groups")
    private PipelineGroups pipelines = new PipelineGroups();

    @ConfigSubtag
    @SkipParameterResolution
    private EnvironmentsConfig environments = new EnvironmentsConfig();

    @ConfigSubtag(label = "scms")
    private SCMs scms = new SCMs();

    private ConfigOrigin origin;
    private final ConfigErrors errors = new ConfigErrors();

    public PartialConfig() {
    }

    public PartialConfig(PipelineGroups pipelines) {
        this.pipelines = pipelines;
    }

    public PartialConfig(EnvironmentsConfig environments, PipelineGroups pipelines) {
        this.environments = environments;
        this.pipelines = pipelines;
    }

    public PartialConfig(EnvironmentsConfig environments, PipelineGroups pipelines, SCMs scms) {
        this.environments = environments;
        this.pipelines = pipelines;
        this.scms = scms;
    }

    @Override
    public String toString() {
        return String.format("ConfigPartial: %s pipes, %s environments, %s scms; From %s", pipelines.size(), environments.size(), scms.size(), origin);
    }

    @Override
    public void validate(ValidationContext validationContext) {
        validatePermissionsOnSubtree();
    }

    public void validatePermissionsOnSubtree() {
        errors.clear();

        final ConfigRepoConfig repo = configRepoConfig();

        if (null == repo) {
            return;
        }

        getEnvironments().stream()
                .filter(env -> !repo.canRefer(ENVIRONMENT, env.name()))
                .forEach(this::addViolationOnForbiddenEnvironment);

        getGroups().stream()
                .filter(group -> !repo.canRefer(PIPELINE_GROUP, group.getGroup()))
                .forEach(this::addViolationOnForbiddenPipelineGroup);

        getGroups().forEach(g -> g.forEach(p -> p.dependencyMaterialConfigs().stream().
                filter(this::upstreamPipelineNotDefinedInPartial).
                filter(d -> !repo.canRefer(PIPELINE, d.getPipelineName())).
                forEach(this::addViolationOnForbiddenPipeline)));

        ErrorCollector.getAllErrors(this).forEach(this.errors::addAll);
    }

    public boolean hasErrors() {
        return errors().present();
    }

    @Override
    public ConfigErrors errors() {
        return errors;
    }

    @Override
    public void addError(String fieldName, String message) {
        errors.add(fieldName, message);
    }

    @Override
    public ConfigOrigin getOrigin() {
        return origin;
    }

    public void setOrigin(ConfigOrigin origin) {
        this.origin = origin;
    }

    @Override
    public void setOrigins(ConfigOrigin origins) {
        this.origin = origins;
        for (EnvironmentConfig env : this.environments) {
            env.setOrigins(origins);
        }
        for (PipelineConfigs pipes : this.pipelines) {
            pipes.setOrigins(origins);
        }
        for (SCM scm : this.scms) {
            scm.setOrigins(origins);
        }
    }

    public EnvironmentsConfig getEnvironments() {
        return environments;
    }

    public void setEnvironments(EnvironmentsConfig environments) {
        this.environments = environments;
    }

    public PipelineGroups getGroups() {
        return pipelines;
    }

    public void setPipelines(PipelineGroups pipelines) {
        this.pipelines = pipelines;
    }

    public SCMs getScms() {
        return scms;
    }

    public void setScms(SCMs scms) {
        this.scms = scms;
    }

    private boolean upstreamPipelineNotDefinedInPartial(DependencyMaterialConfig dependency) {
        return getGroups().stream().allMatch(group -> group.findBy(dependency.getPipelineName()) == null);
    }

    private void addViolationOnForbiddenPipelineGroup(PipelineConfigs forbidden) {
        forbidden.addError(PIPELINE_GROUP.getType(), PipelineGroup.notAllowedToRefer(forbidden.getGroup()));
    }

    private void addViolationOnForbiddenPipeline(DependencyMaterialConfig forbidden) {
        forbidden.addError(PIPELINE.getType(), Pipeline.notAllowedToRefer(forbidden.getPipelineName()));
    }

    private void addViolationOnForbiddenEnvironment(EnvironmentConfig forbidden) {
        forbidden.addError(ENVIRONMENT.getType(), Environment.notAllowedToRefer(forbidden.name()));
    }

    private ConfigRepoConfig configRepoConfig() {
        final ConfigOrigin origin = getOrigin();

        if (origin instanceof RepoConfigOrigin) {
            return ((RepoConfigOrigin) origin).getConfigRepo();
        } else {
            return null;
        }
    }
}
