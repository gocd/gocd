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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

class ServerConfigTest {
    private ServerConfig defaultServerConfig;
    private ServerConfig another;
    private BasicCruiseConfig basicCruiseConfig;
    private ConfigSaveValidationContext validationContext;

    @BeforeEach
    void setUp() {
        defaultServerConfig = new ServerConfig("artifactsDir", new SecurityConfig());
        another = new ServerConfig("artifactsDir", new SecurityConfig());
        basicCruiseConfig = new BasicCruiseConfig();
        validationContext = ConfigSaveValidationContext.forChain(basicCruiseConfig);
    }

    @Test
    void shouldReturnSiteUrlAsSecurePreferedSiteUrlIfSecureSiteUrlIsNotDefined() {
        defaultServerConfig.setSiteUrl("http://example.com");
        defaultServerConfig.setSecureSiteUrl(null);
        assertThat(defaultServerConfig.getSiteUrlPreferablySecured().getUrl(), is("http://example.com"));
    }

    @Test
    void shouldReturnSecureSiteUrlAsSecurePreferedSiteUrlIfBothSiteUrlAndSecureSiteUrlIsDefined() {
        defaultServerConfig.setSiteUrl("http://example.com");
        defaultServerConfig.setSecureSiteUrl("https://example.com");
        assertThat(defaultServerConfig.getSiteUrlPreferablySecured().getUrl(), is("https://example.com"));
    }

    @Test
    void shouldReturnBlankUrlBothSiteUrlAndSecureSiteUrlIsNotDefined() {
        defaultServerConfig.setSiteUrl(null);
        defaultServerConfig.setSecureSiteUrl(null);
        assertThat(defaultServerConfig.getSiteUrlPreferablySecured().hasNonNullUrl(), is(false));
    }

    @Test
    void shouldReturnAnEmptyForSecureSiteUrlIfOnlySiteUrlIsConfigured() throws Exception {
        ServerConfig serverConfig = new ServerConfig(null, null, new SiteUrl("http://foo.bar:813"), new SecureSiteUrl());
        assertThat(serverConfig.getHttpsUrl(), is(new SecureSiteUrl()));
    }

    @Test
    void shouldReturnDefaultTaskRepositoryLocation() {
        ServerConfig serverConfig = new ServerConfig(null, null, new SiteUrl("http://foo.bar:813"), new SecureSiteUrl());
        assertThat(serverConfig.getCommandRepositoryLocation(), is("default"));
    }

    @Test
    void shouldReturnTaskRepositoryLocation() {
        ServerConfig serverConfig = new ServerConfig(null, null, new SiteUrl("http://foo.bar:813"), new SecureSiteUrl());
        serverConfig.setCommandRepositoryLocation("foo");
        assertThat(serverConfig.getCommandRepositoryLocation(), is("foo"));
    }

    @Test
    void shouldIgnoreErrorsFieldOnEquals() throws Exception {
        ServerConfig one = new ServerConfig(new SecurityConfig(), new MailHost(new GoCipher()), new SiteUrl("siteURL"), new SecureSiteUrl("secureURL"));
        one.addError("siteUrl", "I dont like this url");
        assertThat(one, is(new ServerConfig(new SecurityConfig(), new MailHost(new GoCipher()), new SiteUrl("siteURL"), new SecureSiteUrl("secureURL"))));
    }

