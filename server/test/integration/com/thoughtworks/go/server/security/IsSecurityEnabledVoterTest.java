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

package com.thoughtworks.go.server.security;

import com.thoughtworks.go.config.CachedGoPartials;
import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.config.GoConfigMigration;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.InstanceFactory;
import com.thoughtworks.go.util.ConfigElementImplementationRegistryMother;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.SystemTimeClock;
import org.hamcrest.core.Is;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.vote.AccessDecisionVoter;

import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class IsSecurityEnabledVoterTest {

    private GoConfigFileHelper configHelper = new GoConfigFileHelper();
    private GoConfigDao goConfigDao;

    @AfterClass
    public static void tearDownConfigFileLocation() {
    }

    @Before
    public void setup() throws Exception {
        goConfigDao = GoConfigFileHelper.createTestingDao();
        configHelper.usingCruiseConfigDao(goConfigDao);
        configHelper.initializeConfigFile();
        new SystemEnvironment().set(SystemEnvironment.INBUILT_LDAP_PASSWORD_AUTH_ENABLED, true);
    }

    @After
    public void tearDown() throws Exception {
        new SystemEnvironment().set(SystemEnvironment.INBUILT_LDAP_PASSWORD_AUTH_ENABLED, false);
    }

    @Test
    public void shouldNotVoteAccessGrantedIfSecurityIsEnabledButAnonymousIsNot() {
        configHelper.addSecurityWithBogusLdapConfig(false);
        GoConfigService configService = new GoConfigService(goConfigDao, null, new SystemTimeClock(), mock(GoConfigMigration.class), null, null,
                ConfigElementImplementationRegistryMother.withNoPlugins(),
                new InstanceFactory(), mock(CachedGoPartials.class), null);
        IsSecurityEnabledVoter voter = new IsSecurityEnabledVoter(configService);
        int accessStatus = voter.vote(null, null, null);
        assertThat(accessStatus, Is.is(AccessDecisionVoter.ACCESS_ABSTAIN));
    }
}
