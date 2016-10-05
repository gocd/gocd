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

package com.thoughtworks.go.server.service.materials;

import com.thoughtworks.go.config.UpdateConfigCommand;
import com.thoughtworks.go.config.update.CreatePackageRepositoryCommand;
import com.thoughtworks.go.domain.NullPlugin;
import com.thoughtworks.go.domain.Plugin;
import com.thoughtworks.go.domain.config.*;
import com.thoughtworks.go.domain.packagerepository.PackageRepositories;
import com.thoughtworks.go.domain.packagerepository.PackageRepository;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.presentation.TriStateSelection;
import com.thoughtworks.go.server.dao.PluginSqlMapDao;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.serverhealth.HealthStateType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Arrays;

import static java.util.Arrays.asList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})

public class PackageRepositoryServiceIntegrationTest {
    @Autowired
    private GoConfigService goConfigService;
    @Autowired
    private PackageRepositoryService service;
    @Autowired
    private PluginSqlMapDao pluginSqlMapDao;
    @Autowired
    private EntityHashingService entityHashingService;

    @Mock
    private PluginManager pluginManager;

    private Username username;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        service.setPluginManager(pluginManager);
        username = new Username("CurrentUser");
        UpdateConfigCommand command = goConfigService.modifyAdminPrivilegesCommand(asList(username.getUsername().toString()), new TriStateSelection(Admin.GO_SYSTEM_ADMIN, TriStateSelection.Action.add));
        goConfigService.updateConfig(command);
    }

    @After
    public void tearDown() throws Exception {
        pluginSqlMapDao.deleteAllPlugins();
        goConfigService.getConfigForEditing().setPackageRepositories(new PackageRepositories());
    }

    @Test
    public void shouldDeleteTheSpecifiedPackageRepository() throws Exception {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        String repoId = "npm";
        PackageRepository npmRepo = new PackageRepository();
        npmRepo.setId(repoId);
        goConfigService.getConfigForEditing().setPackageRepositories(new PackageRepositories(npmRepo));
        assertThat(goConfigService.getConfigForEditing().getPackageRepositories().size(), is(1));
        assertThat(goConfigService.getConfigForEditing().getPackageRepositories().find(repoId), is(npmRepo));

        service.deleteRepository(username, npmRepo, result);

        HttpLocalizedOperationResult expectedResult = new HttpLocalizedOperationResult();
        expectedResult.setMessage(LocalizedMessage.string("PACKAGE_REPOSITORY_DELETE_SUCCESSFUL", npmRepo.getId()));

        assertThat(result, is(expectedResult));
        assertThat(goConfigService.getConfigForEditing().getPackageRepositories().size(), is(0));
        assertNull(goConfigService.getConfigForEditing().getPackageRepositories().find(repoId));
    }

    @Test
    public void shouldReturnTheExactLocalizeMessageIfItFailsToDeletePackageRepository() throws Exception {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        String repoId = "npm";
        PackageRepository npmRepo = new PackageRepository();
        npmRepo.setId(repoId);
        goConfigService.getConfigForEditing().setPackageRepositories(new PackageRepositories(npmRepo));
        HttpLocalizedOperationResult expectedResult = new HttpLocalizedOperationResult();
        expectedResult.unauthorized(LocalizedMessage.string("UNAUTHORIZED_TO_OPERATE"), HealthStateType.unauthorised());

        assertThat(goConfigService.getConfigForEditing().getPackageRepositories().size(), is(1));
        assertThat(goConfigService.getConfigForEditing().getPackageRepositories().find(repoId), is(npmRepo));

        service.deleteRepository(new Username("UnauthorizedUser"), npmRepo, result);

        assertThat(result, is(expectedResult));
        assertThat(goConfigService.getConfigForEditing().getPackageRepositories().size(), is(1));
        assertThat(goConfigService.getConfigForEditing().getPackageRepositories().find(repoId), is(npmRepo));
    }

    @Test
    public void shouldReturnTheExactLocalizeMessageIfItFailsToCreatePackageRepository() throws Exception {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        String repoId = "npm";
        PackageRepository npmRepo = new PackageRepository();
        npmRepo.setId(repoId);
        goConfigService.getConfigForEditing().setPackageRepositories(new PackageRepositories(npmRepo));
        HttpLocalizedOperationResult expectedResult = new HttpLocalizedOperationResult();
        expectedResult.unauthorized(LocalizedMessage.string("UNAUTHORIZED_TO_OPERATE"), HealthStateType.unauthorised());

        assertThat(goConfigService.getConfigForEditing().getPackageRepositories().size(), is(1));
        assertThat(goConfigService.getConfigForEditing().getPackageRepositories().find(repoId), is(npmRepo));

        service.createPackageRepository(npmRepo, new Username("UnauthorizedUser"), result);

        assertThat(result, is(expectedResult));
        assertThat(goConfigService.getConfigForEditing().getPackageRepositories().size(), is(1));
        assertThat(goConfigService.getConfigForEditing().getPackageRepositories().find(repoId), is(npmRepo));
    }


    @Test
    public void shouldReturnTheExactLocalizeMessageIfItFailsToUpdatePackageRepository() throws Exception {
        HttpLocalizedOperationResult expectedResult = new HttpLocalizedOperationResult();
        expectedResult.unauthorized(LocalizedMessage.string("UNAUTHORIZED_TO_OPERATE"), HealthStateType.unauthorised());

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        String oldRepoId = "npmOrg";
        String newRepoId = "npm.org";
        PackageRepository oldPackageRepo = new PackageRepository();
        PackageRepository newPackageRepo = new PackageRepository();

        PluginConfiguration pluginConfiguration = new PluginConfiguration();
        pluginConfiguration.setId("npm");
        oldPackageRepo.setPluginConfiguration(pluginConfiguration);
        oldPackageRepo.setId(oldRepoId);
        oldPackageRepo.setName(oldRepoId);
        newPackageRepo.setPluginConfiguration(pluginConfiguration);
        newPackageRepo.setId(newRepoId);
        newPackageRepo.setName(newRepoId);
        Configuration configuration = new Configuration();
        configuration.add(new ConfigurationProperty(new ConfigurationKey("foo"), new ConfigurationValue("bar")));
        oldPackageRepo.setConfiguration(configuration);
        newPackageRepo.setConfiguration(configuration);
        when(pluginManager.getPluginDescriptorFor("npm")).thenReturn(new GoPluginDescriptor("npm", "1", null, null, null, false));
        goConfigService.getConfigForEditing().setPackageRepositories(new PackageRepositories(oldPackageRepo));

        assertThat(goConfigService.getConfigForEditing().getPackageRepositories().size(), is(1));
        assertThat(goConfigService.getConfigForEditing().getPackageRepositories().find(oldRepoId), is(oldPackageRepo));
        assertNull(goConfigService.getConfigForEditing().getPackageRepositories().find(newRepoId));
        service.updatePackageRepository(newPackageRepo, new Username("UnauthorizedUser"), "md5", result, oldRepoId);
        assertThat(result, is(expectedResult));
        assertThat(goConfigService.getConfigForEditing().getPackageRepositories().size(), is(1));
        assertThat(goConfigService.getConfigForEditing().getPackageRepositories().find(oldRepoId), is(oldPackageRepo));
        assertNull(goConfigService.getConfigForEditing().getPackageRepositories().find(newRepoId));
    }
}
