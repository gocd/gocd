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
package com.thoughtworks.go.config;

import com.thoughtworks.go.domain.SecureSiteUrl;
import com.thoughtworks.go.domain.SiteUrl;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

public class ServerConfigTest {
    private ServerConfig defaultServerConfig;
    private ServerConfig another;

    @Before
    public void setUp() {
        defaultServerConfig = new ServerConfig("artifactsDir", new SecurityConfig());
        another = new ServerConfig("artifactsDir", new SecurityConfig());
    }

    @Test
    public void shouldReturnSiteUrlAsSecurePreferedSiteUrlIfSecureSiteUrlIsNotDefined() {
        defaultServerConfig.setSiteUrl("http://example.com");
        defaultServerConfig.setSecureSiteUrl(null);
        assertThat(defaultServerConfig.getSiteUrlPreferablySecured().getUrl(), is("http://example.com"));
    }

    @Test
    public void shouldReturnSecureSiteUrlAsSecurePreferedSiteUrlIfBothSiteUrlAndSecureSiteUrlIsDefined() {
        defaultServerConfig.setSiteUrl("http://example.com");
        defaultServerConfig.setSecureSiteUrl("https://example.com");
        assertThat(defaultServerConfig.getSiteUrlPreferablySecured().getUrl(), is("https://example.com"));
    }

    @Test
    public void shouldReturnBlankUrlBothSiteUrlAndSecureSiteUrlIsNotDefined() {
        defaultServerConfig.setSiteUrl(null);
        defaultServerConfig.setSecureSiteUrl(null);
        assertThat(defaultServerConfig.getSiteUrlPreferablySecured().hasNonNullUrl(), is(false));
    }

    @Test
    public void shouldReturnAnEmptyForSecureSiteUrlIfOnlySiteUrlIsConfigured() throws Exception {
        ServerConfig serverConfig = new ServerConfig(null, null, new SiteUrl("http://foo.bar:813"), new SecureSiteUrl());
        assertThat(serverConfig.getHttpsUrl(), is(new SecureSiteUrl()));
    }

    @Test
    public void shouldReturnDefaultTaskRepositoryLocation() {
        ServerConfig serverConfig = new ServerConfig(null, null, new SiteUrl("http://foo.bar:813"), new SecureSiteUrl());
        assertThat(serverConfig.getCommandRepositoryLocation(), is("default"));
    }

    @Test
    public void shouldReturnTaskRepositoryLocation() {
        ServerConfig serverConfig = new ServerConfig(null, null, new SiteUrl("http://foo.bar:813"), new SecureSiteUrl());
        serverConfig.setCommandRepositoryLocation("foo");
        assertThat(serverConfig.getCommandRepositoryLocation(), is("foo"));
    }

    @Test
    public void shouldIgnoreErrorsFieldOnEquals() throws Exception {
        ServerConfig one = new ServerConfig(new SecurityConfig(), new MailHost(new GoCipher()), new SiteUrl("siteURL"), new SecureSiteUrl("secureURL"));
        one.addError("siteUrl", "I dont like this url");
        assertThat(one, is(new ServerConfig(new SecurityConfig(), new MailHost(new GoCipher()), new SiteUrl("siteURL"), new SecureSiteUrl("secureURL"))));
    }

    @Test
    public void shouldNotUpdatePasswordForMailHostIfNotChangedOrNull() throws IOException {
        File cipherFile = new SystemEnvironment().getDESCipherFile();
        FileUtils.deleteQuietly(cipherFile);
        FileUtils.writeStringToFile(cipherFile, "269298bc31c44620", UTF_8);
        GoCipher goCipher = new GoCipher();
        MailHost mailHost = new MailHost("abc", 12, "admin", "p", null, true, true, "anc@mail.com", "anc@mail.com", goCipher);
        ServerConfig serverConfig = new ServerConfig(null, mailHost, null, null);
        assertThat(serverConfig.mailHost().getPassword(), is("p"));

        String encryptedPassword = serverConfig.mailHost().getEncryptedPassword();
        serverConfig.updateMailHost(new MailHost("abc", 12, "admin", "p", encryptedPassword, false /* Password Not Changed */, true, "anc@mail.com", "anc@mail.com", goCipher));
        assertThat(serverConfig.mailHost().getPassword(), is("p"));
        assertThat(serverConfig.mailHost().getEncryptedPassword(), is(encryptedPassword));

        serverConfig.updateMailHost(new MailHost("abc", 12, "admin", null, "", true, true, "anc@mail.com", "anc@mail.com"));
        assertThat(serverConfig.mailHost().getPassword(), is(nullValue()));
        assertThat(serverConfig.mailHost().getEncryptedPassword(), is(nullValue()));
    }

