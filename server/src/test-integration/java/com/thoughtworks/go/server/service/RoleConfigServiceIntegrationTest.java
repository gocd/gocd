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

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.helper.ConfigFileFixture;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.util.GoConfigFileHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertFalse;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
public class RoleConfigServiceIntegrationTest {

    @Autowired
    RoleConfigService roleConfigService;
    @Autowired
    GoConfigService goConfigService;
    @Autowired
    GoConfigDao goConfigDao;

    private GoConfigFileHelper configFileHelper;

    @Before
    public void setUp() throws Exception {
        configFileHelper = new GoConfigFileHelper(ConfigFileFixture.CONFIG_WITH_ADMIN_AND_SECURITY_AUTH_CONFIG);
        configFileHelper.onSetUp();
        configFileHelper.usingCruiseConfigDao(goConfigDao).initializeConfigFile();
        goConfigService.forceNotifyListeners();
    }

    @After
    public void tearDown() throws Exception {
        configFileHelper.onTearDown();
    }

    @Test
    public void delete_shouldBeAbleToDeleteAnExistingGoCDRole() throws Exception {
        RoleConfig role = new RoleConfig(new CaseInsensitiveString("committer"));

        roleConfigService.delete(new Username("loser"), role, new HttpLocalizedOperationResult());

        CruiseConfig cruiseConfig = goConfigService.cruiseConfig();

        RolesConfig roles = cruiseConfig.server().security().getRoles();
        assertFalse(roles.isRoleExist(new CaseInsensitiveString("committer")));
    }

    @Test
    public void delete_shouldBeAbleToDeleteAnExistingPluginRole() throws Exception {
        PluginRoleConfig role = new PluginRoleConfig("go_admins", "tw-ldap");

        roleConfigService.delete(new Username("loser"), role, new HttpLocalizedOperationResult());

        CruiseConfig cruiseConfig = goConfigService.cruiseConfig();

        RolesConfig roles = cruiseConfig.server().security().getRoles();
        assertFalse(roles.isRoleExist(new CaseInsensitiveString("go_admins")));
    }
}
