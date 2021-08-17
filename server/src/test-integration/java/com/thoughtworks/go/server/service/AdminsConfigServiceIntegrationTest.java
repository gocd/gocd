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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.util.GoConfigFileHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Collections;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
public class AdminsConfigServiceIntegrationTest {
    @Autowired private GoConfigDao goConfigDao;
    @Autowired private AdminsConfigService adminsConfigService;
    @Autowired private CachedGoConfig cachedGoConfig;
    @Autowired private EntityHashingService entityHashingService;

    private static final GoConfigFileHelper CONFIG_HELPER = new GoConfigFileHelper();
    private static final Username USERNAME = new Username(new CaseInsensitiveString("admin"));

    @BeforeEach
    public void setUp() throws Exception {
        CONFIG_HELPER.usingCruiseConfigDao(goConfigDao);
        CONFIG_HELPER.onSetUp();
        cachedGoConfig.clearListeners();
    }

    @AfterEach
    public void tearDown() throws Exception {
        CONFIG_HELPER.usingCruiseConfigDao(goConfigDao);
        CONFIG_HELPER.onTearDown();
        cachedGoConfig.clearListeners();
    }

    @Test
    public void update_shouldBeAbleToUpdateSystemAdminsWithUsers() {
        CONFIG_HELPER.addAdmins("existing_admin_user");

        assertTrue(adminsConfigService.systemAdmins().has(new AdminUser(new CaseInsensitiveString("existing_admin_user")), null));

        AdminUser newAdminUser = new AdminUser(new CaseInsensitiveString("new_admin_user"));
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        String hashForEntity = entityHashingService.hashForEntity(adminsConfigService.systemAdmins());

        adminsConfigService.update(USERNAME, new AdminsConfig(newAdminUser), hashForEntity, result);

        assertThat(result.httpCode(), is(200));
        assertThat(adminsConfigService.systemAdmins().size(), is(1));
        assertTrue(adminsConfigService.systemAdmins().has(newAdminUser, null));
    }

    @Test
    public void update_shouldBeAbleToUpdateSystemAdminsWithRoles() {
        Role devs = new RoleConfig(new CaseInsensitiveString("devs"), new RoleUser(new CaseInsensitiveString("first")));
        Role qas = new RoleConfig(new CaseInsensitiveString("qas"), new RoleUser(new CaseInsensitiveString("first")));
        CONFIG_HELPER.addRole(devs);
        CONFIG_HELPER.addRole(qas);

        CONFIG_HELPER.addAdminRoles("devs");
        assertTrue(adminsConfigService.systemAdmins().has(null, Collections.singletonList(devs)));

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        String hashForEntity = entityHashingService.hashForEntity(adminsConfigService.systemAdmins());

        adminsConfigService.update(USERNAME, new AdminsConfig(new AdminRole(new CaseInsensitiveString("qas"))), hashForEntity, result);

        assertThat(result.httpCode(), is(200));
        assertThat(adminsConfigService.systemAdmins().size(), is(1));
        assertTrue(adminsConfigService.systemAdmins().has(null, Collections.singletonList(qas)));
    }

    @Test
    public void update_shouldEnsureOnlyValidRolesCanBeSystemAdmins() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        String hashForEntity = entityHashingService.hashForEntity(adminsConfigService.systemAdmins());
        AdminsConfig newSystemAdmins = new AdminsConfig(new AdminRole(new CaseInsensitiveString("qas")));

        adminsConfigService.update(USERNAME, newSystemAdmins, hashForEntity, result);

        assertThat(result.httpCode(), is(422));
        assertThat(result.message(), is("Validations failed for admins. Error(s): [Role \"qas\" does not exist.]. Please correct and resubmit."));
        assertThat(adminsConfigService.systemAdmins().size(), is(0));
        assertThat(newSystemAdmins.errors().on("roles"), is("Role \"qas\" does not exist."));
    }
}