    @Test
    void shouldNotUpdatePasswordForMailHostIfNotChangedOrNull() throws IOException {
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
    void shouldAllowArtifactPurgingIfPurgeParametersAreDefined() {
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
    void shouldGetTheDefaultJobTimeoutValue() {
        assertThat(new ServerConfig("artifacts", new SecurityConfig(), 10.0, 20.0).getJobTimeout(), is("0"));
        assertThat(new ServerConfig("artifacts", new SecurityConfig(), 10.0, 20.0, "30").getJobTimeout(), is("30"));
    }

    @Test
    void shouldValidateThatTimeoutIsValidIfItsANumber() {
        ServerConfig serverConfig = new ServerConfig("artifacts", new SecurityConfig(), 10, 20, "30");
        serverConfig.validate(validationContext);
        assertThat(serverConfig.errors().isEmpty(), is(true));
    }

    @Test
    void shouldValidateThatTimeoutIsInvalidIfItsNotAValidNumber() {
        ServerConfig serverConfig = new ServerConfig("artifacts", new SecurityConfig(), 10, 20, "30M");
        serverConfig.validate(null);
        assertThat(serverConfig.errors().isEmpty(), is(false));
        assertThat(serverConfig.errors().on(ServerConfig.JOB_TIMEOUT), is("Timeout should be a valid number as it represents number of minutes"));
    }

    @Test
    void validate_shouldFailIfThePurgeStartIsBiggerThanPurgeUpto() {
        ServerConfig serverConfig = new ServerConfig("artifacts", new SecurityConfig(), 20.1, 20.05, "30");
        serverConfig.validate(null);
        assertThat(serverConfig.errors().isEmpty(), is(false));
        assertThat(serverConfig.errors().on(ServerConfig.PURGE_START), is("Error in artifact cleanup values. The trigger value (20.1GB) should be less than the goal (20.05GB)"));
    }

    @Test
    void validate_shouldFailIfThePurgeStartIsNotSpecifiedButPurgeUptoIs() {
        ServerConfig serverConfig = new ServerConfig("artifacts", new SecurityConfig(), null, 20.05);
        serverConfig.validate(null);
        assertThat(serverConfig.errors().isEmpty(), is(false));
        assertThat(serverConfig.errors().on(ServerConfig.PURGE_START), is("Error in artifact cleanup values. The trigger value is has to be specified when a goal is set"));
    }

    @Test
    void validate_shouldFailIfThePurgeStartIs0SpecifiedButPurgeUptoIs() {
        ServerConfig serverConfig = new ServerConfig("artifacts", new SecurityConfig(), 0, 20.05, "30");
        serverConfig.validate(null);
        assertThat(serverConfig.errors().isEmpty(), is(false));
        assertThat(serverConfig.errors().on(ServerConfig.PURGE_START), is("Error in artifact cleanup values. The trigger value is has to be specified when a goal is set"));
    }

    @Test
    void validate_shouldPassIfThePurgeStartIsSmallerThanPurgeUpto() {
        ServerConfig serverConfig = new ServerConfig("artifacts", new SecurityConfig(), 20.0, 20.05, "30");
        serverConfig.validate(validationContext);
        assertThat(serverConfig.errors().isEmpty(), is(true));
    }

    @Test
    void validate_shouldPassIfThePurgeStartAndPurgeUptoAreBothNotSet() {
        ServerConfig serverConfig = new ServerConfig("artifacts", new SecurityConfig());
        serverConfig.validate(validationContext);
        assertThat(serverConfig.errors().isEmpty(), is(true));
    }

    @Test
    void should_useServerId_forEqualityCheck() {
        ServerConfig configWithoutServerId = new ServerConfig();
        ServerConfig configWithServerId = new ServerConfig();
        configWithServerId.ensureServerIdExists();
        assertThat(configWithoutServerId, not(configWithServerId));
    }

    @Test
    void shouldEnsureAgentAutoregisterKeyExists() throws Exception {
        ServerConfig serverConfig = new ServerConfig();
        assertNull(serverConfig.getAgentAutoRegisterKey());
        assertNotNull(serverConfig.getClass().getMethod("ensureAgentAutoregisterKeyExists").getAnnotation(PostConstruct.class));
        serverConfig.ensureAgentAutoregisterKeyExists();
        assertTrue(StringUtils.isNotBlank(serverConfig.getAgentAutoRegisterKey()));
    }

    @Test
    void shouldEnsureWebhookSecretExists() throws Exception {
        ServerConfig serverConfig = new ServerConfig();
        assertNull(serverConfig.getWebhookSecret());
        assertNotNull(serverConfig.getClass().getMethod("ensureWebhookSecretExists").getAnnotation(PostConstruct.class));
        serverConfig.ensureWebhookSecretExists();
        assertTrue(StringUtils.isNotBlank(serverConfig.getWebhookSecret()));
    }

    @Test
    void shouldEnsureTokenGenerationKeyExists() throws Exception {
        ServerConfig serverConfig = new ServerConfig();
        assertNull(serverConfig.getTokenGenerationKey());
        assertNotNull(serverConfig.getClass().getMethod("ensureTokenGenerationKeyExists").getAnnotation(PostConstruct.class));
        serverConfig.ensureTokenGenerationKeyExists();
        assertTrue(StringUtils.isNotBlank(serverConfig.getTokenGenerationKey()));
    }

    @Test
    void shouldValidateCommandRepoLocationIsValidFile() {
        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setCommandRepositoryLocation("command-repo");

        serverConfig.validate(validationContext);

        assertTrue(serverConfig.errors().isEmpty());
    }

    @Test
    void shouldErrorWhenCommandRepoLocationIsEmpty() {
        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setCommandRepositoryLocation("");
        basicCruiseConfig.setServerConfig(serverConfig);

        serverConfig.validate(validationContext);

        assertFalse(serverConfig.errors().isEmpty());
        assertThat(serverConfig.errors().on(ServerConfig.COMMAND_REPO_LOCATION), is("Command Repository Location cannot be empty"));
    }
}
