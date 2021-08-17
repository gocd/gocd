/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.plugin.configrepo.contract;

import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.plugin.configrepo.contract.material.CRDependencyMaterial;
import com.thoughtworks.go.plugin.configrepo.contract.material.CRGitMaterial;
import com.thoughtworks.go.plugin.configrepo.contract.material.CRMaterial;
import com.thoughtworks.go.plugin.configrepo.contract.tasks.CRBuildTask;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;

import static com.thoughtworks.go.util.TestUtils.contains;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class CRPipelineTest extends AbstractCRTest<CRPipeline> {

    private final CRPipeline pipe1;
    private final CRPipeline customPipeline;
    private final CRPipeline invalidNoName;
    private final CRPipeline invalidNoMaterial;
    private final CRPipeline invalidNoStages;
    private final CRPipeline invalidNoNamedMaterials;
    private final CRGitMaterial veryCustomGit;
    private final CRStage buildStage;
    private final CRPipeline invalidNoGroup;

    public CRPipelineTest() {
        CRBuildTask rakeTask = CRBuildTask.rake();
        CRJob buildRake = new CRJob("build");
        buildRake.addTask(rakeTask);
        veryCustomGit = new CRGitMaterial("gitMaterial1", "dir1", false, false, null, Arrays.asList("externals", "tools"), "gitrepo", "feature12", true);

        buildStage = new CRStage("build");
        buildStage.addJob(buildRake);
        pipe1 = new CRPipeline("pipe1", "group1");
        pipe1.addStage(buildStage);
        pipe1.addMaterial(veryCustomGit);


        customPipeline = new CRPipeline("pipe2", "group1");
        customPipeline.addStage(buildStage);
        customPipeline.addMaterial(new CRDependencyMaterial("pipe1", "pipe1", "build", false));
        customPipeline.setLabelTemplate("foo-1.0-${COUNT}");
        customPipeline.setTimer(new CRTimer("0 15 10 * * ? *", false));

        invalidNoName = new CRPipeline(null, "group1");
        invalidNoName.addStage(buildStage);
        invalidNoName.addMaterial(veryCustomGit);

        invalidNoMaterial = new CRPipeline();
        invalidNoMaterial.setName("pipe4");
        invalidNoMaterial.setGroup("g1");
        invalidNoMaterial.addStage(buildStage);

        invalidNoGroup = new CRPipeline("name", null);
        invalidNoGroup.addMaterial(veryCustomGit);
        invalidNoGroup.addStage(buildStage);

        invalidNoStages = new CRPipeline();
        invalidNoStages.setName("pipe4");
        invalidNoStages.setGroup("g1");

        invalidNoNamedMaterials = new CRPipeline("pipe2", "group1");
        invalidNoNamedMaterials.addMaterial(veryCustomGit);
        invalidNoNamedMaterials.addMaterial(new CRDependencyMaterial("pipe1", "build", false));
        invalidNoNamedMaterials.addStage(buildStage);
        invalidNoNamedMaterials.setGroup("g1");
    }

    @Test
    public void shouldAppendPrettyLocationInErrors_WhenPipelineHasExplicitLocationField() {
        CRPipeline p = new CRPipeline();
        p.setName("pipe4");
        p.addMaterial(veryCustomGit);
        // plugin may voluntarily set this
        p.setLocation("pipe4.json");

        ErrorCollection errors = new ErrorCollection();
        p.getErrors(errors, "TEST");

        String fullError = errors.getErrorsAsText();

        assertThat(fullError, contains("pipe4.json; Pipeline pipe4"));
        assertThat(fullError, contains("Missing field 'group'."));
        assertThat(fullError, contains("Pipeline has no stages."));
    }

    @Test
    public void shouldCheckErrorsInMaterials() {
        CRPipeline p = new CRPipeline();
        p.setName("pipe4");

        CRGitMaterial invalidGit = new CRGitMaterial("gitMaterial1", "dir1", false, false, null, Arrays.asList("externals", "tools"), null, "feature12", true);
        p.addMaterial(invalidGit);
        // plugin may voluntarily set this
        p.setLocation("pipe4.json");

        ErrorCollection errors = new ErrorCollection();
        p.getErrors(errors, "TEST");

        String fullError = errors.getErrorsAsText();

        assertThat(fullError, contains("Pipeline pipe4; Git material"));
        assertThat(fullError, contains("Missing field 'url'."));
    }

    @Test
    public void shouldCheckMissingDestinationDirectoryWhenManySCMs() {
        CRPipeline p = new CRPipeline();
        p.setName("pipe4");

        CRGitMaterial simpleGit1 = new CRGitMaterial();
        simpleGit1.setUrl("url1");
        CRGitMaterial simpleGit2 = new CRGitMaterial();
        simpleGit2.setUrl("url2");

        p.addMaterial(simpleGit1);
        p.addMaterial(simpleGit2);

        ErrorCollection errors = new ErrorCollection();
        p.getErrors(errors, "TEST");

        String fullError = errors.getErrorsAsText();

        assertThat(fullError, contains("Pipeline pipe4; Git material"));
        assertThat(fullError, contains("Material must have destination directory when there are many SCM materials"));
    }

    @Test
    public void shouldCheckErrorsInStages() {
        CRPipeline p = new CRPipeline();
        p.setName("pipe4");
        // plugin may voluntarily set this
        p.setLocation("pipe4.json");

        CRStage invalidSameEnvironmentVariableTwice = new CRStage("bla");
        invalidSameEnvironmentVariableTwice.addEnvironmentVariable("key", "value1");
        invalidSameEnvironmentVariableTwice.addEnvironmentVariable("key", "value2");
        p.addStage(invalidSameEnvironmentVariableTwice);

        ErrorCollection errors = new ErrorCollection();
        p.getErrors(errors, "TEST");

        String fullError = errors.getErrorsAsText();

        assertThat(fullError, contains("Pipeline pipe4; Stage (bla)"));
        assertThat(fullError, contains("Stage has no jobs"));
        assertThat(fullError, contains("Environment variable key defined more than once"));
    }

    @Test
    public void shouldAddAnErrorWhenBothTemplateAndStagesAreDefined() throws Exception {
        CRPipeline crPipeline = new CRPipeline("p1", "g1");
        crPipeline.addMaterial(veryCustomGit);
        crPipeline.addStage(buildStage);
        crPipeline.setTemplate("Template");
        ErrorCollection errorCollection = new ErrorCollection();

        crPipeline.getErrors(errorCollection, "TEST");

        MatcherAssert.assertThat(errorCollection.getErrorCount(), is(1));
        MatcherAssert.assertThat(errorCollection.getErrorsAsText(), contains("Pipeline has to either define stages or template. Not both."));
    }

    @Test
    public void shouldAddAnErrorIfNeitherTemplateOrStagesAreDefined() throws Exception {
        ErrorCollection errorCollection = new ErrorCollection();
        CRPipeline crPipeline = new CRPipeline("p1", "g1");
        crPipeline.setLockBehavior(PipelineConfig.LOCK_VALUE_LOCK_ON_FAILURE);
        crPipeline.addMaterial(veryCustomGit);
        crPipeline.getErrors(errorCollection, "TEST");

        MatcherAssert.assertThat(errorCollection.getErrorsAsText(), contains("Pipeline has to define stages or template."));
    }

    @Test
    public void shouldAddAnErrorForDuplicateParameterNames() throws Exception {
        CRPipeline crPipeline = new CRPipeline("p1", "g1");
        crPipeline.setLockBehavior(PipelineConfig.LOCK_VALUE_LOCK_ON_FAILURE);
        crPipeline.addParameter(new CRParameter("param1", "value1"));
        crPipeline.addParameter(new CRParameter("param1", "value1"));
        crPipeline.addMaterial(veryCustomGit);
        ErrorCollection errors = new ErrorCollection();

        crPipeline.getErrors(errors, "TEST");

        MatcherAssert.assertThat(errors.getErrorsAsText(), contains("Param name 'param1' is not unique."));
    }

    @Test
    public void shouldAddAnErrorForDuplicateEnvironmentVariables() throws Exception {
        CRPipeline crPipeline = new CRPipeline("p1", "g1");
        crPipeline.setLockBehavior(PipelineConfig.LOCK_VALUE_LOCK_ON_FAILURE);
        crPipeline.addMaterial(veryCustomGit);
        crPipeline.addEnvironmentVariable("env1", "value1");
        crPipeline.addEnvironmentVariable("env1", "value2");
        ErrorCollection errors = new ErrorCollection();

        crPipeline.getErrors(errors, "TEST");

        MatcherAssert.assertThat(errors.getErrorsAsText(), contains("Environment variable env1 defined more than once"));
    }

    @Override
    public void addGoodExamples(Map<String, CRPipeline> examples) {
        examples.put("pipe1", pipe1);
        examples.put("customPipeline", customPipeline);
    }

    @Override
    public void addBadExamples(Map<String, CRPipeline> examples) {
        examples.put("invalidNoName", invalidNoName);
        examples.put("invalidNoMaterial", invalidNoMaterial);
        examples.put("invalidNoStages", invalidNoStages);
        examples.put("invalidNoNamedMaterials", invalidNoNamedMaterials);
        examples.put("invalidNoGroup", invalidNoGroup);
    }

    @Test
    public void shouldHandlePolymorphismWhenDeserializingJobs() {
        String json = gson.toJson(pipe1);

        CRPipeline deserializedValue = gson.fromJson(json, CRPipeline.class);

        CRMaterial git = deserializedValue.getMaterialByName("gitMaterial1");
        assertThat(git instanceof CRGitMaterial, is(true));
        assertThat(((CRGitMaterial) git).getBranch(), is("feature12"));
    }

    @Test
    public void shouldAddAnErrorIfLockBehaviorValueIsInvalid() throws Exception {
        CRPipeline validPipelineWithInvalidLockBehaviorOnly = new CRPipeline("p1", "g1");
        validPipelineWithInvalidLockBehaviorOnly.addMaterial(veryCustomGit);
        validPipelineWithInvalidLockBehaviorOnly.addStage(buildStage);
        validPipelineWithInvalidLockBehaviorOnly.setLockBehavior("INVALID_LOCK_VALUE");

        ErrorCollection errorCollection = new ErrorCollection();
        validPipelineWithInvalidLockBehaviorOnly.getErrors(errorCollection, "TEST");

        String expectedMessage = "Lock behavior has an invalid value (INVALID_LOCK_VALUE). Valid values are:";
        MatcherAssert.assertThat(errorCollection.getErrorCount(), is(1));
        MatcherAssert.assertThat(errorCollection.getOrCreateErrorList(validPipelineWithInvalidLockBehaviorOnly.getLocation("TEST")).get(0), contains(expectedMessage));
    }
}
