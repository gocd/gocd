package com.thoughtworks.go.config.parts;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.metrics.service.MetricsProbeService;
import com.thoughtworks.go.util.ConfigElementImplementationRegistryMother;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.TestFileUtil;
import org.jdom.input.JDOMParseException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.contains;
import static org.mockito.Mockito.mock;

/**
 * Created by tomzo on 6/22/15.
 */
public class XmlPartialConfigProviderTest {

    private ConfigCache configCache = new ConfigCache();
    private MetricsProbeService metricsProbeService;
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

        metricsProbeService = mock(MetricsProbeService.class);
        xmlLoader = new MagicalGoConfigXmlLoader(configCache, ConfigElementImplementationRegistryMother.withNoPlugins(), metricsProbeService);
        xmlPartialProvider = new XmlPartialConfigProvider(xmlLoader);

        xmlWriter = new MagicalGoConfigXmlWriter(configCache, ConfigElementImplementationRegistryMother.withNoPlugins(), metricsProbeService);

        tmpFolder = TestFileUtil.createTestFolder(baseFolder, "partialTestDir");
        helper = new PartialConfigHelper(xmlWriter, tmpFolder);
    }

    @After
    public void tearDown() {
        FileUtil.deleteFolder(baseFolder);
    }

    @Test
    public void shouldParseFileWithOnePipeline() throws Exception
    {
        GoConfigMother mother = new GoConfigMother();
        PipelineConfig pipe1 = mother.cruiseConfigWithOnePipelineGroup().getAllPipelineConfigs().get(0);

        File file = helper.addFileWithPipeline("pipe1.gocd.xml", pipe1);

        PartialConfig part = xmlPartialProvider.ParseFile(file);
        PipelineConfig pipeRead = part.getGroups().get(0).get(0);
        assertThat(pipeRead,is(pipe1));
    }

    @Test
    public void shouldParseFileWithOnePipelineGroup() throws Exception
    {
        GoConfigMother mother = new GoConfigMother();
        PipelineConfigs group1 = mother.cruiseConfigWithOnePipelineGroup().getGroups().get(0);

        File file = helper.addFileWithPipelineGroup("group1.gocd.xml", group1);

        PartialConfig part = xmlPartialProvider.ParseFile(file);
        PipelineConfigs groupRead = part.getGroups().get(0);
        assertThat(groupRead,is(group1));
        assertThat(groupRead.size(),is(group1.size()));
        assertThat(groupRead.get(0),is(group1.get(0)));
    }

    @Test
    public void shouldLoadDirectoryWithOnePipeline() throws Exception
    {
        GoConfigMother mother = new GoConfigMother();
        PipelineConfig pipe1 = mother.cruiseConfigWithOnePipelineGroup().getAllPipelineConfigs().get(0);

        helper.addFileWithPipeline("pipe1.gocd.xml", pipe1);

        PartialConfig part = xmlPartialProvider.Load(tmpFolder,mock(PartialConfigLoadContext.class));
        PipelineConfig pipeRead = part.getGroups().get(0).get(0);
        assertThat(pipeRead,is(pipe1));
    }

    @Test
    public void shouldLoadDirectoryWithOnePipelineGroup() throws Exception
    {
        GoConfigMother mother = new GoConfigMother();
        PipelineConfigs group1 = mother.cruiseConfigWithOnePipelineGroup().getGroups().get(0);

        helper.addFileWithPipelineGroup("group1.gocd.xml", group1);

        PartialConfig part = xmlPartialProvider.Load(tmpFolder, mock(PartialConfigLoadContext.class));
        PipelineConfigs groupRead = part.getGroups().get(0);
        assertThat(groupRead,is(group1));
        assertThat(groupRead.size(),is(group1.size()));
        assertThat(groupRead.get(0),is(group1.get(0)));
    }

    @Test
    public void shouldGetFilesToLoadMatchingPattern() throws Exception {
        GoConfigMother mother = new GoConfigMother();
        PipelineConfig pipe1 = mother.cruiseConfigWithOnePipelineGroup().getAllPipelineConfigs().get(0);

        File file1 = helper.addFileWithPipeline("pipe1.gocd.xml", pipe1);
        File file2 = helper.addFileWithPipeline("pipe1.gcd.xml", pipe1);
        File file3 = helper.addFileWithPipeline("subdir/pipe1.gocd.xml", pipe1);
        File file4 = helper.addFileWithPipeline("subdir/sub/pipe1.gocd.xml", pipe1);

        File[] matchingFiles = xmlPartialProvider.getFiles(tmpFolder, "**/*.gocd.xml");

        File[] expected = new File[3];
        expected[0] = file1;
        expected[1] = file3;
        expected[2] = file4;
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
            PartialConfig part = xmlPartialProvider.Load(tmpFolder, mock(PartialConfigLoadContext.class));
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
            PartialConfig part = xmlPartialProvider.Load(tmpFolder, mock(PartialConfigLoadContext.class));
        }
        catch (JDOMParseException ex)
        {
            return;
        }
        fail("should have thrown");
    }
}
