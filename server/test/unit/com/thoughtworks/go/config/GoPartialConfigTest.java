package com.thoughtworks.go.config;

import com.thoughtworks.go.config.materials.ScmMaterialConfig;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.ConfigReposConfig;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.server.materials.ScmMaterialCheckoutService;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by tomzo on 6/18/15.
 */
public class GoPartialConfigTest {

    private GoConfigPluginService configPluginService;
    private GoConfigWatchList configWatchList;
    private ScmMaterialCheckoutService checkoutService;
    private PartialConfigProvider plugin;

    private GoRepoConfigDataSource repoConfigDataSource;

    private BasicCruiseConfig cruiseConfig ;

    private GoPartialConfig partialConfig;

    File folder = new File("dir");

    @Before
    public void SetUp()
    {
        configPluginService = mock(GoConfigPluginService.class);
        plugin = mock(PartialConfigProvider.class);
        when(configPluginService.partialConfigProviderFor(any(ConfigRepoConfig.class))).thenReturn(plugin);

        cruiseConfig = new BasicCruiseConfig();
        CachedFileGoConfig fileMock = mock(CachedFileGoConfig.class);
        when(fileMock.currentConfig()).thenReturn(cruiseConfig);

        configWatchList = new GoConfigWatchList(fileMock);
        checkoutService = mock(ScmMaterialCheckoutService.class);

        repoConfigDataSource = new GoRepoConfigDataSource(configWatchList,configPluginService,checkoutService);

        partialConfig = new GoPartialConfig(repoConfigDataSource,configWatchList);
    }

    @Test
    public void shouldReturnEmptyArrayWhenWatchListEmpty()
    {
        assertThat(partialConfig.lastPartials().length,is(0));
    }
    @Test
    public void shouldReturnEmptyArrayWhenNothingParsedYet_AndWatchListNotEmpty()
    {
        SetOneConfigRepo();

        assertThat(partialConfig.lastPartials(),is(new PartialConfig[0]));
    }

    private ScmMaterialConfig SetOneConfigRepo() {
        ScmMaterialConfig material = new GitMaterialConfig("http://my.git");
        cruiseConfig.setConfigRepos(new ConfigReposConfig(new ConfigRepoConfig(material,"myplugin")));
        configWatchList.onConfigChange(cruiseConfig);
        return material;
    }

    @Test
    public void shouldReturnLatestPartialAfterCheckout_AndWatchListNotEmpty() throws Exception
    {
        ScmMaterialConfig material = SetOneConfigRepo();

        PartialConfig part = new PartialConfig();
        when(plugin.Load(any(File.class),any(PartialConfigLoadContext.class))).thenReturn(part);

        repoConfigDataSource.onCheckoutComplete(material,folder,"7a8f");

        assertThat(partialConfig.lastPartials().length, is(1));
        assertThat(partialConfig.lastPartials()[0], is(part));
    }
    @Test
    public void shouldReturnEmptyArray_WhenFirstParsingFailed() throws Exception
    {
        ScmMaterialConfig material = SetOneConfigRepo();

        PartialConfig part = new PartialConfig();
        when(plugin.Load(any(File.class),any(PartialConfigLoadContext.class)))
                .thenThrow(new RuntimeException("Failed parsing"));

        repoConfigDataSource.onCheckoutComplete(material,folder,"7a8f");

        assertThat(repoConfigDataSource.latestParseHasFailedForMaterial(material),is(true));

        assertThat(partialConfig.lastPartials().length,is(0));
    }
    @Test
    public void shouldReturnFirstPartial_WhenFirstParsedSucceed_ButSecondFailed() throws Exception
    {
        ScmMaterialConfig material = SetOneConfigRepo();

        PartialConfig part = new PartialConfig();
        when(plugin.Load(any(File.class),any(PartialConfigLoadContext.class))).thenReturn(part);

        repoConfigDataSource.onCheckoutComplete(material,folder,"7a8f");

        when(plugin.Load(any(File.class),any(PartialConfigLoadContext.class)))
                .thenThrow(new RuntimeException("Failed parsing"));
        repoConfigDataSource.onCheckoutComplete(material,folder,"6354");

        assertThat(repoConfigDataSource.latestParseHasFailedForMaterial(material),is(true));

        assertThat(partialConfig.lastPartials().length, is(1));
        assertThat(partialConfig.lastPartials()[0], is(part));
    }

    @Test
    public void shouldRemovePartialWhenNoLongerInWatchList() throws Exception
    {
        ScmMaterialConfig material = SetOneConfigRepo();

        PartialConfig part = new PartialConfig();
        when(plugin.Load(any(File.class),any(PartialConfigLoadContext.class))).thenReturn(part);

        repoConfigDataSource.onCheckoutComplete(material,folder,"7a8f");

        assertThat(partialConfig.lastPartials().length, is(1));
        assertThat(partialConfig.lastPartials()[0], is(part));

        // we change current configuration
        ScmMaterialConfig othermaterial = new GitMaterialConfig("http://myother.git");
        cruiseConfig.setConfigRepos(new ConfigReposConfig(new ConfigRepoConfig(othermaterial,"myplugin")));
        configWatchList.onConfigChange(cruiseConfig);

        assertThat(partialConfig.lastPartials().length,is(0));
    }

    @Test
    public void shouldListenForConfigRepoListChanges()
    {
        assertTrue(repoConfigDataSource.hasListener(partialConfig));
    }
    @Test
    public void shouldListenForCompletedParsing()
    {
        assertTrue(configWatchList.hasListener(partialConfig));
    }
}