    @Test
    public void shouldAllowArtifactPurgingIfPurgeParametersAreDefined() {
        another = new ServerConfig("artifacts", new SecurityConfig(), 10.0, 20.0);
        assertThat(another.isArtifactPurgingAllowed(), is(true));
        another = new ServerConfig("artifacts", new SecurityConfig(), null, 20.0);
        assertThat(another.isArtifactPurgingAllowed(), is(false));
        another = new ServerConfig("artifacts", new SecurityConfig(), 10.0, null);
        assertThat(another.isArtifactPurgingAllowed(), is(false));
        another = new ServerConfig("artifacts", new SecurityConfig(), null, null);
        assertThat(another.isArtifactPurgingAllowed(), is(false));
    }

    @Test
    public void shouldGetTheDefaultJobTimeoutValue() {
        assertThat(new ServerConfig("artifacts", new SecurityConfig(), 10.0, 20.0).getJobTimeout(), is("0"));
        assertThat(new ServerConfig("artifacts", new SecurityConfig(), 10.0, 20.0, "30").getJobTimeout(), is("30"));
    }

    @Test
    public void shouldValidateThatTimeoutIsValidIfItsANumber() {
        ServerConfig serverConfig = new ServerConfig("artifacts", new SecurityConfig(), 10, 20, "30");
        serverConfig.validate(null);
        assertThat(serverConfig.errors().isEmpty(), is(true));
    }

    @Test
    public void shouldValidateThatTimeoutIsInvalidIfItsNotAValidNumber() {
        ServerConfig serverConfig = new ServerConfig("artifacts", new SecurityConfig(), 10, 20, "30M");
        serverConfig.validate(null);
        assertThat(serverConfig.errors().isEmpty(), is(false));
        assertThat(serverConfig.errors().on(ServerConfig.JOB_TIMEOUT), is("Timeout should be a valid number as it represents number of minutes"));
    }

    @Test
    public void should_useServerId_forEqualityCheck() {
        ServerConfig configWithoutServerId = new ServerConfig();
        ServerConfig configWithServerId = new ServerConfig();
        configWithServerId.ensureServerIdExists();
        assertThat(configWithoutServerId, not(configWithServerId));
    }

    @Test
    public void shouldEnsureAgentAutoregisterKeyExists() throws Exception {
        ServerConfig serverConfig = new ServerConfig();
        assertNull(serverConfig.getAgentAutoRegisterKey());
        assertNotNull(serverConfig.getClass().getMethod("ensureAgentAutoregisterKeyExists").getAnnotation(PostConstruct.class));
        serverConfig.ensureAgentAutoregisterKeyExists();
        assertTrue(StringUtils.isNotBlank(serverConfig.getAgentAutoRegisterKey()));
    }

    @Test
    public void shouldEnsureWebhookSecretExists() throws Exception {
        ServerConfig serverConfig = new ServerConfig();
        assertNull(serverConfig.getWebhookSecret());
        assertNotNull(serverConfig.getClass().getMethod("ensureWebhookSecretExists").getAnnotation(PostConstruct.class));
        serverConfig.ensureWebhookSecretExists();
        assertTrue(StringUtils.isNotBlank(serverConfig.getWebhookSecret()));
    }

    @Test
    public void shouldEnsureTokenGenerationKeyExists() throws Exception {
        ServerConfig serverConfig = new ServerConfig();
        assertNull(serverConfig.getTokenGenerationKey());
        assertNotNull(serverConfig.getClass().getMethod("ensureTokenGenerationKeyExists").getAnnotation(PostConstruct.class));
        serverConfig.ensureTokenGenerationKeyExists();
        assertTrue(StringUtils.isNotBlank(serverConfig.getTokenGenerationKey()));
    }


    @Test
    public void shouldEnsureArtifactConfigWithArtifactDirectoryExists() throws Exception {
        ServerConfig serverConfig = new ServerConfig();
        assertNull(serverConfig.getArtifactConfig().getArtifactsDir().getArtifactDir());
        assertNotNull(serverConfig.getClass().getMethod("ensureArtifactConfigExists").getAnnotation(PostConstruct.class));
        serverConfig.ensureArtifactConfigExists();
        assertTrue(StringUtils.isNotBlank(serverConfig.getArtifactConfig().getArtifactsDir().getArtifactDir()));
    }
}
