/*
 * Copyright 2019 ThoughtWorks, Inc.
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
import com.thoughtworks.go.domain.SecureSiteUrl;
import com.thoughtworks.go.domain.SiteUrl;
import com.thoughtworks.go.domain.User;
import com.thoughtworks.go.security.CryptoException;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.server.service.result.BulkUpdateUsersOperationResult;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.util.GoConfigFileHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:testPropertyConfigurer.xml",
        "classpath:WEB-INF/spring-all-servlet.xml",
})
public class ServerConfigServiceIntegrationTest {

    @Autowired
    GoConfigService goConfigService;
    @Autowired
    ServerConfigService serverConfigService;
    @Autowired
    UserService userService;
    @Autowired
    GoConfigDao goConfigDao;

    private GoConfigFileHelper configHelper = new GoConfigFileHelper();

    @Before
    public void setup() throws Exception {
        configHelper.usingCruiseConfigDao(goConfigDao).initializeConfigFile();
        configHelper.onSetUp();

        goConfigService.forceNotifyListeners();
        goConfigService.security().securityAuthConfigs().add(new SecurityAuthConfig("file", "cd.go.authentication.passwordfile"));
    }

    @After
    public void tearDown() throws Exception {
        configHelper.onTearDown();
    }

    @Test
    public void shouldNotUpdateMailHostWhenMailhostisEmpty() {
        MailHost mailHost = new MailHost(new GoCipher());
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        serverConfigService.updateServerConfig(mailHost, "artifacts", null, null, "42", true, "http://site_url", "https://secure_site_url", "default", result,
                goConfigDao.md5OfConfigFile());

        assertThat(goConfigService.getMailHost(), is(nullValue()));
        assertThat(result.isSuccessful(), is(true));
    }

    @Test
    public void shouldUpdateServerConfig() throws CryptoException {
        MailHost mailHost = new MailHost("boo", 1, "username", "password", new GoCipher().encrypt("password"), true, true, "from@from.com", "admin@admin.com", new GoCipher());
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        serverConfigService.updateServerConfig(mailHost, "newArtifactsDir", 10.0, 20.0, "42", true, "http://site_url", "https://secure_site_url", "gist-repo/folder",
                result, goConfigDao.md5OfConfigFile());

        assertThat(goConfigService.getMailHost(), is(mailHost));
        assertThat(goConfigService.serverConfig().artifactsDir(), is("newArtifactsDir"));
        assertThat(goConfigService.serverConfig().getSiteUrl().getUrl(), is("http://site_url"));
        assertThat(goConfigService.serverConfig().getSecureSiteUrl().getUrl(), is("https://secure_site_url"));
        assertThat(goConfigService.serverConfig().getJobTimeout(), is("42"));
        assertThat(goConfigService.serverConfig().getPurgeStart(), is(10.0));
        assertThat(goConfigService.serverConfig().getPurgeUpto(), is(20.0));
        assertThat(goConfigService.serverConfig().getCommandRepositoryLocation(), is("gist-repo/folder"));

        assertThat(result.isSuccessful(), is(true));
    }

    @Test
    public void shouldAllowNullValuesForPurgeStartAndPurgeUpTo() throws CryptoException {
        MailHost mailHost = new MailHost("boo", 1, "username", "password", new GoCipher().encrypt("password"), true, true, "from@from.com", "admin@admin.com", new GoCipher());
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        serverConfigService.updateServerConfig(mailHost, "newArtifactsDir", null, null, "42", true, "http://site_url", "https://secure_site_url", "default",
                result,
                goConfigDao.md5OfConfigFile());

        assertThat(goConfigService.serverConfig().getPurgeStart(), is(nullValue()));
        assertThat(goConfigService.serverConfig().getPurgeUpto(), is(nullValue()));
    }

    @Test
    public void shouldUpdateWithEmptySecureSiteUrlAndSiteUrl() throws CryptoException {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        MailHost mailHost = new MailHost("boo", 1, "username", "password", new GoCipher().encrypt("password"), true, true, "from@from.com", "admin@admin.com", new GoCipher());
        serverConfigService.updateServerConfig(mailHost, "newArtifactsDir", null, null, "42", true, "", "", "default", result,
                goConfigDao.md5OfConfigFile());
        assertThat(goConfigService.serverConfig().getSiteUrl(), is(new SiteUrl()));
        assertThat(goConfigService.serverConfig().getSecureSiteUrl(), is(new SecureSiteUrl()));
    }

    @Test
    public void update_shouldKeepTheExistingSecurityAsIs() throws IOException {
        Role role = new RoleConfig(new CaseInsensitiveString("awesome"), new RoleUser(new CaseInsensitiveString("first")));
        configHelper.turnOnSecurity();
        configHelper.addRole(role);

        SecurityConfig securityConfig = createSecurity(role, null, false);
        securityConfig.securityAuthConfigs().addAll(goConfigService.serverConfig().security().securityAuthConfigs());

        MailHost mailHost = new MailHost("boo", 1, "username", "password", true, true, "from@from.com", "admin@admin.com");
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        serverConfigService.updateServerConfig(mailHost, "artifacts", null, null, "42", true, "http://site_url", "https://secure_site_url", "default", result,
                goConfigDao.md5OfConfigFile());

        assertThat(goConfigService.getMailHost(), is(mailHost));
        assertThat(goConfigService.security(), is(securityConfig));
        assertThat(result.isSuccessful(), is(true));
    }

    private SecurityConfig createSecurity(Role role, SecurityAuthConfig securityAuthConfig, boolean allowOnlyKnownUsersToLogin) {
        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.addRole(role);
        if (securityAuthConfig != null) {
            securityConfig.securityAuthConfigs().add(securityAuthConfig);
        }
        securityConfig.modifyAllowOnlyKnownUsers(allowOnlyKnownUsersToLogin);
        return securityConfig;
    }

    @Test
    public void shouldUpdateServerConfigShouldFailWhenConfigSaveFails() {
        MailHost mailHost = new MailHost("boo", 1, "username", "password", true, true, "from@from.com", "admin@admin.com");
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        serverConfigService.updateServerConfig(mailHost, "artifacts", null, null, "-42", true, "http://site_url", "https://secure_site_url", "default", result,
                goConfigDao.md5OfConfigFile());

        assertThat(result.isSuccessful(), is(false));
        assertThat(result.message(), containsString("Failed to save the server configuration. Reason: "));
        assertThat(result.message(), containsString("Timeout cannot be a negative number as it represents number of minutes"));
    }

    @Test
    public void updateServerConfig_ShouldFailWhenAllowAutoLoginIsTurnedOffWithNoAdminsRemaining() throws IOException {
        configHelper.enableSecurity();
        userService.deleteUsers(userService.allUsers().stream().map(User::getName).collect(Collectors.toList()), "admin", new BulkUpdateUsersOperationResult());
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        serverConfigService.updateServerConfig(new MailHost(new GoCipher()), "artifacts", null, null, "42",
                false,
                "http://site_url", "https://secure_site_url", "default", result, goConfigDao.md5OfConfigFile());

        assertThat(result.isSuccessful(), is(false));
        assertThat(result.message(), containsString("Cannot disable auto login with no admins enabled."));
    }

    @Test
    public void shouldNotUpdateWhenHostnameIsInvalid() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        serverConfigService.updateServerConfig(new MailHost("boo%bee", 1, "username", "password", true, true, "from@from.com", "admin@admin.com"),
                "artifacts", null, null, "42", true, "http://site_url", "https://secure_site_url", "default", result, goConfigDao.md5OfConfigFile());

        assertThat(result.isSuccessful(), is(false));
        assertThat(result.message(), is("Invalid hostname. A valid hostname can only contain letters (A-z) digits (0-9) hyphens (-) dots (.) and underscores (_)."));
        assertThat(goConfigService.getMailHost(), is(nullValue()));
    }

    @Test
    public void shouldNotUpdateWhenPortIsInvalid() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        serverConfigService.updateServerConfig(new MailHost("boo", -1, "username", "password", true, true, "from@from.com", "admin@admin.com"),
                "artifacts", null, null, "42", true, "http://site_url", "https://secure_site_url", "default", result, goConfigDao.md5OfConfigFile());

        assertThat(result.isSuccessful(), is(false));
        assertThat(result.message(), is("Invalid port."));
        assertThat(goConfigService.getMailHost(), is(nullValue()));
    }

    @Test
    public void shouldNotUpdateWhenEmailIsInvalid() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        serverConfigService.updateServerConfig(new MailHost("boo", 1, "username", "password", true, true, "from", "admin@admin.com"),
                "artifacts", null, null, "42", true,
                "http://site_url", "https://secure_site_url", "default", result, goConfigDao.md5OfConfigFile());

        assertThat(result.isSuccessful(), is(false));
        assertThat(result.message(), is("From address is not a valid email address."));
        assertThat(goConfigService.getMailHost(), is(nullValue()));
    }

    @Test
    public void shouldNotUpdateWhenAdminEmailIsInvalid() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        serverConfigService.updateServerConfig(new MailHost("boo", 1, "username", "password", true, true, "from@from.com", "admin"),
                "artifacts", null, null, "42", true,
                "http://site_url", "https://secure_site_url", "default", result, goConfigDao.md5OfConfigFile());

        assertThat(result.isSuccessful(), is(false));
        assertThat(result.message(), is("Admin address is not a valid email address."));
        assertThat(goConfigService.getMailHost(), is(nullValue()));
    }

    @Test
    public void shouldSiteUrlForGivenUrl() throws URISyntaxException {
        configHelper.setBaseUrls(new SiteUrl("http://foo.com"), new SecureSiteUrl("https://bar.com"));
        assertThat(serverConfigService.siteUrlFor("http://test.host/foo/bar", true), is("https://bar.com/foo/bar"));
        assertThat(serverConfigService.siteUrlFor("http://test.host/foo/bar", false), is("http://foo.com/foo/bar"));
    }

    @Test
    public void shouldReturnTheSameURLWhenNothingIsConfigured() throws URISyntaxException {
        assertThat(serverConfigService.siteUrlFor("http://test.host/foo/bar", true), is("http://test.host/foo/bar"));
        assertThat(serverConfigService.siteUrlFor("http://test.host/foo/bar", false), is("http://test.host/foo/bar"));
    }

    @Test
    public void shouldUseTheSiteUrlWhenSecureSiteUrlIsNotPresentAndOnlyIfSiteUrlIsHttps() throws URISyntaxException {
        configHelper.setBaseUrls(new SiteUrl("https://foo.com"), new SecureSiteUrl());
        assertThat(serverConfigService.siteUrlFor("http://test.host/foo/bar", true), is("https://foo.com/foo/bar"));
        assertThat(serverConfigService.siteUrlFor("http://test.host/foo/bar", false), is("https://foo.com/foo/bar"));
    }

    @Test
    public void shouldUseTheSecureSiteUrlInspiteOfCallerNotForcingSsl_whenAlreadyUsingHTTPS() throws URISyntaxException {
        configHelper.setBaseUrls(new SiteUrl("http://foo.com:80"), new SecureSiteUrl("https://bar.com:443"));
        assertThat(serverConfigService.siteUrlFor("https://test.host:1000/foo/bar", false), is("https://bar.com:443/foo/bar"));
        assertThat(serverConfigService.siteUrlFor("http://test.host/foo/bar", false), is("http://foo.com:80/foo/bar"));
    }
}
