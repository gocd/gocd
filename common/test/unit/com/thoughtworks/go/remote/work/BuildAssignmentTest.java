/*
 * Copyright 2017 ThoughtWorks, Inc.
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

import com.google.gson.Gson;
import com.thoughtworks.go.config.ArtifactPlans;
import com.thoughtworks.go.config.ArtifactPropertiesGenerators;
import com.thoughtworks.go.config.EnvironmentVariablesConfig;
import com.thoughtworks.go.config.Resources;
import com.thoughtworks.go.domain.DefaultJobPlan;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.helper.MaterialsMother;
import com.thoughtworks.go.helper.ModificationsMother;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import org.junit.Test;

import java.util.*;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class BuildAssignmentTest {
    @Test
    public void shouldInitializeEnvironmentContextFromJobPlan() throws Exception {
        DefaultJobPlan defaultJobPlan = jobForPipeline("foo");

        EnvironmentVariablesConfig variables = new EnvironmentVariablesConfig();
        variables.add("key1", "value1");
        variables.add("key2", "value2");

        defaultJobPlan.setVariables(variables);

        BuildAssignment buildAssignment = BuildAssignment.create(defaultJobPlan, BuildCause.createManualForced(), new ArrayList<>(), null, null);
        EnvironmentVariableContext context = buildAssignment.initialEnvironmentVariableContext();

        assertThat(context.getProperties().size(), is(2));
        assertThat(context.getProperty("key1"), is("value1"));
        assertThat(context.getProperty("key2"), is("value2"));
    }

    @Test
    public void shouldInitializeEnvironmentContextFromJobPlanWithTriggerVariablesOverridingEnvVariablesFromJob() throws Exception {
        DefaultJobPlan defaultJobPlan = jobForPipeline("foo");
        EnvironmentVariablesConfig triggerVariables = new EnvironmentVariablesConfig();
        triggerVariables.add("key1", "override");
        triggerVariables.add("key3", "value3");

        EnvironmentVariablesConfig variables = new EnvironmentVariablesConfig();
        variables.add("key1", "value1");
        variables.add("key2", "value2");

        defaultJobPlan.setTriggerVariables(triggerVariables);
        defaultJobPlan.setVariables(variables);

        BuildAssignment buildAssignment = BuildAssignment.create(defaultJobPlan, BuildCause.createManualForced(), new ArrayList<>(), null, null);
        EnvironmentVariableContext context = buildAssignment.initialEnvironmentVariableContext();

        assertThat(context.getProperties().size(), is(2));
        assertThat(context.getProperty("key1"), is("override"));
        assertThat(context.getProperty("key2"), is("value2"));
    }

    @Test
    public void shouldIntializeEnvironmentContextWithJobPlanEnvironmentVariablesOveridingEnvVariablesFromTheEnvironment() throws Exception {
        DefaultJobPlan defaultJobPlan = jobForPipeline("foo");

        EnvironmentVariablesConfig variables = new EnvironmentVariablesConfig();
        variables.add("key1", "value_from_job_plan");
        variables.add("key2", "value2");

        defaultJobPlan.setVariables(variables);

        EnvironmentVariableContext contextFromEnvironment = new EnvironmentVariableContext("key1", "value_from_environment");
        contextFromEnvironment.setProperty("key3", "value3", false);

        BuildAssignment buildAssignment = BuildAssignment.create(defaultJobPlan, BuildCause.createManualForced(), new ArrayList<>(), null, contextFromEnvironment);
        EnvironmentVariableContext context = buildAssignment.initialEnvironmentVariableContext();

        assertThat(context.getProperties().size(), is(3));
        assertThat(context.getProperty("key1"), is("value_from_job_plan"));
        assertThat(context.getProperty("key2"), is("value2"));
        assertThat(context.getProperty("key3"), is("value3"));
    }

    @Test
    public void shouldNotHaveReferenceToModifiedFilesSinceLargeCommitsCouldCauseBothServerAndAgentsToRunOutOfMemory_MoreoverThisInformationIsNotRequiredOnAgentSide() {
        List<Modification> modificationsForSvn = ModificationsMother.multipleModificationList();
        List<Modification> modificationsForHg = ModificationsMother.multipleModificationList();
        MaterialRevision svn = new MaterialRevision(MaterialsMother.svnMaterial(), modificationsForSvn);
        MaterialRevision hg = new MaterialRevision(MaterialsMother.hgMaterial(), modificationsForHg);
        MaterialRevisions materialRevisions = new MaterialRevisions(svn, hg);
        BuildCause buildCause = BuildCause.createWithModifications(materialRevisions, "user1");

        BuildAssignment buildAssignment = BuildAssignment.create(jobForPipeline("foo"), buildCause, new ArrayList<>(), null, null);

        assertThat(buildAssignment.getBuildApprover(), is("user1"));
        assertThat(buildAssignment.materialRevisions().getRevisions().size(), is(materialRevisions.getRevisions().size()));
        assertRevisions(buildAssignment, svn);
        assertRevisions(buildAssignment, hg);
    }

    @Test
    public void shouldCopyAdditionalDataToBuildAssignment() {
        MaterialRevision packageMaterialRevision = ModificationsMother.createPackageMaterialRevision("revision");
        Map<String, String> additionalData = new HashMap<>();
        additionalData.put("a1", "v1");
        additionalData.put("a2", "v2");
        String additionalDataAsString = new Gson().toJson(additionalData);
        packageMaterialRevision.getModifications().first().setAdditionalData(additionalDataAsString);
        MaterialRevisions materialRevisions = new MaterialRevisions(packageMaterialRevision);
        BuildCause buildCause = BuildCause.createWithModifications(materialRevisions, "user1");

        BuildAssignment buildAssignment = BuildAssignment.create(jobForPipeline("foo"), buildCause, new ArrayList<>(), null, null);

        assertThat(buildAssignment.getBuildApprover(), is("user1"));
        assertThat(buildAssignment.materialRevisions().getRevisions().size(), is(materialRevisions.getRevisions().size()));
        assertRevisions(buildAssignment, packageMaterialRevision);
        Modification actualModification = buildAssignment.materialRevisions().getRevisions().get(0).getModification(0);
        assertThat(actualModification.getAdditionalData(), is(additionalDataAsString));
        assertThat(actualModification.getAdditionalDataMap(), is(additionalData));
    }

    private void assertRevisions(BuildAssignment buildAssignment, MaterialRevision expectedRevision) {
        MaterialRevision actualRevision = buildAssignment.materialRevisions().findRevisionFor(expectedRevision.getMaterial());
        assertThat(actualRevision.getMaterial(), is(expectedRevision.getMaterial()));
        assertThat(actualRevision.getModifications().size(), is(expectedRevision.getModifications().size()));
        for (int i = 0; i < actualRevision.getModifications().size(); i++) {
            final Modification actualModification = actualRevision.getModifications().get(i);
            final Modification expectedModification = expectedRevision.getModifications().get(i);
            assertThat(actualModification.getRevision(), is(expectedModification.getRevision()));
            assertThat(actualModification.getModifiedFiles().isEmpty(), is(true));
        }
    }

    private DefaultJobPlan jobForPipeline(String pipelineName) {
        JobIdentifier jobIdentifier = new JobIdentifier(pipelineName, 1, "1", "defaultStage", "1", "job1", 100L);
        return new DefaultJobPlan(new Resources(), new ArtifactPlans(), new ArtifactPropertiesGenerators(), 1L, jobIdentifier, null, new EnvironmentVariablesConfig(), new EnvironmentVariablesConfig(), null);
    }
}
