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
package com.thoughtworks.go.remote.work;

import com.thoughtworks.go.config.ArtifactStores;
import com.thoughtworks.go.config.SecretParamAware;
import com.thoughtworks.go.config.SecretParams;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.builder.Builder;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;

public class BuildAssignment implements Serializable, SecretParamAware {
    private final boolean fetchMaterials;
    private final boolean cleanWorkingDirectory;
    private final List<Builder> builders;
    private final List<ArtifactPlan> artifactPlans;
    private final ArtifactStores artifactStores;
    private final File buildWorkingDirectory;
    private final JobIdentifier jobIdentifier;
    private final EnvironmentVariableContext initialContext = new EnvironmentVariableContext();
    private final MaterialRevisions materialRevisions = new MaterialRevisions();
    private final String approver;

    private BuildAssignment(BuildCause buildCause, File buildWorkingDirectory, List<Builder> builder, JobIdentifier jobIdentifier,
                            boolean fetchMaterials, boolean cleanWorkingDirectory, List<ArtifactPlan> artifactPlans,
                            ArtifactStores artifactStores) {
        this.buildWorkingDirectory = buildWorkingDirectory;
        this.builders = builder;
        this.jobIdentifier = jobIdentifier;
        this.fetchMaterials = fetchMaterials;
        this.cleanWorkingDirectory = cleanWorkingDirectory;
        this.artifactPlans = artifactPlans;
        this.artifactStores = artifactStores;
        for (MaterialRevision materialRevision : buildCause.getMaterialRevisions()) {
            ArrayList<Modification> modifications = new ArrayList<>();
            for (Modification modification : materialRevision.getModifications()) {
                modifications.add(new Modification(modification, false));
            }
            materialRevisions.addRevision(new MaterialRevision(materialRevision.getMaterial(), materialRevision.isChanged(), modifications));
        }
        approver = buildCause.getApprover();
    }

    @Override
    public String toString() {
        return "BuildAssignment{" +
                "jobIdentifier=" + jobIdentifier +
                ", materialRevisions=" + materialRevisions +
                ", approver='" + approver + '\'' +
                '}';
    }

    public static BuildAssignment create(JobPlan plan, BuildCause buildCause, List<Builder> builders, File buildWorkingDirectory, EnvironmentVariableContext contextFromEnvironment, ArtifactStores artifactStores) {
        BuildAssignment buildAssignment = new BuildAssignment(buildCause, buildWorkingDirectory, builders, plan.getIdentifier(), plan.shouldFetchMaterials(),
                plan.shouldCleanWorkingDir(), plan.getArtifactPlans(), artifactStores);

        if (contextFromEnvironment != null) {
            buildAssignment.initialEnvironmentVariableContext().addAll(contextFromEnvironment);
        }
        
        String commaSeparatedResources = plan.getResources().stream().map (Object::toString).collect(Collectors.joining(","));
        buildAssignment.initialEnvironmentVariableContext().setProperty("GO_AGENT_RESOURCES", commaSeparatedResources, false);
        buildAssignment.initialEnvironmentVariableContext().setProperty("GO_TRIGGER_USER", buildAssignment.getBuildApprover(), false);
        buildAssignment.getJobIdentifier().populateEnvironmentVariables(buildAssignment.initialEnvironmentVariableContext());
        buildAssignment.materialRevisions().populateEnvironmentVariables(buildAssignment.initialEnvironmentVariableContext(), buildWorkingDirectory);
        plan.applyTo(buildAssignment.initialEnvironmentVariableContext());
        return buildAssignment;
    }

    public MaterialRevisions materialRevisions() {
        return materialRevisions;
    }


    public File getWorkingDirectory() {
        return buildWorkingDirectory;
    }

    public List<Builder> getBuilders() {
        return builders;
    }

    public EnvironmentVariableContext initialEnvironmentVariableContext() {
        return initialContext;
    }

    public String getBuildApprover() {
        return approver;
    }

    public JobIdentifier getJobIdentifier() {
        return jobIdentifier;
    }

    public boolean shouldFetchMaterials() {
        return fetchMaterials;
    }

    public boolean shouldCleanWorkingDir() {
        return cleanWorkingDirectory;
    }

    public List<ArtifactPlan> getArtifactPlans() {
        return artifactPlans;
    }

    public ArtifactStores getArtifactStores() {
        return artifactStores;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BuildAssignment)) return false;

        BuildAssignment that = (BuildAssignment) o;

        if (fetchMaterials != that.fetchMaterials) return false;
        if (cleanWorkingDirectory != that.cleanWorkingDirectory) return false;
        if (builders != null ? !builders.equals(that.builders) : that.builders != null) return false;
        if (artifactPlans != null ? !artifactPlans.equals(that.artifactPlans) : that.artifactPlans != null)
            return false;
        if (artifactStores != null ? !artifactStores.equals(that.artifactStores) : that.artifactStores != null)
            return false;
        if (buildWorkingDirectory != null ? !buildWorkingDirectory.equals(that.buildWorkingDirectory) : that.buildWorkingDirectory != null)
            return false;
        if (jobIdentifier != null ? !jobIdentifier.equals(that.jobIdentifier) : that.jobIdentifier != null)
            return false;
        if (initialContext != null ? !initialContext.equals(that.initialContext) : that.initialContext != null)
            return false;
        if (materialRevisions != null ? !materialRevisions.equals(that.materialRevisions) : that.materialRevisions != null)
            return false;
        return approver != null ? approver.equals(that.approver) : that.approver == null;
    }

    @Override
    public int hashCode() {
        int result = (fetchMaterials ? 1 : 0);
        result = 31 * result + (cleanWorkingDirectory ? 1 : 0);
        result = 31 * result + (builders != null ? builders.hashCode() : 0);
        result = 31 * result + (artifactPlans != null ? artifactPlans.hashCode() : 0);
        result = 31 * result + (artifactStores != null ? artifactStores.hashCode() : 0);
        result = 31 * result + (buildWorkingDirectory != null ? buildWorkingDirectory.hashCode() : 0);
        result = 31 * result + (jobIdentifier != null ? jobIdentifier.hashCode() : 0);
        result = 31 * result + (initialContext != null ? initialContext.hashCode() : 0);
        result = 31 * result + (materialRevisions != null ? materialRevisions.hashCode() : 0);
        result = 31 * result + (approver != null ? approver.hashCode() : 0);
        return result;
    }

    @Override
    public boolean hasSecretParams() {
        return !SecretParams.union(secretParamsInEnvironmentVariables(), secretParamsInMaterials()).isEmpty();
    }

    @Override
    public SecretParams getSecretParams() {
        return SecretParams.union(secretParamsInEnvironmentVariables(), secretParamsInMaterials());
    }

    private SecretParams secretParamsInEnvironmentVariables() {
        return this.initialEnvironmentVariableContext().getSecretParams()
                .stream()
                .collect(SecretParams.toSecretParams());
    }

    private SecretParams secretParamsInMaterials() {
        final List<Material> materials = stream(this.materialRevisions().spliterator(), true)
                .map(MaterialRevision::getMaterial).collect(toList());

        return materials.stream()
                .filter(material -> material instanceof SecretParamAware)
                .filter(material -> ((SecretParamAware) material).hasSecretParams())
                .map(material -> ((SecretParamAware) material).getSecretParams())
                .collect(SecretParams.toFlatSecretParams());
    }
}
