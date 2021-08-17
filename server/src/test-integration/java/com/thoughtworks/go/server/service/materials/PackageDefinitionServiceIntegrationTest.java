/*
 * Copyright 2021 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.domain.config.*;
import com.thoughtworks.go.domain.packagerepository.PackageDefinition;
import com.thoughtworks.go.domain.packagerepository.PackageRepositories;
import com.thoughtworks.go.domain.packagerepository.PackageRepository;
import com.thoughtworks.go.presentation.TriStateSelection;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.util.GoConfigFileHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.hamcrest.MatcherAssert.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
public class PackageDefinitionServiceIntegrationTest {
    @Autowired
    private GoConfigDao goConfigDao;
    @Autowired
    private GoConfigService goConfigService;
    @Autowired
    private PackageDefinitionService service;
    @Autowired
    private CachedGoPartials cachedGoPartials;
    @Autowired
    private DatabaseAccessHelper dbHelper;

    private GoConfigFileHelper configHelper = new GoConfigFileHelper();
    private Username user;
    private String repoId;

    @BeforeEach
    public void setup() throws Exception {
        cachedGoPartials.clear();
        configHelper = new GoConfigFileHelper();
        dbHelper.onSetUp();
        configHelper.usingCruiseConfigDao(goConfigDao).initializeConfigFile();
        configHelper.onSetUp();
        goConfigService.forceNotifyListeners();
        user = new Username(new CaseInsensitiveString("current"));
        final PackageRepository npmRepo = new PackageRepository();
        PluginConfiguration pluginConfiguration = new PluginConfiguration();
        pluginConfiguration.setId("npm");
        pluginConfiguration.setVersion("1");
        npmRepo.setPluginConfiguration(pluginConfiguration);
        repoId = "repoId";
        npmRepo.setId(repoId);
        npmRepo.setName(repoId);
        Configuration configuration = new Configuration();
        configuration.add(new ConfigurationProperty(new ConfigurationKey("PACKAGE_ID"), new ConfigurationValue("prettyjson")));
        npmRepo.setConfiguration(configuration);
        goConfigService.updateConfig(new UpdateConfigCommand() {
            @Override
            public CruiseConfig update(CruiseConfig cruiseConfig) throws Exception {
                cruiseConfig.setPackageRepositories(new PackageRepositories(npmRepo));
                return cruiseConfig;
            }
        });
        UpdateConfigCommand command = goConfigService.modifyAdminPrivilegesCommand(asList(user.getUsername().toString()), new TriStateSelection(Admin.GO_SYSTEM_ADMIN, TriStateSelection.Action.add));
        goConfigService.updateConfig(command);
    }

    @AfterEach
    public void tearDown() throws Exception {
        cachedGoPartials.clear();
        configHelper.onTearDown();
        dbHelper.onTearDown();
    }

    @Test
    public void shouldReturnTheExactLocalizeMessageIfItFailsToCreatePackageDefinition() throws Exception {
        String packageUuid = "random-uuid";
        String packageName = "prettyjson";
        Configuration configuration = new Configuration();
        configuration.add(new ConfigurationProperty(new ConfigurationKey("PACKAGE_ID"), new ConfigurationValue("prettyjson")));
        PackageDefinition packageDefinition = new PackageDefinition(packageUuid, packageName, configuration);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        HttpLocalizedOperationResult expectedResult = new HttpLocalizedOperationResult();
        String repositoryId = "Id";
        expectedResult.unprocessableEntity(EntityType.PackageRepository.notFoundMessage( repositoryId));

        assertNull(service.find(packageUuid));
        service.createPackage(packageDefinition, repositoryId, user, result);

        assertThat(result, is(expectedResult));
        assertNull(service.find(packageUuid));
    }

    @Test
    public void shouldDeletePackageDefinition() throws Exception {
        String packageUuid = "random-uuid";
        String packageName = "prettyjson";
        Configuration configuration = new Configuration();
        configuration.add(new ConfigurationProperty(new ConfigurationKey("PACKAGE_ID"), new ConfigurationValue("prettyjson")));
        PackageDefinition packageDefinition = new PackageDefinition(packageUuid, packageName, configuration);

        PackageRepositories repositories = goConfigService.getConfigForEditing().getPackageRepositories();
        PackageRepository repository = repositories.find(repoId);
        repository.addPackage(packageDefinition);
        repositories.removePackageRepository(repoId);
        repositories.add(repository);
        goConfigService.getConfigForEditing().setPackageRepositories(repositories);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        HttpLocalizedOperationResult expectedResult = new HttpLocalizedOperationResult();
        expectedResult.setMessage(EntityType.PackageDefinition.deleteSuccessful(packageDefinition.getId()));

        assertThat(goConfigService.getConfigForEditing().getPackageRepositories().find(repoId).getPackages().find(packageUuid), is(packageDefinition));
        service.deletePackage(packageDefinition, user, result);

        assertThat(result, is(expectedResult));
        assertNull(goConfigService.getConfigForEditing().getPackageRepositories().find(repoId).getPackages().find(packageUuid));
    }
}
