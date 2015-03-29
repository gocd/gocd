package com.thoughtworks.go.config;

import com.thoughtworks.go.domain.config.ArtifactCleanupStrategy;
import com.thoughtworks.go.domain.config.PluginConfiguration;
import com.thoughtworks.go.domain.config.RepositoryMetadataStoreHelper;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.metrics.service.MetricsProbeService;
import com.thoughtworks.go.util.ConfigElementImplementationRegistryMother;
import com.thoughtworks.go.util.GoConstants;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.ByteArrayOutputStream;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ArtifactCleanupStrategyConfigTest {
    private MagicalGoConfigXmlLoader xmlLoader;
    private ConfigCache configCache = new ConfigCache();
    private MetricsProbeService metricsProbeService;
    private ByteArrayOutputStream output;
    private MagicalGoConfigXmlWriter xmlWriter;

    @Before
    public void setup() throws Exception {
        RepositoryMetadataStoreHelper.clear();
        metricsProbeService = Mockito.mock(MetricsProbeService.class);
        output = new ByteArrayOutputStream();
        xmlLoader = new MagicalGoConfigXmlLoader(configCache, ConfigElementImplementationRegistryMother.withNoPlugins(), metricsProbeService);
        xmlWriter = new MagicalGoConfigXmlWriter(configCache, ConfigElementImplementationRegistryMother.withNoPlugins(), metricsProbeService);

    }

    @Test
    public void shouldSuccessfullyLoadStageConfigWithArtifactCleanupStrategy() throws Exception {
        String xml = "<cruise schemaVersion='" + GoConstants.CONFIG_SCHEMA_VERSION + "'>\n"
                + "<pipelines group=\"group_name\">\n"
                + "  <pipeline name=\"new_name\">\n"
                + "    <materials>\n"
                + "      <git url='xyz' />\n"
                + "    </materials>\n"
                + "    <stage name=\"stage_name\">\n"
                + "      <artifactCleanupStrategy>"
                + "     		<pluginConfiguration id='plugin-id' version='1.0'/>"
                + "      </artifactCleanupStrategy>"
                + "      <jobs>\n"
                + "        <job name=\"job_name\" />\n"
                + "      </jobs>\n"
                + "    </stage>\n"
                + "  </pipeline>\n"
                + "</pipelines></cruise>";

        GoConfigHolder goConfigHolder = xmlLoader.loadConfigHolder(xml);
        StageConfig stageConfig = goConfigHolder.config.getPipelineConfigByName(new CaseInsensitiveString("new_name")).getFirstStageConfig();
        assertThat(stageConfig.getArtifactCleanupStrategy().getPluginConfiguration().getId(), is("plugin-id"));
        assertThat(stageConfig.getArtifactCleanupStrategy().getPluginConfiguration().getVersion(), is("1.0"));
    }

    @Test
    public void shouldSuccessfullyLoadServerConfigWithArtifactCleanupStrategy() throws Exception {
        String xml = "<cruise schemaVersion='" + GoConstants.CONFIG_SCHEMA_VERSION + "'>\n"
                + "<server artifactsdir='artifacts'>"
                + "      <artifactCleanupStrategy>"
                + "     		<pluginConfiguration id='plugin-id' version='1.0'/>"
                + "      </artifactCleanupStrategy>"
                + " </server>"
                + "</cruise>";
        GoConfigHolder goConfigHolder = xmlLoader.loadConfigHolder(xml);
        ArtifactCleanupStrategy artifactCleanupStrategy = goConfigHolder.config.server().getArtifactCleanupStrategy();
        assertThat(artifactCleanupStrategy.getPluginConfiguration().getId(), is("plugin-id"));
        assertThat(artifactCleanupStrategy.getPluginConfiguration().getVersion(), is("1.0"));
    }

    @Test
    public void shouldWriteCruiseConfigWithArtifactCleanupStrategyUnderStageConfig() throws Exception {
        CruiseConfig config = GoConfigMother.configWithPipelines("pipeline1");
        StageConfig stageConfig = config.getPipelineConfigByName(new CaseInsensitiveString("pipeline1")).getFirstStageConfig();
        stageConfig.setArtifactCleanupStrategy(new ArtifactCleanupStrategy(new PluginConfiguration("plugin-id", "1.0")));
        xmlWriter.write(config, output, false);

        CruiseConfig newConfig = xmlLoader.loadConfigHolder(output.toString()).config;
        StageConfig updatedStageConfig = newConfig.getPipelineConfigByName(new CaseInsensitiveString("pipeline1")).getFirstStageConfig();
        assertThat(updatedStageConfig.getArtifactCleanupStrategy().getPluginConfiguration().getId(), is("plugin-id"));
        assertThat(updatedStageConfig.getArtifactCleanupStrategy().getPluginConfiguration().getVersion(), is("1.0"));
    }


    @Test
    public void shouldWriteCruiseConfigWithArtifactCleanupStrategyUnderServerConfig() throws Exception {
        CruiseConfig config = GoConfigMother.configWithPipelines("pipeline1");
        config.server().ensureServerIdExists();
        config.server().setArtifactCleanupStrategy(new ArtifactCleanupStrategy(new PluginConfiguration("plugin-id", "1.0")));
        xmlWriter.write(config, output, false);
        System.out.println(output.toString());

        CruiseConfig newConfig = xmlLoader.loadConfigHolder(output.toString()).config;
        ArtifactCleanupStrategy artifactCleanupStrategy = newConfig.server().getArtifactCleanupStrategy();
        assertThat(artifactCleanupStrategy.getPluginConfiguration().getId(), is("plugin-id"));
        assertThat(artifactCleanupStrategy.getPluginConfiguration().getVersion(), is("1.0"));
    }

}


