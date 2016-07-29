/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.domain;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.TestFileUtil;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.thoughtworks.go.helper.EnvironmentVariablesConfigMother.env;
import static com.thoughtworks.go.utils.SerializationTester.serializeAndDeserialize;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.*;

public class DefaultJobPlanTest {

    private File workingFolder;
    private File toClean;

    @Before
    public void setUp() throws IOException {
        workingFolder = TestFileUtil.createTempFolder("workingFolder");
        File file = new File(workingFolder, "cruise-output/log.xml");
        file.getParentFile().mkdirs();
        file.createNewFile();
    }

    @After
    public void tearDown() {
        FileUtil.deleteFolder(workingFolder);
        FileUtils.deleteQuietly(toClean);
    }

    @Test
    public void shouldMatchResourcesIfBuildPlanHasNoResources() {
        DefaultJobPlan plan = new DefaultJobPlan(new Resources(), new ArtifactPlans(), new ArtifactPropertiesGenerators(), 0, null, null, new EnvironmentVariablesConfig(), new EnvironmentVariablesConfig(), null);
        Resources agentResources = new Resources(new Resource("Foo"));
        assertTrue(plan.match(agentResources));
    }

    @Test
    public void shouldMatchIfBuildPlanAndAgentHaveSameResources() {
        DefaultJobPlan plan = new DefaultJobPlan(new Resources(new Resource("Foo")), new ArtifactPlans(), new ArtifactPropertiesGenerators(), 0, null, null, new EnvironmentVariablesConfig(), new EnvironmentVariablesConfig(), null);
        assertTrue(plan.match(new Resources(new Resource("Foo"))));
    }

    @Test
    public void shouldNotMatchIfAgentDonotContainBuildPlanResources() {
        DefaultJobPlan plan = new DefaultJobPlan(new Resources(new Resource("Foo")), new ArtifactPlans(), new ArtifactPropertiesGenerators(), 0, null, null, new EnvironmentVariablesConfig(), new EnvironmentVariablesConfig(), null);
        assertFalse(plan.match(new Resources(new Resource("Bar"))));
    }

    @Test
    public void shouldMatchIfAgentAndBuildPlanResourcesIrrespectiveOfOrder() {
        DefaultJobPlan plan = new DefaultJobPlan(new Resources(new Resource("Foo"), new Resource("Bar"), new Resource("car")), new ArtifactPlans(), new ArtifactPropertiesGenerators(), 0, null, null, new EnvironmentVariablesConfig(), new EnvironmentVariablesConfig(), null);
        assertTrue(plan.match(
                new Resources(new Resource("Bar"), new Resource("car"), new Resource("Foo"))));
    }

    @Test
    public void shouldMatchIfBothAgentAndBuildPlanHaveNotResources() {
        DefaultJobPlan plan = new DefaultJobPlan(new Resources(), new ArtifactPlans(), new ArtifactPropertiesGenerators(), 0, null, null, new EnvironmentVariablesConfig(), new EnvironmentVariablesConfig(), null);

        assertTrue(plan.match(new Resources()));
    }

    @Test
    public void shouldMergeTestReportFilesAndUploadResult() throws Exception {
        ArtifactPlans artifactPlans = new ArtifactPlans();
        DefaultJobPlan plan = new DefaultJobPlan(new Resources(), artifactPlans, new ArtifactPropertiesGenerators(), -1, null, null, new EnvironmentVariablesConfig(), new EnvironmentVariablesConfig(), null);
        artifactPlans.add(new TestArtifactPlan("test1", "test"));
        artifactPlans.add(new TestArtifactPlan("test2", "test"));

        final File firstTestFolder = prepareTestFolder(workingFolder, "test1");
        final File secondTestFolder = prepareTestFolder(workingFolder, "test2");

        StubGoPublisher publisher = new StubGoPublisher();
        plan.publishArtifacts(publisher, workingFolder);

        publisher.assertPublished(firstTestFolder.getAbsolutePath(), "test");
        publisher.assertPublished(secondTestFolder.getAbsolutePath(), "test");
        publisher.assertPublished("result", "testoutput");
        publisher.assertPublished("result" + File.separator + "index.html", "testoutput");
    }

