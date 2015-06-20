package com.thoughtworks.go.config;

import com.thoughtworks.go.helper.ConfigFileFixture;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.LogFixture;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Created by tomzo on 6/20/15.
 */
public class GoConfigDaoBasicTest extends GoConfigDaoBaseTest {

    public  GoConfigDaoBasicTest()
    {
        configHelper = new GoConfigFileHelper();
        goConfigDao = configHelper.getGoConfigFileDao();
        mergedGoConfig = configHelper.getCachedGoConfig();
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
        mergedGoConfig.save(ConfigFileFixture.VERSION_5, true);
        CruiseConfig cruiseConfig = goConfigDao.load();
        assertThat(cruiseConfig.getAllPipelineConfigs().size(), is(1));
        assertThat(cruiseConfig.getAllPipelineConfigs().get(0).name(), is(new CaseInsensitiveString("framework")));
    }
}
