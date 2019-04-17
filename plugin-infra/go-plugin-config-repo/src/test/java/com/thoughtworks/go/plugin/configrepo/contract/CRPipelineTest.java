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
package com.thoughtworks.go.plugin.configrepo.contract;

import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.plugin.configrepo.contract.material.CRDependencyMaterial;
import com.thoughtworks.go.plugin.configrepo.contract.material.CRGitMaterial;
import com.thoughtworks.go.plugin.configrepo.contract.material.CRMaterial;
import com.thoughtworks.go.plugin.configrepo.contract.tasks.CRBuildTask;
import org.hamcrest.MatcherAssert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Map;

import static com.thoughtworks.go.util.TestUtils.contains;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.junit.Assert.assertThat;
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

    public CRPipelineTest()
    {
        CRBuildTask rakeTask = CRBuildTask.rake();
        CRJob buildRake = new CRJob("build", rakeTask);
        veryCustomGit = new CRGitMaterial("gitMaterial1", "dir1", false,true, "gitrepo", "feature12", false, "externals", "tools");

        buildStage = new CRStage("build", buildRake);
        pipe1 = new CRPipeline("pipe1","group1",veryCustomGit, null, buildStage);


        customPipeline = new CRPipeline("pipe2","group1",veryCustomGit, null, buildStage);
        customPipeline.addMaterial(new CRDependencyMaterial("pipe1","pipe1","build"));
        customPipeline.setLabelTemplate("foo-1.0-${COUNT}");
        customPipeline.setMingle( new CRMingle("http://mingle.example.com","my_project"));
        customPipeline.setTimer(new CRTimer("0 15 10 * * ? *"));

        invalidNoName = new CRPipeline(null,"group1",veryCustomGit, "template", buildStage);
        invalidNoMaterial = new CRPipeline();
        invalidNoMaterial.setName("pipe4");
        invalidNoMaterial.setGroupName("g1");
        invalidNoMaterial.addStage(buildStage);

        invalidNoGroup = new CRPipeline("name",null,veryCustomGit, null, buildStage);

        invalidNoStages = new CRPipeline();
        invalidNoStages.setName("pipe4");
        invalidNoStages.setGroupName("g1");
        invalidNoStages.addMaterial(veryCustomGit);

        invalidNoNamedMaterials = new CRPipeline("pipe2","group1",veryCustomGit, null, buildStage);
        invalidNoNamedMaterials.addMaterial(new CRDependencyMaterial("pipe1","build"));
        invalidNoNamedMaterials.setGroupName("g1");
    }

    @Test
    public void shouldAppendPrettyLocationInErrors_WhenPipelineHasExplicitLocationField()
    {
        CRPipeline p = new CRPipeline();
        p.setName("pipe4");
        p.addMaterial(veryCustomGit);
        // plugin may voluntarily set this
        p.setLocation("pipe4.json");

        ErrorCollection errors = new ErrorCollection();
        p.getErrors(errors,"TEST");

        String fullError = errors.getErrorsAsText();

        assertThat(fullError,contains("pipe4.json; Pipeline pipe4"));
        assertThat(fullError,contains("Missing field 'group'."));
        assertThat(fullError,contains("Pipeline has no stages."));
    }

    @Test
    public void shouldCheckErrorsInMaterials()
    {
        CRPipeline p = new CRPipeline();
        p.setName("pipe4");

        CRGitMaterial invalidGit = new CRGitMaterial("gitMaterial1", "dir1", false,true, null, "feature12",false, "externals", "tools");
        p.addMaterial(invalidGit);
        // plugin may voluntarily set this
        p.setLocation("pipe4.json");

        ErrorCollection errors = new ErrorCollection();
        p.getErrors(errors,"TEST");

        String fullError = errors.getErrorsAsText();

        assertThat(fullError,contains("Pipeline pipe4; Git material"));
        assertThat(fullError,contains("Missing field 'url'."));
    }
    @Test
    public void shouldCheckMissingDestinationDirectoryWhenManySCMs()
    {
        CRPipeline p = new CRPipeline();
        p.setName("pipe4");

        CRGitMaterial simpleGit1 = new CRGitMaterial();
        simpleGit1.setUrl("url1");
        CRGitMaterial simpleGit2 = new CRGitMaterial();
        simpleGit2.setUrl("url2");

        p.addMaterial(simpleGit1);
        p.addMaterial(simpleGit2);

        ErrorCollection errors = new ErrorCollection();
        p.getErrors(errors,"TEST");

        String fullError = errors.getErrorsAsText();

        assertThat(fullError,contains("Pipeline pipe4; Git material"));
        assertThat(fullError,contains("Material must have destination directory when there are many SCM materials"));
    }

    @Test
    public void shouldCheckErrorsInStages()
    {
        CRPipeline p = new CRPipeline();
        p.setName("pipe4");
        // plugin may voluntarily set this
        p.setLocation("pipe4.json");

        CRStage invalidSameEnvironmentVariableTwice = new CRStage("bla");
        invalidSameEnvironmentVariableTwice.addEnvironmentVariable("key","value1");
        invalidSameEnvironmentVariableTwice.addEnvironmentVariable("key","value2");
        p.addStage(invalidSameEnvironmentVariableTwice);

        ErrorCollection errors = new ErrorCollection();
        p.getErrors(errors,"TEST");

        String fullError = errors.getErrorsAsText();

        assertThat(fullError,contains("Pipeline pipe4; Stage (bla)"));
        assertThat(fullError,contains("Stage has no jobs"));
        assertThat(fullError,contains("Environment variable key defined more than once"));
    }

    @Test
    public void shouldAddAnErrorWhenBothTemplateAndStagesAreDefined() throws Exception {
        CRPipeline crPipeline = new CRPipeline("p1", "g1", veryCustomGit, "template", buildStage);
        ErrorCollection errorCollection = new ErrorCollection();
        crPipeline.getErrors(errorCollection, "TEST");

        MatcherAssert.assertThat(errorCollection.getErrorCount(), is(1));
        MatcherAssert.assertThat(errorCollection.getErrorsAsText(), contains("Pipeline has to either define stages or template. Not both."));
    }

    @Test
    public void shouldAddAnErrorIfNeitherTemplateOrStagesAreDefined() throws Exception {
        ArrayList<CRMaterial> materials = new ArrayList<>();
        materials.add(veryCustomGit);
        CRPipeline crPipeline = new CRPipeline("p1", "g1", "label", PipelineConfig.LOCK_VALUE_LOCK_ON_FAILURE, null, null, null, new ArrayList<>(), materials, new ArrayList<>(), null, new ArrayList<>());
        ErrorCollection errorCollection = new ErrorCollection();
        crPipeline.getErrors(errorCollection, "TEST");

        MatcherAssert.assertThat(errorCollection.getErrorsAsText(), contains("Pipeline has to define stages or template."));
    }

    @Test
    public void shouldAddAnErrorForDuplicateParameterNames() throws Exception {
        ArrayList<CRMaterial> materials = new ArrayList<>();
        materials.add(veryCustomGit);

        ArrayList<CRParameter> crParameters = new ArrayList<>();
        crParameters.add(new CRParameter("param1", "value1"));
        crParameters.add(new CRParameter("param1", "value2"));
        CRPipeline crPipeline = new CRPipeline("p1", "g1", "label", PipelineConfig.LOCK_VALUE_LOCK_ON_FAILURE, null, null, null, new ArrayList<>(), materials, null, "t1", crParameters);
        ErrorCollection errors = new ErrorCollection();
        crPipeline.getErrors(errors, "TEST");

        MatcherAssert.assertThat(errors.getErrorsAsText(), contains("Param name 'param1' is not unique."));
    }

    @Test
    public void shouldAddAnErrorForDuplicateEnvironmentVariables() throws Exception {
        ArrayList<CRMaterial> materials = new ArrayList<>();
        materials.add(veryCustomGit);

        ArrayList<CREnvironmentVariable> crEnvironmentVariables = new ArrayList<>();
        crEnvironmentVariables.add(new CREnvironmentVariable("env1", "value1"));
        crEnvironmentVariables.add(new CREnvironmentVariable("env1", "value2"));
        CRPipeline crPipeline = new CRPipeline("p1", "g1", "label", PipelineConfig.LOCK_VALUE_LOCK_ON_FAILURE, null, null, null, crEnvironmentVariables, materials, null, "t1", new ArrayList<>());
        ErrorCollection errors = new ErrorCollection();
        crPipeline.getErrors(errors, "TEST");

        MatcherAssert.assertThat(errors.getErrorsAsText(), contains("Environment variable env1 defined more than once"));
    }

    @Override
    public void addGoodExamples(Map<String, CRPipeline> examples) {
        examples.put("pipe1",pipe1);
        examples.put("customPipeline",customPipeline);
    }

    @Override
    public void addBadExamples(Map<String, CRPipeline> examples) {
        examples.put("invalidNoName",invalidNoName);
        examples.put("invalidNoMaterial",invalidNoMaterial);
        examples.put("invalidNoStages",invalidNoStages);
        examples.put("invalidNoNamedMaterials",invalidNoNamedMaterials);
        examples.put("invalidNoGroup",invalidNoGroup);
    }

    @Test
    public void shouldHandlePolymorphismWhenDeserializingJobs()
    {
        String json = gson.toJson(pipe1);

        CRPipeline deserializedValue = gson.fromJson(json,CRPipeline.class);

        CRMaterial git = deserializedValue.getMaterialByName("gitMaterial1");
        assertThat(git instanceof CRGitMaterial,is(true));
        assertThat(((CRGitMaterial)git).getBranch(),is("feature12"));
    }

    @Test
    public void shouldAddAnErrorIfLockBehaviorValueIsInvalid() throws Exception {
        CRPipeline validPipelineWithInvalidLockBehaviorOnly = new CRPipeline("pipe1", "group1", null,
                "INVALID_LOCK_VALUE", null, null, null, emptyList(), asList(veryCustomGit), asList(buildStage), null, emptyList());

        ErrorCollection errorCollection = new ErrorCollection();
        validPipelineWithInvalidLockBehaviorOnly.getErrors(errorCollection, "TEST");

        String expectedMessage = "Lock behavior has an invalid value (INVALID_LOCK_VALUE). Valid values are:";
        MatcherAssert.assertThat(errorCollection.getErrorCount(), is(1));
        MatcherAssert.assertThat(errorCollection.getOrCreateErrorList(validPipelineWithInvalidLockBehaviorOnly.getLocation("TEST")).get(0), contains(expectedMessage));
    }
}
