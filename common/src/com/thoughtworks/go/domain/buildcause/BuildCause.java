/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.domain.buildcause;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.EnvironmentVariablesConfig;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.Materials;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.ModificationSummaries;
import com.thoughtworks.go.domain.ModificationVisitorAdapter;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.domain.materials.Revision;
import com.thoughtworks.go.server.domain.Username;

/**
 * @understands why a pipeline was triggered and what revisions it contains
 */
public class BuildCause implements Serializable {

    private MaterialRevisions materialRevisions = MaterialRevisions.EMPTY;
    private BuildTrigger trigger;
    private static final BuildCause NEVER_RUN = new BuildCause(MaterialRevisions.EMPTY, BuildTrigger.forNeverRun(), "");

    private String approver;
    private EnvironmentVariablesConfig variables;

    protected BuildCause() {
        variables = new EnvironmentVariablesConfig();
    }

    private BuildCause(MaterialRevisions materialRevisions, BuildTrigger trigger, String approver) {
        this();
        this.approver = approver;
        if (materialRevisions != null) {
            this.materialRevisions = materialRevisions;
        }
        this.trigger = trigger;
    }

    public MaterialRevisions getMaterialRevisions() {
        return materialRevisions;
    }

    public final String getBuildCauseMessage() {
        return trigger.getMessage();
    }

    public void setMessage(String buildCauseMessage) {
        trigger.setMessage(buildCauseMessage);
    }

    public final boolean isForced() {
        return trigger.isForced();
    }

    public void setMaterialRevisions(MaterialRevisions materialRevisions) {
        this.materialRevisions = materialRevisions;
        BuildTrigger newTrigger = BuildTrigger.forModifications(materialRevisions);
        if (newTrigger.trumps(this.trigger)) {
            this.trigger = newTrigger;
        }
    }

    public ModificationSummaries toModificationSummaries() {
        return new ModificationSummaries(getMaterialRevisions());
    }

    public boolean trumps(BuildCause existingBuildCause) {
        return trigger.trumps(existingBuildCause.trigger);
    }

    public Date getModifiedDate() {
        return materialRevisions.getDateOfLatestModification();
    }

    public Materials materials() {
        final List<Material> materials = new ArrayList<>();
        materialRevisions.accept(new ModificationVisitorAdapter() {
            public void visit(Material material, Revision revision) {
                materials.add(material);
            }
        });
        return new Materials(materials);

    }

    public String toString() {
        return String.format("[%s: %s]", trigger.getDbName(), getBuildCauseMessage());
    }

    public boolean materialsMatch(MaterialConfigs other){
        try {
            assertMaterialsMatch(other);
        } catch (BuildCauseOutOfDateException e) {
            return false;
        }
        return true;
    }
    public boolean pipelineConfigAndMaterialRevisionMatch(PipelineConfig pipelineConfig){
        if(!pipelineConfig.isConfigOriginSameAsOneOfMaterials())
        {
            return true;
        }

        RepoConfigOrigin repoConfigOrigin = (RepoConfigOrigin)pipelineConfig.getOrigin();

        MaterialConfig configAndCodeMaterial = repoConfigOrigin.getMaterial();
        //TODO if revision in any of the pipelines match
        MaterialRevision revision = this.getMaterialRevisions().findRevisionForFingerPrint(configAndCodeMaterial.getFingerprint());

        String revisionString = revision.getRevision().getRevision();
        if(pipelineConfig.isConfigOriginFromRevision(revisionString))
        {
            return true;
        }

        return false;
    }

    public void assertMaterialsMatch(MaterialConfigs other) {
        Materials materialsFromBuildCause = materials();
        for (MaterialConfig materialConfig : other) {
            if (!materialsFromBuildCause.hasMaterialConfigWithFingerprint(materialConfig)) {
                invalid(other);
            }
        }
    }
    public void assertPipelineConfigAndMaterialRevisionMatch(PipelineConfig pipelineConfig) {
        if(!pipelineConfig.isConfigOriginSameAsOneOfMaterials())
        {
            return;
        }
        // then config and code revision must both match
        if(this.trigger.isForced())
        {
            // we should not check when manual trigger because of re-runs
            // and possibility to specify revisions to run with
            return;
        }

        RepoConfigOrigin repoConfigOrigin = (RepoConfigOrigin)pipelineConfig.getOrigin();

        MaterialConfig configAndCodeMaterial = repoConfigOrigin.getMaterial();
        //TODO if revision in any of the pipelines match
        MaterialRevision revision = this.getMaterialRevisions().findRevisionForFingerPrint(configAndCodeMaterial.getFingerprint());

        String revisionString = revision.getRevision().getRevision();
        if(pipelineConfig.isConfigOriginFromRevision(revisionString))
        {
            return;
        }

        invalidRevision(repoConfigOrigin.getRevision(),revisionString);
    }

