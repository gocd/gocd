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
package com.thoughtworks.go.config.parts;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.domain.PipelineGroups;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.helper.EnvironmentConfigMother;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.util.ConfigElementImplementationRegistryMother;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.TestFileUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class XmlPartialConfigProviderTest {

    private ConfigCache configCache = new ConfigCache();
    private MagicalGoConfigXmlLoader xmlLoader;
    private XmlPartialConfigProvider xmlPartialProvider;
    private MagicalGoConfigXmlWriter xmlWriter;
    private  PartialConfigHelper helper;
    private File baseFolder;
    private  File tmpFolder;

    @Before
    public void SetUp()
    {
        baseFolder = TestFileUtil.createTempFolder("test");

        xmlLoader = new MagicalGoConfigXmlLoader(configCache, ConfigElementImplementationRegistryMother.withNoPlugins());
        xmlPartialProvider = new XmlPartialConfigProvider(xmlLoader);

        xmlWriter = new MagicalGoConfigXmlWriter(configCache, ConfigElementImplementationRegistryMother.withNoPlugins());

        tmpFolder = TestFileUtil.createTestFolder(baseFolder, "partialTestDir");
        helper = new PartialConfigHelper(xmlWriter, tmpFolder);
    }

    @After
    public void tearDown() throws IOException {
        FileUtil.deleteDirectoryNoisily(baseFolder);
    }

    @Test
    public void shouldParseFileWithOnePipeline() throws Exception
    {
        GoConfigMother mother = new GoConfigMother();
        PipelineConfig pipe1 = mother.cruiseConfigWithOnePipelineGroup().getAllPipelineConfigs().get(0);

        File file = helper.addFileWithPipeline("pipe1.gocd.xml", pipe1);

        PartialConfig part = xmlPartialProvider.parseFile(file);
        PipelineConfig pipeRead = part.getGroups().get(0).get(0);
        assertThat(pipeRead,is(pipe1));
    }

    @Test
    public void shouldParseFileWithOnePipelineGroup() throws Exception
    {
        GoConfigMother mother = new GoConfigMother();
        PipelineConfigs group1 = mother.cruiseConfigWithOnePipelineGroup().getGroups().get(0);

        File file = helper.addFileWithPipelineGroup("group1.gocd.xml", group1);

        PartialConfig part = xmlPartialProvider.parseFile(file);
        PipelineConfigs groupRead = part.getGroups().get(0);
        assertThat(groupRead,is(group1));
        assertThat(groupRead.size(),is(group1.size()));
        assertThat(groupRead.get(0),is(group1.get(0)));
    }
    @Test
    public void shouldParseFileWithOneEnvironment() throws Exception
    {
        EnvironmentConfig env = EnvironmentConfigMother.environment("dev");

        File file = helper.addFileWithEnvironment("dev-env.gocd.xml", env);

        PartialConfig part = xmlPartialProvider.parseFile(file);

        EnvironmentsConfig loadedEnvs = part.getEnvironments();
        assertThat(loadedEnvs.size(),is(1));
        assertThat(loadedEnvs.get(0),is(env));
    }


    @Test
    public void shouldLoadDirectoryWithOnePipeline() throws Exception
    {
        GoConfigMother mother = new GoConfigMother();
        PipelineConfig pipe1 = mother.cruiseConfigWithOnePipelineGroup().getAllPipelineConfigs().get(0);

        helper.addFileWithPipeline("pipe1.gocd.xml", pipe1);

        PartialConfig part = xmlPartialProvider.load(tmpFolder,mock(PartialConfigLoadContext.class));
        PipelineConfig pipeRead = part.getGroups().get(0).get(0);
        assertThat(pipeRead,is(pipe1));
    }

    @Test
    public void shouldLoadDirectoryWithOnePipelineGroup() throws Exception
    {
        GoConfigMother mother = new GoConfigMother();
        PipelineConfigs group1 = mother.cruiseConfigWithOnePipelineGroup().getGroups().get(0);

        helper.addFileWithPipelineGroup("group1.gocd.xml", group1);

        PartialConfig part = xmlPartialProvider.load(tmpFolder, mock(PartialConfigLoadContext.class));
        PipelineConfigs groupRead = part.getGroups().get(0);
        assertThat(groupRead,is(group1));
        assertThat(groupRead.size(),is(group1.size()));
        assertThat(groupRead.get(0),is(group1.get(0)));
    }

    @Test
    public void shouldLoadDirectoryWithTwoPipelineGroupsAndEnvironment() throws Exception
    {
        GoConfigMother mother = new GoConfigMother();
        PipelineGroups groups = mother.cruiseConfigWithTwoPipelineGroups().getGroups();
        EnvironmentConfig env = EnvironmentConfigMother.environment("dev");

        helper.addFileWithPipelineGroup("group1.gocd.xml", groups.get(0));
        helper.addFileWithPipelineGroup("group2.gocd.xml", groups.get(1));
        helper.addFileWithEnvironment("dev-env.gocd.xml", env);

        PartialConfig part = xmlPartialProvider.load(tmpFolder, mock(PartialConfigLoadContext.class));

        PipelineGroups groupsRead = part.getGroups();
        assertThat(groupsRead.size(),is(2));

        EnvironmentsConfig loadedEnvs = part.getEnvironments();
        assertThat(loadedEnvs.size(),is(1));
        assertThat(loadedEnvs.get(0),is(env));
    }

    @Test
    public void shouldGetFilesToLoadMatchingPattern() throws Exception {
        GoConfigMother mother = new GoConfigMother();
        PipelineConfig pipe1 = mother.cruiseConfigWithOnePipelineGroup().getAllPipelineConfigs().get(0);

        File file1 = helper.addFileWithPipeline("pipe1.gocd.xml", pipe1);
        File file2 = helper.addFileWithPipeline("pipe1.gcd.xml", pipe1);
        File file3 = helper.addFileWithPipeline("subdir/pipe1.gocd.xml", pipe1);
        File file4 = helper.addFileWithPipeline("subdir/sub/pipe1.gocd.xml", pipe1);

        File[] matchingFiles = xmlPartialProvider.getFiles(tmpFolder, mock(PartialConfigLoadContext.class));

        File[] expected = new File[] {file1, file3, file4};
        assertArrayEquals("Matched files are: " + Arrays.asList(matchingFiles).toString(), expected, matchingFiles);
    }

    @Test
    public void shouldUseExplicitPatternWhenProvided() throws Exception {
        GoConfigMother mother = new GoConfigMother();
        PipelineConfig pipe1 = mother.cruiseConfigWithOnePipelineGroup().getAllPipelineConfigs().get(0);

        File file1 = helper.addFileWithPipeline("pipe1.myextension", pipe1);
        File file2 = helper.addFileWithPipeline("pipe1.gcd.xml", pipe1);
        File file3 = helper.addFileWithPipeline("subdir/pipe1.gocd.xml", pipe1);
        File file4 = helper.addFileWithPipeline("subdir/sub/pipe1.gocd.xml", pipe1);

        PartialConfigLoadContext context = mock(PartialConfigLoadContext.class);
        Configuration configs = new Configuration();
        configs.addNewConfigurationWithValue("pattern","*.myextension",false);
        when(context.configuration()).thenReturn(configs);

        File[] matchingFiles = xmlPartialProvider.getFiles(tmpFolder, context);

        File[] expected = new File[1];
        expected[0] = file1;
        assertArrayEquals(expected,matchingFiles);
    }

    @Test
    public void shouldFailToLoadDirectoryWithDuplicatedPipeline() throws Exception
    {
        GoConfigMother mother = new GoConfigMother();
        PipelineConfig pipe1 = mother.cruiseConfigWithOnePipelineGroup().getAllPipelineConfigs().get(0);

        helper.addFileWithPipeline("pipe1.gocd.xml", pipe1);
        helper.addFileWithPipeline("pipedup.gocd.xml", pipe1);

        try {
            PartialConfig part = xmlPartialProvider.load(tmpFolder, mock(PartialConfigLoadContext.class));
        }
        catch (Exception ex)
        {
            assertThat(ex.getMessage(),is("You have defined multiple pipelines called 'pipeline1'. Pipeline names must be unique."));
            return;
        }
        fail("should have thrown");
    }

    @Test
    public void shouldFailToLoadDirectoryWithNonXmlFormat() throws Exception
    {
        String content = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                + "<cruise xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"cruise-config.xsd\" schemaVersion=\"38\">\n"
                + "/cruise>";// missing '<'

        helper.writeFileWithContent("bad.gocd.xml",content);

        try
        {
            PartialConfig part = xmlPartialProvider.load(tmpFolder, mock(PartialConfigLoadContext.class));
        }
        catch (RuntimeException ex)
        {
            return;
        }
        fail("should have thrown");
    }
}
