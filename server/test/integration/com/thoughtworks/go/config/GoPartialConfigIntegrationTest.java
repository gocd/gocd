/*
 * Copyright 2016 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thoughtworks.go.config;

import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.helper.PartialConfigMother;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.serverhealth.HealthStateLevel;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.ListUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class GoPartialConfigIntegrationTest {
    @Autowired
    private ServerHealthService serverHealthService;
    @Autowired
    private CachedGoPartials cachedGoPartials;
    @Autowired
    private CachedGoConfig cachedGoConfig;
    @Autowired
    private GoConfigService configService;
    @Autowired
    private GoPartialConfig goPartialConfig;
    @Autowired
    private GoCache goCache;
    @Autowired
    private GoConfigDao goConfigDao;

    private GoConfigFileHelper configHelper = new GoConfigFileHelper();
    private ConfigRepoConfig repoConfig1;
    private ConfigRepoConfig repoConfig2;

    @Before
    public void setUp() throws Exception {
        goCache.clear();
        configHelper.usingCruiseConfigDao(goConfigDao);
        configHelper.onSetUp();
        repoConfig1 = new ConfigRepoConfig(new GitMaterialConfig("url1"), "plugin");
        repoConfig2 = new ConfigRepoConfig(new GitMaterialConfig("url2"), "plugin");
        configHelper.addConfigRepo(repoConfig1);
        configHelper.addConfigRepo(repoConfig2);

    }

    @After
    public void teardown() throws Exception {
        configHelper.onTearDown();
    }

    @Test
    public void shouldSaveConfigWhenANewValidPartialGetsAdded() {
        goPartialConfig.onSuccessPartialConfig(repoConfig1, PartialConfigMother.withPipeline("p1", new RepoConfigOrigin(repoConfig1, "4567")));
        assertThat(goConfigDao.loadConfigHolder().config.getAllPipelineNames().contains(new CaseInsensitiveString("p1")), is(true));
    }

    @Test
    public void shouldNotSaveConfigWhenANewInValidPartialGetsAdded() {
        goPartialConfig.onSuccessPartialConfig(repoConfig1, PartialConfigMother.invalidPartial("p1"));
        assertThat(goConfigDao.loadConfigHolder().config.getAllPipelineNames().contains(new CaseInsensitiveString("p1")), is(false));
        assertThat(serverHealthService.containsError(HealthStateType.invalidConfigMerge(), HealthStateLevel.ERROR), is(true));
    }

    @Test
    public void shouldTryToValidateMergeAndSaveAllKnownPartialsWhenAPartialChange() {
        cachedGoPartials.addOrUpdate(repoConfig1.getMaterialConfig().getFingerprint(), PartialConfigMother.withPipeline("p1_repo1", new RepoConfigOrigin(repoConfig1, "1234")));
        assertThat(goConfigDao.loadConfigHolder().config.getAllPipelineNames().contains(new CaseInsensitiveString("p1_repo1")), is(false));

        goPartialConfig.onSuccessPartialConfig(repoConfig2, PartialConfigMother.withPipeline("p2_repo2", new RepoConfigOrigin(repoConfig2, "4567")));

        assertThat(goConfigDao.loadConfigHolder().config.getAllPipelineNames().contains(new CaseInsensitiveString("p1_repo1")), is(true));
        assertThat(goConfigDao.loadConfigHolder().config.getAllPipelineNames().contains(new CaseInsensitiveString("p2_repo2")), is(true));

        ServerHealthState healthStateForInvalidConfigMerge = ListUtil.find(serverHealthService.getAllLogs(), new ListUtil.Condition() {
            @Override
            public <T> boolean isMet(T item) {
                return ((ServerHealthState) item).getType().equals(HealthStateType.invalidConfigMerge());
            }
        });
        assertThat(healthStateForInvalidConfigMerge, is(nullValue()));
    }

    @Test
    public void shouldValidateAndMergeJustTheChangedPartialAlongWithAllValidPartialsIfValidationIfAllKnownPartialsFail() {
        goPartialConfig.onSuccessPartialConfig(repoConfig1, PartialConfigMother.withPipeline("p1_repo1", new RepoConfigOrigin(repoConfig1, "1")));
        assertThat(goConfigDao.loadConfigHolder().config.getAllPipelineNames().contains(new CaseInsensitiveString("p1_repo1")), is(true));

        goPartialConfig.onSuccessPartialConfig(repoConfig1, PartialConfigMother.invalidPartial("p1_repo1_invalid", new RepoConfigOrigin(repoConfig1, "2")));
        assertThat(goConfigDao.loadConfigHolder().config.getAllPipelineNames().contains(new CaseInsensitiveString("p1_repo1_invalid")), is(false));

        goPartialConfig.onSuccessPartialConfig(repoConfig2, PartialConfigMother.withPipeline("p2_repo2", new RepoConfigOrigin(repoConfig2, "1")));

        assertThat(goConfigDao.loadConfigHolder().config.getAllPipelineNames().contains(new CaseInsensitiveString("p1_repo1")), is(true));
        assertThat(goConfigDao.loadConfigHolder().config.getAllPipelineNames().contains(new CaseInsensitiveString("p2_repo2")), is(true));
        assertThat(goConfigDao.loadConfigHolder().config.getAllPipelineNames().contains(new CaseInsensitiveString("p1_repo1_invalid")), is(false));

        ServerHealthState healthStateForInvalidConfigMerge = ListUtil.find(serverHealthService.getAllLogs(), new ListUtil.Condition() {
            @Override
            public <T> boolean isMet(T item) {
                return ((ServerHealthState) item).getType().equals(HealthStateType.invalidConfigMerge());
            }
        });
        assertThat(healthStateForInvalidConfigMerge, is(nullValue()));
    }
}