/*
 * Copyright Thoughtworks, Inc.
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
import com.thoughtworks.go.config.exceptions.GoConfigInvalidException;
import com.thoughtworks.go.domain.SecureSiteUrl;
import com.thoughtworks.go.domain.SiteUrl;
import com.thoughtworks.go.util.GoConfigFileHelper;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
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

    private final GoConfigFileHelper configHelper = new GoConfigFileHelper();

    @BeforeEach
    public void setup() throws Exception {
        configHelper.usingCruiseConfigDao(goConfigDao);
        configHelper.onSetUp();

        goConfigService.forceNotifyListeners();
        goConfigService.security().securityAuthConfigs().add(new SecurityAuthConfig("file", "cd.go.authentication.passwordfile"));
    }

    @AfterEach
    public void tearDown() {
        configHelper.onTearDown();
    }

    @Test
    public void shouldPreferTheSecureSiteUrl() throws Exception {
        configHelper.setBaseUrls(new SiteUrl("http://foo.com"), new SecureSiteUrl("https://bar.com"));
        assertThat(serverConfigService.siteUrlWithPath("/foo/bar")).isEqualTo(url("https://bar.com/go/foo/bar"));
    }

    @Test
    public void shouldReturnAComputedUrlWhenNothingIsConfigured() throws Exception {
        assertThat(serverConfigService.siteUrlWithPath("/foo/bar").toString()).matches("http://.*:8153/go/foo/bar");
    }

    @Test
    public void shouldUseTheSiteUrlWhenSecureSiteUrlIsNotPresent() throws Exception {
        configHelper.setBaseUrls(new SiteUrl("https://foo.com"), new SecureSiteUrl());
        assertThat(serverConfigService.siteUrlWithPath("/foo/bar")).isEqualTo(url("https://foo.com/go/foo/bar"));
        configHelper.setBaseUrls(new SiteUrl("http://foo.com"), new SecureSiteUrl());
        assertThat(serverConfigService.siteUrlWithPath("/foo/bar")).isEqualTo(url("http://foo.com/go/foo/bar"));
    }

    private static @NonNull URL url(String url) throws MalformedURLException {
        return URI.create(url).toURL();
    }

    @Test
    public void shouldSetDefaultJobTimeout() {
        serverConfigService.createOrUpdateDefaultJobTimeout("10");

        assertThat(serverConfigService.getDefaultJobTimeout()).isEqualTo("10");
    }

    @Test
    public void shouldUpdateDefaultJobTimeout() {
        serverConfigService.createOrUpdateDefaultJobTimeout("10");

        assertThat(serverConfigService.getDefaultJobTimeout()).isEqualTo("10");

        serverConfigService.createOrUpdateDefaultJobTimeout("5");

        assertThat(serverConfigService.getDefaultJobTimeout()).isEqualTo("5");
    }

    @Test
    public void shouldUpdateArtifactConfigWithoutPurgeSettingsIfValid() {
        ArtifactConfig artifactConfig = new ArtifactConfig();
        artifactConfig.setArtifactsDir(new ArtifactDirectory("test"));

        assertThat(goConfigService.serverConfig().artifactsDir()).isEqualTo("artifactsDir");

        serverConfigService.updateArtifactConfig(artifactConfig);

        assertThat(goConfigService.serverConfig().artifactsDir()).isEqualTo("test");
    }

    @Test
    public void shouldUpdateArtifactConfigWithPurgeSettingsIfValid() {
        ArtifactConfig artifactConfig = new ArtifactConfig();
        artifactConfig.setArtifactsDir(new ArtifactDirectory("test"));
        PurgeSettings purgeSettings = new PurgeSettings();
        purgeSettings.setPurgeStart(new PurgeStart(10.0));
        purgeSettings.setPurgeUpto(new PurgeUpto(20.0));
        artifactConfig.setPurgeSettings(purgeSettings);

        assertThat(goConfigService.serverConfig().artifactsDir()).isEqualTo("artifactsDir");
        assertNull(goConfigService.serverConfig().getPurgeStartDiskSpaceInGigabytes());
        assertNull(goConfigService.serverConfig().getPurgeUptoDiskSpaceInGigabytes());

        serverConfigService.updateArtifactConfig(artifactConfig);

        assertThat(goConfigService.serverConfig().artifactsDir()).isEqualTo("test");
        assertThat(goConfigService.serverConfig().getPurgeStartDiskSpaceInGigabytes()).isEqualTo(10.0);
        assertThat(goConfigService.serverConfig().getPurgeUptoDiskSpaceInGigabytes()).isEqualTo(20.0);
    }

    @Test
    public void shouldNotUpdateArtifactConfigIfInvalid() {
        ArtifactConfig artifactConfig = new ArtifactConfig();

        assertThat(goConfigService.serverConfig().artifactsDir()).isEqualTo("artifactsDir");

        assertThatThrownBy(() -> serverConfigService.updateArtifactConfig(artifactConfig))
                .isInstanceOf(GoConfigInvalidException.class);
    }
}