    @Test
    public void shouldReportErrorWithTestArtifactSrcWhenUploadFails() throws Exception {
        ArtifactPlans artifactPlans = new ArtifactPlans();
        DefaultJobPlan plan = new DefaultJobPlan(new Resources(), artifactPlans, new ArtifactPropertiesGenerators(), -1, null, null, new EnvironmentVariablesConfig(), new EnvironmentVariablesConfig(), null);
        artifactPlans.add(new TestArtifactPlan("test1", "test"));
        artifactPlans.add(new TestArtifactPlan("test2", "test"));

        prepareTestFolder(workingFolder, "test1");
        prepareTestFolder(workingFolder, "test2");

        StubGoPublisher publisherThatShouldFail = new StubGoPublisher(true);
        try {
            plan.publishArtifacts(publisherThatShouldFail, workingFolder);
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("Failed to upload [test1, test2]"));
        }
    }

    @Test
    public void shouldUploadFilesCorrectly() throws Exception {
        ArtifactPlans artifactPlans = new ArtifactPlans();
        final File src1 = TestFileUtil.createTestFolder(workingFolder, "src1");
        TestFileUtil.createTestFile(src1, "test.txt");
        artifactPlans.add(new ArtifactPlan(src1.getName(), "dest"));
        final File src2 = TestFileUtil.createTestFolder(workingFolder, "src2");
        TestFileUtil.createTestFile(src1, "test.txt");

        artifactPlans.add(new ArtifactPlan(src2.getName(), "test"));
        StubGoPublisher publisher = new StubGoPublisher();
        DefaultJobPlan plan = new DefaultJobPlan(new Resources(), artifactPlans, new ArtifactPropertiesGenerators(), -1, null, null, new EnvironmentVariablesConfig(), new EnvironmentVariablesConfig(), null);


        plan.publishArtifacts(publisher, workingFolder);

        Map<File, String> expectedFiles = new HashMap<File, String>() {
            {
                put(src1, "dest");
                put(src2, "test");
            }
        };
        assertThat(publisher.publishedFiles(), is(expectedFiles));
    }

    @Test
    public void shouldUploadFilesWhichMathedWildCard() throws Exception {
        ArtifactPlans artifactPlans = new ArtifactPlans();
        final File src1 = TestFileUtil.createTestFolder(workingFolder, "src1");
        final File testFile1 = TestFileUtil.createTestFile(src1, "test1.txt");
        final File testFile2 = TestFileUtil.createTestFile(src1, "test2.txt");
        final File testFile3 = TestFileUtil.createTestFile(src1, "readme.pdf");
        artifactPlans.add(new ArtifactPlan(src1.getName() + "/*", "dest"));
        StubGoPublisher publisher = new StubGoPublisher();

        DefaultJobPlan plan = new DefaultJobPlan(new Resources(), artifactPlans, new ArtifactPropertiesGenerators(), -1, null, null, new EnvironmentVariablesConfig(), new EnvironmentVariablesConfig(), null);

        plan.publishArtifacts(publisher, workingFolder);

        Map<File, String> expectedFiles = new HashMap<File, String>() {
            {
                put(testFile1, "dest");
                put(testFile2, "dest");
                put(testFile3, "dest");
            }
        };
        assertThat(publisher.publishedFiles(), is(expectedFiles));
    }

    @Test
    public void shouldApplyEnvironmentVariablesWhenRunningTheJob() {
        EnvironmentVariablesConfig variables = new EnvironmentVariablesConfig();
        variables.add("VARIABLE_NAME", "variable value");
        DefaultJobPlan plan = new DefaultJobPlan(new Resources(), new ArtifactPlans(), new ArtifactPropertiesGenerators(), -1, null, null,
                variables, new EnvironmentVariablesConfig(), null);

        EnvironmentVariableContext variableContext = new EnvironmentVariableContext();
        plan.applyTo(variableContext);
        assertThat(variableContext.getProperty("VARIABLE_NAME"), is("variable value"));
    }

    private File prepareTestFolder(File workingFolder, String folderName) throws Exception {
        File testFolder = TestFileUtil.createTestFolder(workingFolder, folderName);
        File testFile = TestFileUtil.createTestFile(testFolder, "testFile.xml");
        String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<testsuite errors=\"0\" failures=\"0\" tests=\"7\" time=\"0.429\" >\n"
                + "<testcase/>\n"
                + "</testsuite>\n";
        FileUtil.writeContentToFile(content, testFile);
        return testFolder;
    }

    @Test
    public void shouldBeAbleToSerializeAndDeserialize() throws ClassNotFoundException, IOException {
        DefaultJobPlan original = new DefaultJobPlan(new Resources(), new ArtifactPlans(),
                new ArtifactPropertiesGenerators(), 0, new JobIdentifier(), "uuid", new EnvironmentVariablesConfig(), new EnvironmentVariablesConfig(), null);
        DefaultJobPlan clone = (DefaultJobPlan) serializeAndDeserialize(original);
        assertThat(clone,is(original));
    }

    @Test
    public void shouldRespectTriggerVariablesOverConfigVariables() {
        DefaultJobPlan original = new DefaultJobPlan(new Resources(), new ArtifactPlans(),
                new ArtifactPropertiesGenerators(), 0, new JobIdentifier(), "uuid", env(new String[]{"blah","foo"},new String[]{"value","bar"}), new EnvironmentVariablesConfig(), null);
        original.setTriggerVariables(env(new String[]{"blah","another"},new String[]{"override","anotherValue"}));
        EnvironmentVariableContext variableContext = new EnvironmentVariableContext();
        original.applyTo(variableContext);
        assertThat(variableContext.getProperty("blah"),is("override"));
        assertThat(variableContext.getProperty("foo"),is("bar"));
        //becuase its a security issue to let operator set values for unconfigured variables
        assertThat(variableContext.getProperty("another"),is(nullValue()));
    }
}
