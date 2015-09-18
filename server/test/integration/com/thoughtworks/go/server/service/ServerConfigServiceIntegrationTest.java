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

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.server.security.ldap.BaseConfig;
import com.thoughtworks.go.config.server.security.ldap.BasesConfig;
import com.thoughtworks.go.domain.ServerSiteUrlConfig;
import com.thoughtworks.go.i18n.Localizer;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.server.security.InMemoryLdapServerForTests;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.unboundid.ldif.LDIFRecord;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class ServerConfigServiceIntegrationTest {

    @Autowired
    GoConfigService goConfigService;
    @Autowired ServerConfigService serverConfigService;
    @Autowired UserService userService;
    @Autowired
    GoConfigDao goConfigDao;
    @Autowired Localizer localizer;

    private GoConfigFileHelper configHelper = new GoConfigFileHelper();

    private InMemoryLdapServerForTests ldapServer;
    private LDIFRecord employeesOrgUnit;

    private static final int PORT = 12389;
    private static final String LDAP_URL = "ldap://localhost:" + PORT;
    private static final String BASE_DN = "dc=corp,dc=somecompany,dc=com";
    private static final String MANAGER_DN = "cn=Active Directory Ldap User,ou=SomeSystems,ou=Accounts,ou=Principal," + BASE_DN;
    private static final String MANAGER_PASSWORD = "some-password";
    private static final String SEARCH_BASE = "ou=Employees,ou=Company,ou=Principal," + BASE_DN;
    private static final String SEARCH_FILTER = "(sAMAccountName={0})";

    @Before
    public void setup() throws Exception {
        configHelper.usingCruiseConfigDao(goConfigDao).initializeConfigFile();
        configHelper.onSetUp();

        goConfigService.forceNotifyListeners();

        ldapServer = new InMemoryLdapServerForTests(BASE_DN, MANAGER_DN, MANAGER_PASSWORD).start(PORT);
        ldapServer.addOrganizationalUnit("Principal", "ou=Principal," + BASE_DN);
        ldapServer.addOrganizationalUnit("Company", "ou=Company,ou=Principal," + BASE_DN);
        employeesOrgUnit = ldapServer.addOrganizationalUnit("Employees", "ou=Employees,ou=Company,ou=Principal," + BASE_DN);
    }

    @After
    public void tearDown() throws Exception {
        ldapServer.stop();
        configHelper.onTearDown();
    }

    @Test
    public void shouldUpdateServerConfigWithoutValidatingMailHostWhenMailhostisEmpty() {
        LdapConfig ldapConfig = new LdapConfig(LDAP_URL, MANAGER_DN, MANAGER_PASSWORD, null, true, new BasesConfig(new BaseConfig(SEARCH_BASE)), SEARCH_FILTER);
        PasswordFileConfig passwordFileConfig = new PasswordFileConfig("valid_path.txt");
        MailHost mailHost = new MailHost(new GoCipher());
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();


        serverConfigService.updateServerConfig(mailHost, ldapConfig, passwordFileConfig, "artifacts", null, null, "42", true, "http://site_url", "https://secure_site_url", "default", result,
                goConfigDao.md5OfConfigFile());

        assertThat(goConfigService.getMailHost(), is(mailHost));
        assertThat(goConfigService.security(), is(new SecurityConfig(ldapConfig, passwordFileConfig, false)));
        assertThat(result.isSuccessful(), is(true));
    }

    @Test
    public void shouldUpdateServerConfig() throws InvalidCipherTextException {
        LdapConfig ldapConfig = new LdapConfig(LDAP_URL, MANAGER_DN, MANAGER_PASSWORD, null, true, new BasesConfig(new BaseConfig(SEARCH_BASE)), SEARCH_FILTER);
        PasswordFileConfig passwordFileConfig = new PasswordFileConfig("valid_path.txt");
        MailHost mailHost = new MailHost("boo", 1, "username", "password", new GoCipher().encrypt("password"), true, true, "from@from.com", "admin@admin.com", new GoCipher());
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        serverConfigService.updateServerConfig(mailHost, ldapConfig, passwordFileConfig, "newArtifactsDir", 10.0, 20.0, "42", true, "http://site_url", "https://secure_site_url", "gist-repo/folder",
                result, goConfigDao.md5OfConfigFile());

        assertThat(goConfigService.getMailHost(), is(mailHost));
        assertThat(goConfigService.security(), is(new SecurityConfig(ldapConfig, passwordFileConfig, false)));
        assertThat(goConfigService.serverConfig().artifactsDir(), is("newArtifactsDir"));
        assertThat(goConfigService.serverConfig().getSiteUrl().getUrl(), is("http://site_url"));
        assertThat(goConfigService.serverConfig().getSecureSiteUrl().getUrl(), is("https://secure_site_url"));
        assertThat(goConfigService.serverConfig().getJobTimeout(), is("42"));
        assertThat(goConfigService.serverConfig().getPurgeStart(), is(10.0));
        assertThat(goConfigService.serverConfig().getPurgeUpto(), is(20.0));
        assertThat(goConfigService.serverConfig().getCommandRepositoryLocation(),is("gist-repo/folder"));

        assertThat(result.isSuccessful(), is(true));
    }

    @Test
    public void shouldAllowNullValuesForPurgeStartAndPurgeUpTo() throws InvalidCipherTextException {
        LdapConfig ldapConfig = new LdapConfig(LDAP_URL, MANAGER_DN, MANAGER_PASSWORD, null, true, new BasesConfig(new BaseConfig(SEARCH_BASE)), SEARCH_FILTER);
        PasswordFileConfig passwordFileConfig = new PasswordFileConfig("valid_path.txt");
        MailHost mailHost = new MailHost("boo", 1, "username", "password", new GoCipher().encrypt("password"), true, true, "from@from.com", "admin@admin.com", new GoCipher());
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        serverConfigService.updateServerConfig(mailHost, ldapConfig, passwordFileConfig, "newArtifactsDir", null, null, "42", true, "http://site_url", "https://secure_site_url", "default",
                result,
                goConfigDao.md5OfConfigFile());

        assertThat(goConfigService.serverConfig().getPurgeStart(), is(nullValue()));
        assertThat(goConfigService.serverConfig().getPurgeUpto(), is(nullValue()));
    }

    @Test
    public void shouldUpdateWithEmptySecureSiteUrlAndSiteUrl() throws InvalidCipherTextException {
        LdapConfig ldapConfig = new LdapConfig(LDAP_URL, MANAGER_DN, MANAGER_PASSWORD, null, true, new BasesConfig(new BaseConfig(SEARCH_BASE)), SEARCH_FILTER);
        PasswordFileConfig passwordFileConfig = new PasswordFileConfig("valid_path.txt");
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        MailHost mailHost = new MailHost("boo", 1, "username", "password", new GoCipher().encrypt("password"), true, true, "from@from.com", "admin@admin.com", new GoCipher());
        serverConfigService.updateServerConfig(mailHost, ldapConfig, passwordFileConfig, "newArtifactsDir", null, null, "42", true, "", "", "default", result,
                goConfigDao.md5OfConfigFile());
        assertThat(goConfigService.serverConfig().getSiteUrl(), is(new ServerSiteUrlConfig()));
        assertThat(goConfigService.serverConfig().getSecureSiteUrl(), is(new ServerSiteUrlConfig()));
    }

    @Test
    public void update_shouldKeepTheExistingSecurityAsIs() throws IOException {
        Role role = new Role(new CaseInsensitiveString("awesome"), new RoleUser(new CaseInsensitiveString("first")));
        configHelper.turnOnSecurity();
        configHelper.addRole(role);

        LdapConfig ldapConfig = new LdapConfig(LDAP_URL, MANAGER_DN, MANAGER_PASSWORD, null, true, new BasesConfig(new BaseConfig(SEARCH_BASE)), SEARCH_FILTER);
        PasswordFileConfig passwordConfig = new PasswordFileConfig("valid_path.txt");
        SecurityConfig securityConfig = createSecurity(role, ldapConfig, passwordConfig, false);

        MailHost mailHost = new MailHost("boo", 1, "username", "password", true, true, "from@from.com", "admin@admin.com");
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        serverConfigService.updateServerConfig(mailHost, ldapConfig, passwordConfig, "artifacts", null, null, "42", true, "http://site_url", "https://secure_site_url", "default", result,
                goConfigDao.md5OfConfigFile());

        assertThat(goConfigService.getMailHost(), is(mailHost));
        assertThat(goConfigService.security(), is(securityConfig));
        assertThat(result.isSuccessful(), is(true));
    }

    private SecurityConfig createSecurity(Role role, LdapConfig ldapConfig, PasswordFileConfig passwordConfig, boolean allowOnlyKnownUsersToLogin) {
        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.addRole(role);
        securityConfig.modifyLdap(ldapConfig);
        securityConfig.modifyPasswordFile(passwordConfig);
        securityConfig.modifyAllowOnlyKnownUsers(allowOnlyKnownUsersToLogin);
        return securityConfig;
    }

    @Test
    public void shouldUpdateServerConfigShouldFailWhenConfigSaveFails() {
        LdapConfig ldapConfig = new LdapConfig(LDAP_URL, MANAGER_DN, MANAGER_PASSWORD, null, true, new BasesConfig(new BaseConfig("")), "");
        PasswordFileConfig passwordFileConfig = new PasswordFileConfig("valid_path.txt");
        MailHost mailHost = new MailHost("boo", 1, "username", "password", true, true, "from@from.com", "admin@admin.com");
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        serverConfigService.updateServerConfig(mailHost, ldapConfig, passwordFileConfig, "artifacts", null, null, "42", true, "http://site_url", "https://secure_site_url", "default", result,
                goConfigDao.md5OfConfigFile());

        assertThat(result.isSuccessful(), is(false));
        assertThat(result.message(localizer), containsString("Failed to save the server configuration. Reason: "));
        assertThat(result.message(localizer), containsString("Search Base should not be empty"));
    }

    @Test
    public void shouldUpdateOnlyLdapConfiguration() {
        CruiseConfig cruiseConfig = goConfigDao.loadForEditing();
        LdapConfig newLdapConfig = new LdapConfig("url", "managerDN", "managerPassword", "encrypted", true, new BasesConfig(new BaseConfig("base1"), new BaseConfig("base2")), "filter");
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        ServerConfig serverConfig = cruiseConfig.server();
        serverConfigService.updateServerConfig(cruiseConfig.mailHost(), newLdapConfig, serverConfig.security().passwordFileConfig(),
                serverConfig.artifactsDir(), serverConfig.getPurgeStart(), serverConfig.getPurgeUpto(), serverConfig.getJobTimeout(), true,
                serverConfig.getSiteUrl().getUrl(), serverConfig.getSecureSiteUrl().getUrl(), serverConfig.getCommandRepositoryLocation(), result, cruiseConfig.getMd5());

        goConfigDao.forceReload();
        CruiseConfig updatedCruiseConfig = goConfigDao.loadForEditing();
        assertThat(result.isSuccessful(), is(true));
        assertThat(updatedCruiseConfig.server().security().ldapConfig().isEnabled(), is(true));
    }

    @Test
    public void updateServerConfig_ShouldFailWhenAllowAutoLoginIsTurnedOffWithNoAdminsRemaining() throws IOException {
        configHelper.turnOnSecurity();
        userService.deleteAll();
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        serverConfigService.updateServerConfig(new MailHost(new GoCipher()), new LdapConfig(new GoCipher()), new PasswordFileConfig(), "artifacts", null, null, "42",
                false,
                "http://site_url", "https://secure_site_url", "default", result, goConfigDao.md5OfConfigFile());

        assertThat(result.isSuccessful(), is(false));
        assertThat(result.message(localizer), containsString("Cannot disable auto login with no admins enabled."));
    }

    @Test
    public void shouldNotUpdateWhenHostnameIsInvalid() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        serverConfigService.updateServerConfig(new MailHost("boo%bee", 1, "username", "password", true, true, "from@from.com", "admin@admin.com"),
                new LdapConfig(new GoCipher()),
                new PasswordFileConfig(), "artifacts", null, null, "42", true, "http://site_url", "https://secure_site_url", "default", result, goConfigDao.md5OfConfigFile());

        assertThat(result.isSuccessful(), is(false));
        assertThat(result.message(localizer), is("Invalid hostname. A valid hostname can only contain letters (A-z) digits (0-9) hyphens (-) dots (.) and underscores (_)."));
        assertThat(goConfigService.getMailHost(), is(new MailHost(new GoCipher())));
    }

    @Test
    public void shouldNotUpdateWhenPortIsInvalid() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        serverConfigService.updateServerConfig(new MailHost("boo", -1, "username", "password", true, true, "from@from.com", "admin@admin.com"), new LdapConfig(new GoCipher()),
                new PasswordFileConfig(), "artifacts", null, null, "42", true, "http://site_url", "https://secure_site_url", "default", result, goConfigDao.md5OfConfigFile());

        assertThat(result.isSuccessful(), is(false));
        assertThat(result.message(localizer), is("Invalid port."));
        assertThat(goConfigService.getMailHost(), is(new MailHost(new GoCipher())));
    }

    @Test
    public void shouldNotUpdateWhenEmailIsInvalid() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        serverConfigService.updateServerConfig(new MailHost("boo", 1, "username", "password", true, true, "from", "admin@admin.com"), new LdapConfig(new GoCipher()),
                new PasswordFileConfig(),
                "artifacts", null, null, "42", true,
                "http://site_url", "https://secure_site_url", "default", result, goConfigDao.md5OfConfigFile());

        assertThat(result.isSuccessful(), is(false));
        assertThat(result.message(localizer), is("From address is not a valid email address."));
        assertThat(goConfigService.getMailHost(), is(new MailHost(new GoCipher())));
    }

    @Test
    public void shouldNotUpdateWhenAdminEmailIsInvalid() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        serverConfigService.updateServerConfig(new MailHost("boo", 1, "username", "password", true, true, "from@from.com", "admin"), new LdapConfig(new GoCipher()),
                new PasswordFileConfig(),
                "artifacts", null, null, "42", true,
                "http://site_url", "https://secure_site_url", "default", result, goConfigDao.md5OfConfigFile());

        assertThat(result.isSuccessful(), is(false));
        assertThat(result.message(localizer), is("Admin address is not a valid email address."));
        assertThat(goConfigService.getMailHost(), is(new MailHost(new GoCipher())));
    }

    @Test
    public void shouldReturnErrorResultWhenLdapSearchFails() throws Exception {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        LdapConfig invalidLdapConfig = new LdapConfig(new GoCipher());
        serverConfigService.validateLdapSettings(invalidLdapConfig, result);
        assertThat(result.isSuccessful(), is(false));
        assertThat(result.message(localizer), is("Cannot connect to ldap, please check the settings. Reason: An LDAP connection URL must be supplied."));

        result = new HttpLocalizedOperationResult();
        invalidLdapConfig = new LdapConfig("ldap://some_loser_url", MANAGER_DN, MANAGER_PASSWORD, null, true, new BasesConfig(new BaseConfig(SEARCH_BASE)), SEARCH_FILTER);
        serverConfigService.validateLdapSettings(invalidLdapConfig, result);
        assertThat(result.isSuccessful(), is(false));
        assertThat(result.message(localizer),
                is("Cannot connect to ldap, please check the settings. Reason: some_loser_url:389; nested exception is javax.naming.CommunicationException: some_loser_url:389 [Root exception is java.net.UnknownHostException: some_loser_url]"));

        result = new HttpLocalizedOperationResult();
        invalidLdapConfig = new LdapConfig(LDAP_URL, "invalidDN=1", MANAGER_PASSWORD, null, true, new BasesConfig(new BaseConfig(SEARCH_BASE)), SEARCH_FILTER);
        serverConfigService.validateLdapSettings(invalidLdapConfig, result);
        assertThat(result.isSuccessful(), is(false));
        assertThat(result.message(localizer), is("Cannot connect to ldap, please check the settings." +
                " Reason: [LDAP: error code 49 - Unable to bind as user 'invalidDN=1' because no such entry" +
                " exists in the server.]; nested exception is javax.naming.AuthenticationException:" +
                " [LDAP: error code 49 - Unable to bind as user 'invalidDN=1' because no such entry exists in the server.]"));

        result = new HttpLocalizedOperationResult();
        invalidLdapConfig = new LdapConfig(LDAP_URL, MANAGER_DN, "wrong_password", null, true, new BasesConfig(new BaseConfig(SEARCH_BASE)), SEARCH_FILTER);
        serverConfigService.validateLdapSettings(invalidLdapConfig, result);
        assertThat(result.isSuccessful(), is(false));
        assertThat(result.message(localizer), is("Cannot connect to ldap, please check the settings." +
                " Reason: [LDAP: error code 49 - Unable to bind as user 'cn=Active Directory Ldap User," +
                "ou=SomeSystems,ou=Accounts,ou=Principal,dc=corp,dc=somecompany,dc=com' because the provided" +
                " password was incorrect.]; nested exception is javax.naming.AuthenticationException:" +
                " [LDAP: error code 49 - Unable to bind as user 'cn=Active Directory Ldap User," +
                "ou=SomeSystems,ou=Accounts,ou=Principal,dc=corp,dc=somecompany,dc=com' because the provided" +
                " password was incorrect.]"));

        result = new HttpLocalizedOperationResult();
        LdapConfig validConfig = new LdapConfig(LDAP_URL, MANAGER_DN, MANAGER_PASSWORD, null, true, new BasesConfig(new BaseConfig(SEARCH_BASE)), SEARCH_FILTER);
        serverConfigService.validateLdapSettings(validConfig, result);
        assertThat("Expected no message. Got: " + result.message(localizer), result.isSuccessful(), is(true));
    }

    @Test
    public void shouldSiteUrlForGivenUrl() throws URISyntaxException {
        configHelper.setBaseUrls(new ServerSiteUrlConfig("http://foo.com"), new ServerSiteUrlConfig("https://bar.com"));
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
        configHelper.setBaseUrls(new ServerSiteUrlConfig("https://foo.com"), new ServerSiteUrlConfig());
        assertThat(serverConfigService.siteUrlFor("http://test.host/foo/bar", true), is("https://foo.com/foo/bar"));
        assertThat(serverConfigService.siteUrlFor("http://test.host/foo/bar", false), is("https://foo.com/foo/bar"));
    }

    @Test
    public void shouldUseTheSecureSiteUrlInspiteOfCallerNotForcingSsl_whenAlreadyUsingHTTPS() throws URISyntaxException {
        configHelper.setBaseUrls(new ServerSiteUrlConfig("http://foo.com:80"), new ServerSiteUrlConfig("https://bar.com:443"));
        assertThat(serverConfigService.siteUrlFor("https://test.host:1000/foo/bar", false), is("https://bar.com:443/foo/bar"));
        assertThat(serverConfigService.siteUrlFor("http://test.host/foo/bar", false), is("http://foo.com:80/foo/bar"));
    }

    @Test
    public void shouldUseTheNewPasswordIfItIsChanged() {
        LdapConfig ldapConfig = new LdapConfig(LDAP_URL, MANAGER_DN, "changed_password", "encrypted_password", true, new BasesConfig(new BaseConfig(SEARCH_BASE)), SEARCH_FILTER);
        DefaultSpringSecurityContextSource source = serverConfigService.ldapContextSource(ldapConfig);
        assertThat(source.getAuthenticationSource().getCredentials(), is("changed_password"));
    }

    @Test
    public void shouldUseTheEncryptedPasswordWhenPasswordIsNotChanged() throws InvalidCipherTextException {
        String encryptedPassword = new GoCipher().encrypt("encrypted_password");
        LdapConfig ldapConfig = new LdapConfig(LDAP_URL, MANAGER_DN, MANAGER_PASSWORD, encryptedPassword, false, new BasesConfig(new BaseConfig(SEARCH_BASE)), SEARCH_FILTER);
        DefaultSpringSecurityContextSource source = serverConfigService.ldapContextSource(ldapConfig);
        assertThat(source.getAuthenticationSource().getCredentials(), is("encrypted_password"));
    }

}
