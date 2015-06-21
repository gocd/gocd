package com.thoughtworks.go.config;


import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.domain.PipelineGroups;
import com.thoughtworks.go.helper.ConfigFileFixture;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.helper.PipelineMother;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.LogFixture;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.thoughtworks.go.config.PipelineConfigs.DEFAULT_GROUP;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * there is a risk that dao will misbehave when config is merged with remotes
 * Which is why this tests run on merged configuration
 * confirming that case that executes on sole file passes here as well.
 */
public class GoConfigDaoMergedTest extends GoConfigDaoBaseTest {

    public  GoConfigDaoMergedTest()
    {
        GoPartialConfig partials = mock(GoPartialConfig.class);
        PartialConfig[] parts = new PartialConfig[1];
        parts[0] = new PartialConfig(new PipelineGroups(
                PipelineConfigMother.createGroup("part1", PipelineConfigMother.pipelineConfig("remote-pipe"))));

        when(partials.lastPartials()).thenReturn(parts);

        configHelper = new GoConfigFileHelper(partials);
        goConfigDao = configHelper.getGoConfigFileDao();
        cachedGoConfig = configHelper.getCachedGoConfig();
    }

    @Before
    public void setup() throws Exception {
        configHelper.initializeConfigFile();
        logger = LogFixture.startListening();
    }

    @After
    public void teardown() throws Exception {
        logger.stopListening();
    }

    @Test
    public void shouldUpgradeOldXmlWhenRequestedTo() throws Exception {
        cachedGoConfig.save(ConfigFileFixture.VERSION_5, true);
        CruiseConfig cruiseConfig = goConfigDao.load();
        assertThat(cruiseConfig.getAllPipelineConfigs().size(), is(2));
        assertNotNull(cruiseConfig.getPipelineConfigByName(new CaseInsensitiveString("framework")));
    }

    @Test
    public void shouldFailWhenTryingToAddPipelineDefinedRemotely() throws Exception {
        PipelineConfig dupPipelineConfig = PipelineMother.twoBuildPlansWithResourcesAndSvnMaterialsAtUrl("remote-pipe", "ut",
                "www.spring.com");
        try {
            goConfigDao.addPipeline(dupPipelineConfig, DEFAULT_GROUP);
        }
        catch (RuntimeException ex)
        {
            assertThat(ex.getMessage(),is("You have defined multiple pipelines called 'remote-pipe'. Pipeline names must be unique."));
            return;
        }
        fail("Should have thrown");
    }
}
