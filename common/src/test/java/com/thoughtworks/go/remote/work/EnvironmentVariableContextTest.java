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

package com.thoughtworks.go.remote.work;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.materials.dependency.DependencyMaterialRevision;
import com.thoughtworks.go.helper.MaterialsMother;
import com.thoughtworks.go.helper.ModificationsMother;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import org.apache.commons.lang3.StringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.util.Date;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class EnvironmentVariableContextTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();
    @Rule
    public final RestoreSystemProperties restoreSystemProperties = new RestoreSystemProperties();

    private String pipelineName = "pipeline-name";
    private String pipelineLabel = "pipeline-label";
    private String stageName = "stage-name";
    private String stageCounter = "stage-counter";
    private String jobName = "build-name";

    @Test
    public void shouldPopulateEnvironmentForServerUrl(){
        new SystemEnvironment().setProperty("serviceUrl", "some_random_place");

        EnvironmentVariableContext context = new EnvironmentVariableContext();

        context.setProperty("GO_SERVER_URL", SystemEnvironment.getProperty("serviceUrl"), false);
        jobIdentifier().populateEnvironmentVariables(context);

        assertThat(context.getProperty("GO_SERVER_URL"), is("some_random_place"));
    }

    @Test
    public void shouldPopulateEnvironmentForJobIdentifier(){
        EnvironmentVariableContext context = new EnvironmentVariableContext();

        context.setProperty("GO_SERVER_URL", SystemEnvironment.getProperty("serviceUrl"), false);
        jobIdentifier().populateEnvironmentVariables(context);

        assertThat(context.getProperty("GO_PIPELINE_NAME"), is(pipelineName));
        assertThat(context.getProperty("GO_PIPELINE_LABEL"), is(pipelineLabel));
        assertThat(context.getProperty("GO_STAGE_NAME"), is(stageName));
        assertThat(context.getProperty("GO_STAGE_COUNTER"), is(stageCounter));
        assertThat(context.getProperty("GO_JOB_NAME"), is(jobName));
    }

    @Test
    public void shouldPopulateEnvironmentForMaterialUsingMaterialName() throws IOException {
        SvnMaterial svn = MaterialsMother.svnMaterial();
        svn.setName(new CaseInsensitiveString("svn"));
        svn.setFolder("svn-dir");
        MaterialRevision revision = new MaterialRevision(svn, ModificationsMother.oneModifiedFile("revision1"));
        MaterialRevisions materialRevisions = new MaterialRevisions(revision);

        EnvironmentVariableContext context = new EnvironmentVariableContext();

        context.setProperty("GO_SERVER_URL", SystemEnvironment.getProperty("serviceUrl"), false);
        jobIdentifier().populateEnvironmentVariables(context);

        materialRevisions.populateEnvironmentVariables(context, temporaryFolder.newFolder());

        assertThat(context.getProperty("GO_REVISION_SVN"), is("revision1"));
        assertThat(context.getProperty("GO_MATERIAL_SVN_HAS_CHANGED"), is("false"));
    }

    @Test
    public void shouldPopulateEnvironmentForMaterialUsingDest() throws IOException {
        SvnMaterial svn = MaterialsMother.svnMaterial();
        svn.setFolder("svn-dir");
        MaterialRevision revision = new MaterialRevision(svn,
                ModificationsMother.oneModifiedFile("revision1"));
        MaterialRevisions materialRevisions = new MaterialRevisions(revision);

        EnvironmentVariableContext context = new EnvironmentVariableContext();

        context.setProperty("GO_SERVER_URL", SystemEnvironment.getProperty("serviceUrl"), false);
        jobIdentifier().populateEnvironmentVariables(context);

        materialRevisions.populateEnvironmentVariables(context, temporaryFolder.newFolder());

        assertThat(context.getProperty("GO_REVISION_SVN_DIR"), is("revision1"));
    }

    @Test
    public void shouldPopulateEnvironmentForDependencyMaterialUsingMaterialName() throws IOException {
        String materialName = "upstreamPipeline";
        MaterialRevision revision = materialRevision(materialName, "pipeline-name", 1, "pipeline-label", "stage-name", 1);
        MaterialRevisions materialRevisions = new MaterialRevisions(revision);

        EnvironmentVariableContext context = new EnvironmentVariableContext();

        context.setProperty("GO_SERVER_URL", SystemEnvironment.getProperty("serviceUrl"), false);
        jobIdentifier().populateEnvironmentVariables(context);

        materialRevisions.populateEnvironmentVariables(context, temporaryFolder.newFolder());

        assertThat(context.getProperty("GO_DEPENDENCY_LABEL_UPSTREAMPIPELINE"), is("pipeline-label"));
        assertThat(context.getProperty("GO_DEPENDENCY_LOCATOR_UPSTREAMPIPELINE"), is("pipeline-name/1/stage-name/1"));
    }

    @Test
    public void shouldPopulateEnvironmentForDependencyMaterialUsingPipelineNameStageName() throws IOException {
        String EMPTY_NAME = "";
        MaterialRevision revision = materialRevision(EMPTY_NAME, "pipeline-name", 1, "pipeline-label", "stage-name", 1);
        MaterialRevisions materialRevisions = new MaterialRevisions(revision);

        EnvironmentVariableContext context = new EnvironmentVariableContext();

        context.setProperty("GO_SERVER_URL", SystemEnvironment.getProperty("serviceUrl"), false);
        jobIdentifier().populateEnvironmentVariables(context);

        materialRevisions.populateEnvironmentVariables(context, temporaryFolder.newFolder());

        assertThat(context.getProperty("GO_DEPENDENCY_LABEL_PIPELINE_NAME"), is("pipeline-label"));
        assertThat(context.getProperty("GO_DEPENDENCY_LOCATOR_PIPELINE_NAME"),
                is("pipeline-name/1/stage-name/1"));
    }

    private MaterialRevision materialRevision(String materialName, String pipelineName, Integer pipelineCounter,
                                              String pipelineLabel,
                                              String stageName, int stageCounter) {
        DependencyMaterial material = new DependencyMaterial(new CaseInsensitiveString(pipelineName), new CaseInsensitiveString(stageName));
        if (!StringUtils.isEmpty(materialName)) {
            material.setName(new CaseInsensitiveString(materialName));
        }

        DependencyMaterialRevision revision = DependencyMaterialRevision.create(pipelineName, pipelineCounter,
                pipelineLabel, stageName, stageCounter);
        MaterialRevision materialRevision = revision.convert(material, new Date());
        return materialRevision;
    }

    private JobIdentifier jobIdentifier() {
        JobIdentifier jobIdentifier = new JobIdentifier(pipelineName, 1, pipelineLabel, stageName, stageCounter, jobName, 1L);
        return jobIdentifier;
    }
}