    private void invalidRevision(String configRevision,String codeRevision) {
        throw new BuildCauseOutOfDateException(
                "Illegal build cause - pipeline configuration is from different revision than scm material. "
                        + "Pipeline configuration revision: " + configRevision + " "
                        + "while revision to schedule is : " + codeRevision + "");
    }

    private void invalid(MaterialConfigs materialsFromConfig) {
        throw new BuildCauseOutOfDateException(
                "Illegal build cause - it has different materials from the pipeline it is trying to trigger. "
                + "Materials for which modifications are available: " + materials() + " "
                + "while pipeline is configured with: " + materialsFromConfig + "");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        BuildCause that = (BuildCause) o;

        if (materialRevisions != null ? !materialRevisions.equals(that.materialRevisions) : that.materialRevisions != null) {
            return false;
        }
        if (trigger != null ? !trigger.equals(that.trigger) : that.trigger != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = materialRevisions != null ? materialRevisions.hashCode() : 0;
        result = 31 * result + (trigger != null ? trigger.hashCode() : 0);
        return result;
    }

    public boolean isSameAs(BuildCause buildCause) {
        if (this.trigger.getDbName() != buildCause.trigger.getDbName()) {
            return false;
        }
        return materialRevisions.isSameAs(buildCause.materialRevisions);
    }

    public boolean hasNeverRun() {
        return this.equals(createNeverRun());
    }

    public static BuildCause createManualForced(MaterialRevisions materialRevisions, Username username) {
        if (username == null) {
            throw new IllegalArgumentException("Username cannot be null");
        }

        String cause = "";
        if (materialRevisions != null && !materialRevisions.isEmpty()) {
            cause = materialRevisions.buildCausedBy();
        }

        String message = String.format("Forced by %s", username.getDisplayName());

        return new BuildCause(materialRevisions, BuildTrigger.forForced(message), CaseInsensitiveString.str(username.getUsername()));
    }

    public static BuildCause createManualForced() {
        return createManualForced(MaterialRevisions.EMPTY, Username.ANONYMOUS);
    }

    public static BuildCause fromDbString(String text) {
        if (text.equals(BuildTrigger.FORCED_BUILD_CAUSE)) {
            return BuildCause.createManualForced(MaterialRevisions.EMPTY, Username.ANONYMOUS);
        } else if (text.equals(BuildTrigger.MODIFICATION_BUILD_CAUSE)) {
            return createWithEmptyModifications();
        } else if (text.equals(BuildTrigger.EXTERNAL_BUILD_CAUSE)) {
            return createExternal();
        }
        return createWithEmptyModifications();
    }

    public String toDbString() {
        return trigger.getDbName();
    }

    public static BuildCause createWithModifications(MaterialRevisions materialRevisions, String approver) {
        return new BuildCause(materialRevisions, BuildTrigger.forModifications(materialRevisions), approver);
    }

    public static BuildCause createWithEmptyModifications() {
        return createWithModifications(MaterialRevisions.EMPTY, "");
    }

    public static BuildCause createExternal() {
        return new BuildCause(MaterialRevisions.EMPTY, BuildTrigger.forExternal(), "");
    }

    public static BuildCause createNeverRun() {
            return NEVER_RUN;
    }

    public String getApprover() {
        return approver;
    }

    public void setApprover(String approvedBy) {
        approver = approvedBy;
    }

    public EnvironmentVariablesConfig getVariables() {
        return variables;
    }

    //for ibatis
    public void setVariables(EnvironmentVariablesConfig variables) {
        this.variables = variables;
    }

    public void addOverriddenVariables(EnvironmentVariablesConfig variables) {
        this.variables.addAll(variables);
    }


}
