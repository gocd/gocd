/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
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
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.persistence;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.server.security.ldap.BaseConfig;
import com.thoughtworks.go.config.server.security.ldap.BasesConfig;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.*;

public class OauthTokenSweeperTest {
    private OauthRepository mockRepo;
    private OauthTokenSweeper oauthTokenSweeper;

    @Before
    public void setUp() {
        mockRepo = mock(OauthRepository.class);
        oauthTokenSweeper = new OauthTokenSweeper(mockRepo, null);
    }

    @Test
    public void shouldDeleteAllTokensWhenSecurityIsEnabled() {
        oauthTokenSweeper.onConfigChange(configWithoutSecurity());
        oauthTokenSweeper.onConfigChange(configWithPasswordFile());
        verify(mockRepo).deleteAllOauthGrants();
    }

    @Test
    public void shouldDeleteAllTokensWhenSecurityMethodIsChanged() {
        oauthTokenSweeper.onConfigChange(configWithPasswordFile());
        oauthTokenSweeper.onConfigChange(configWithLdap());
        verify(mockRepo).deleteAllOauthGrants();
    }

    @Test
    public void shouldDeleteAllTokensWhenSecurityMethodDetailsAreChanged() {
        CruiseConfig oldConfig = configWithPasswordFile();
        oauthTokenSweeper.onConfigChange(oldConfig);

        CruiseConfig newConfig = configWithPasswordFile();
        SecurityConfig securityConfig = new SecurityConfig(null, new PasswordFileConfig(oldConfig.server().security().passwordFileConfig().path() + ".new"), false, null);
        newConfig.setServerConfig(new ServerConfig("artifacts", securityConfig));

        oauthTokenSweeper.onConfigChange(newConfig);

        verify(mockRepo).deleteAllOauthGrants();
    }

    @Test
    public void shouldNotDeleteTokensWhenSecurityMethodIsNotChanged() {
        oauthTokenSweeper.onConfigChange(configWithLdap());
        oauthTokenSweeper.onConfigChange(configWithLdap());
        verifyNoMoreInteractions(mockRepo);
    }

    @Test
    public void shouldCacheTheNewlyReportedConfigEachTime() {
        CruiseConfig oldConfig = configWithPasswordFile();
        oauthTokenSweeper.onConfigChange(oldConfig);

        CruiseConfig newConfig = configWithPasswordFile();
        SecurityConfig securityConfig = new SecurityConfig(null, new PasswordFileConfig(oldConfig.server().security().passwordFileConfig().path() + ".new"), false, null);
        newConfig.setServerConfig(new ServerConfig("artifacts", securityConfig));

        oauthTokenSweeper.onConfigChange(newConfig);

        verify(mockRepo).deleteAllOauthGrants();

        oauthTokenSweeper.onConfigChange(newConfig);
        verifyNoMoreInteractions(mockRepo);
    }

    @Test
    public void shouldNotDeleteTokensWhenSecurityIsNotChanged() {
        oauthTokenSweeper.onConfigChange(configWithoutSecurity());
        oauthTokenSweeper.onConfigChange(configWithoutSecurity());
        verifyNoMoreInteractions(mockRepo);
    }

    @Test
    public void shouldNotDeleteTokensWhenRolesAreChanged() {
        oauthTokenSweeper.onConfigChange(configWithoutSecurity());

        CruiseConfig newConfig = configWithoutSecurity();
        newConfig.server().security().addRole(new Role(new CaseInsensitiveString("viewer")));

        oauthTokenSweeper.onConfigChange(newConfig);

        verifyNoMoreInteractions(mockRepo);
    }

    @Test
    public void shouldNotDeleteTokensWhenAdminsAreChanged() {
        oauthTokenSweeper.onConfigChange(configWithoutSecurity());

        CruiseConfig newConfig = configWithoutSecurity();
        newConfig.server().security().adminsConfig().add(new AdminRole(new CaseInsensitiveString("root")));

        oauthTokenSweeper.onConfigChange(newConfig);

        verifyNoMoreInteractions(mockRepo);
    }

    private CruiseConfig configWithoutSecurity() {
        return new BasicCruiseConfig();
    }

    private CruiseConfig configWithLdap() {
        return configWithSecurity(
                new LdapConfig("uri", "manager", "pwd", null, true, new BasesConfig(new BaseConfig("foo")), "bar", "foo"),
                null);
    }

    private CruiseConfig configWithPasswordFile() {
        return configWithSecurity(
                null,
                new PasswordFileConfig("password.properties"));
    }

    private CruiseConfig configWithSecurity(LdapConfig ldapConfig, PasswordFileConfig passwordFileConfig) {
        CruiseConfig newCruiseConfig = configWithoutSecurity();
        SecurityConfig securityConfig = new SecurityConfig(ldapConfig, passwordFileConfig, false, null);
        newCruiseConfig.setServerConfig(new ServerConfig("artifacts", securityConfig));
        return newCruiseConfig;
    }
}
