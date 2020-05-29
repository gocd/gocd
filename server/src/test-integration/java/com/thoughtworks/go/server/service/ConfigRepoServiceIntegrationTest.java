/*
 * Copyright 2020 ThoughtWorks, Inc.
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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.config.UpdateConfigCommand;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.ConfigReposConfig;
import com.thoughtworks.go.domain.config.Admin;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.plugin.access.configrepo.ConfigRepoExtension;
import com.thoughtworks.go.presentation.TriStateSelection;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.materials.MaterialUpdateService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.util.GoConfigFileHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static com.thoughtworks.go.helper.MaterialConfigsMother.git;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
public class ConfigRepoServiceIntegrationTest {
    @Autowired
    private GoConfigDao goConfigDao;
    @Autowired
    private GoConfigService goConfigService;
    @Autowired
    private ConfigRepoService configRepoService;
    @Autowired
    private EntityHashingService entityHashingService;
    @Autowired
    private SecurityService securityService;
    @Autowired
    private MaterialUpdateService materialUpdateService;
    @Autowired
    private MaterialConfigConverter materialConfigConverter;

    @Mock
    private ConfigRepoExtension configRepoExtension;

    private String repoId, pluginId;
    private Username user;
    private ConfigRepoConfig configRepo;

    private GoConfigFileHelper configHelper = new GoConfigFileHelper();

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        user = new Username(new CaseInsensitiveString("current"));
        UpdateConfigCommand command = goConfigService.modifyAdminPrivilegesCommand(asList(user.getUsername().toString()), new TriStateSelection(Admin.GO_SYSTEM_ADMIN, TriStateSelection.Action.add));
        goConfigService.updateConfig(command);

        this.repoId = "repo-1";
        this.pluginId = "json-config-repo-plugin";
        MaterialConfig repoMaterial = git("https://foo.git", "master");
        this.configRepo = ConfigRepoConfig.createConfigRepoConfig(repoMaterial, pluginId, repoId);

        configHelper.usingCruiseConfigDao(goConfigDao).initializeConfigFile();
        configHelper.onSetUp();

        goConfigService.forceNotifyListeners();
    }

    @After
    public void tearDown() throws Exception {
        configHelper.onTearDown();
    }

    @Test
    public void shouldFindConfigRepoWithSpecifiedId() throws Exception {
        configHelper.enableSecurity();
        goConfigService.getConfigForEditing().getConfigRepos().add(configRepo);
        assertThat(configRepoService.getConfigRepo(repoId), is(configRepo));
    }

    @Test
    public void shouldReturnNullWhenConfigRepoWithSpecifiedIdIsNotPresent() throws Exception {
        configHelper.enableSecurity();
        assertNull(configRepoService.getConfigRepo(repoId));
    }

    @Test
    public void shouldFindAllConfigRepos() throws Exception {
        configHelper.enableSecurity();
        goConfigService.getConfigForEditing().getConfigRepos().add(configRepo);
        assertThat(configRepoService.getConfigRepos(), is(new ConfigReposConfig(configRepo)));
    }

    @Test
    public void shouldDeleteSpecifiedConfigRepository() throws Exception {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        configHelper.enableSecurity();
        goConfigDao.updateConfig(new UpdateConfigCommand() {
            @Override
            public CruiseConfig update(CruiseConfig cruiseConfig) throws Exception {
                cruiseConfig.getConfigRepos().add(configRepo);
                return cruiseConfig;
            }
        });

        assertThat(configRepoService.getConfigRepo(repoId), is(configRepo));

        configRepoService.deleteConfigRepo(repoId, user, result);

        assertNull(configRepoService.getConfigRepo(repoId));
        assertThat(result.toString(), result.isSuccessful(), is(true));
    }

    @Test
    public void shouldCreateSpecifiedConfigRepository() throws Exception {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        configHelper.enableSecurity();
        configRepoService = new ConfigRepoService(goConfigService, securityService, entityHashingService, configRepoExtension, materialUpdateService, materialConfigConverter);

        when(configRepoExtension.canHandlePlugin(any())).thenReturn(true);

        assertNull(configRepoService.getConfigRepo(repoId));

        configRepoService.createConfigRepo(configRepo, user, result);

        assertThat(configRepoService.getConfigRepo(repoId), is(configRepo));
        assertThat(result.toString(), result.isSuccessful(), is(true));
    }

    @Test
    public void shouldUpdateSpecifiedConfigRepository() throws Exception {
        configRepoService = new ConfigRepoService(goConfigService, securityService, entityHashingService, configRepoExtension, materialUpdateService, materialConfigConverter);

        when(configRepoExtension.canHandlePlugin(any())).thenReturn(true);
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        configHelper.enableSecurity();
        goConfigDao.updateConfig(new UpdateConfigCommand() {
            @Override
            public CruiseConfig update(CruiseConfig cruiseConfig) throws Exception {
                cruiseConfig.getConfigRepos().add(configRepo);
                return cruiseConfig;
            }
        });
        String newRepoId = "repo-2";
        ConfigRepoConfig toUpdateWith = ConfigRepoConfig.createConfigRepoConfig(git("http://bar.git", "master"), "yaml-plugin", newRepoId);

        assertThat(configRepoService.getConfigRepos().size(), is(1));
        assertThat(configRepoService.getConfigRepo(repoId), is(configRepo));

        configRepoService.updateConfigRepo(repoId, toUpdateWith, entityHashingService.hashForEntity(configRepo), user, result);

        assertThat(result.toString(), result.isSuccessful(), is(true));

        assertThat(configRepoService.getConfigRepos().size(), is(1));
        assertThat(configRepoService.getConfigRepo(newRepoId), is(toUpdateWith));
    }
}
