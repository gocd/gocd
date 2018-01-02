/*
 * Copyright 2017 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.SecurityAuthConfig;
import com.thoughtworks.go.config.SecurityConfig;
import com.thoughtworks.go.config.ServerConfig;
import com.thoughtworks.go.domain.config.ConfigurationKey;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.config.ConfigurationValue;
import com.thoughtworks.go.plugin.access.PluginNotFoundException;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationExtension;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationMetadataStore;
import com.thoughtworks.go.plugin.domain.authorization.AuthorizationPluginInfo;
import com.thoughtworks.go.plugin.domain.authorization.Capabilities;
import com.thoughtworks.go.plugin.domain.authorization.SupportedAuthType;
import com.thoughtworks.go.plugin.domain.common.Image;
import com.thoughtworks.go.plugin.domain.common.ValidationError;
import com.thoughtworks.go.plugin.domain.common.ValidationResult;
import com.thoughtworks.go.plugin.domain.common.VerifyConnectionResponse;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.ui.AuthPluginInfoViewModel;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SecurityAuthConfigServiceTest {

    private AuthorizationExtension extension;
    private EntityHashingService hashingService;
    private GoConfigService goConfigService;
    private SecurityAuthConfigService securityAuthConfigService;
    private AuthorizationMetadataStore authorizationMetadataStore;

    @Before
    public void setUp() throws Exception {
        extension = mock(AuthorizationExtension.class);
        hashingService = mock(EntityHashingService.class);
        goConfigService = mock(GoConfigService.class);
        authorizationMetadataStore = mock(AuthorizationMetadataStore.class);
        securityAuthConfigService = new SecurityAuthConfigService(goConfigService, hashingService, extension, authorizationMetadataStore);
    }

    @Test
    public void verifyConnection_shouldSendSuccessResponseOnSuccessfulVerification() throws Exception {
        VerifyConnectionResponse success = new VerifyConnectionResponse("success", "Connection check passed", new ValidationResult());
        SecurityAuthConfig ldap = new SecurityAuthConfig("ldap", "cd.go.ldap");

        when(extension.verifyConnection("cd.go.ldap", ldap.getConfigurationAsMap(true))).thenReturn(success);

        VerifyConnectionResponse response = securityAuthConfigService.verifyConnection(ldap);

        assertThat(response, is(success));
    }

    @Test
    public void verifyConnection_shouldFailForAInvalidAuthConfig() throws Exception {
        SecurityAuthConfig ldap = new SecurityAuthConfig("ldap", "cd.go.ldap",
                new ConfigurationProperty(new ConfigurationKey("username"), new ConfigurationValue()));
        ValidationResult validationResult = new ValidationResult();
        validationResult.addError(new ValidationError("password", "Password cannot be blank"));
        validationResult.addError(new ValidationError("username", "Username cannot be blank"));

        VerifyConnectionResponse validationFailed = new VerifyConnectionResponse("validation-failed", "Connection check passed", validationResult);

        when(extension.verifyConnection("cd.go.ldap", ldap.getConfigurationAsMap(true))).thenReturn(validationFailed);

        VerifyConnectionResponse response = securityAuthConfigService.verifyConnection(ldap);

        assertThat(response, is(validationFailed));
        assertThat(ldap.getProperty("username").errors().get("username").get(0), is("Username cannot be blank"));
        assertThat(ldap.getProperty("password").errors().get("password").get(0), is("Password cannot be blank"));
    }

    @Test
    public void verifyConnection_shouldSendConnectionFailedResponseOnUnSuccessfulVerification() throws Exception {
        VerifyConnectionResponse success = new VerifyConnectionResponse("failure", "Connection check failed", new ValidationResult());
        SecurityAuthConfig ldap = new SecurityAuthConfig("ldap", "cd.go.ldap");

        when(extension.verifyConnection("cd.go.ldap", ldap.getConfigurationAsMap(true))).thenReturn(success);

        VerifyConnectionResponse response = securityAuthConfigService.verifyConnection(ldap);

        assertThat(response, is(success));
    }

    @Test
    public void verifyConnection_shouldFailInAbsenceOfPlugin() throws Exception {
        SecurityAuthConfig ldap = new SecurityAuthConfig("ldap", "cd.go.ldap");

        when(extension.verifyConnection("cd.go.ldap", ldap.getConfigurationAsMap(true))).thenThrow(new PluginNotFoundException(""));

        VerifyConnectionResponse response = securityAuthConfigService.verifyConnection(ldap);

        assertThat(response, is(new VerifyConnectionResponse("failure", "Unable to verify connection, missing plugin: cd.go.ldap",
                new com.thoughtworks.go.plugin.domain.common.ValidationResult())));
    }

    @Test
    public void shouldGetAListOfAllConfiguredWebBasedAuthorizationPlugins() {
        Set<AuthorizationPluginInfo> installedWebBasedPlugins = new HashSet<>();
        String githubPluginId = "cd.go.github";
        AuthorizationPluginInfo githubPluginInfo = pluginInfo(githubPluginId, "GitHub Auth Plugin", SupportedAuthType.Web);
        installedWebBasedPlugins.add(githubPluginInfo);
        installedWebBasedPlugins.add(pluginInfo(githubPluginId, "Google Auth Plugin", SupportedAuthType.Web));
        when(authorizationMetadataStore.getPluginsThatSupportsWebBasedAuthentication()).thenReturn(installedWebBasedPlugins);
        when(authorizationMetadataStore.getPluginInfo(githubPluginId)).thenReturn(githubPluginInfo);

        SecurityConfig securityConfig = new SecurityConfig();
        SecurityAuthConfig github = new SecurityAuthConfig("github", githubPluginId);
        SecurityAuthConfig ldap = new SecurityAuthConfig("ldap", "cd.go.ldap");
        securityConfig.securityAuthConfigs().add(github);

        securityConfig.securityAuthConfigs().add(ldap);
        when(goConfigService.serverConfig()).thenReturn(new ServerConfig(securityConfig, null));

        List<AuthPluginInfoViewModel> allWebBasedAuthorizationConfigs = securityAuthConfigService.getAllConfiguredWebBasedAuthorizationPlugins();
        assertThat(allWebBasedAuthorizationConfigs.size(), is(1));
        AuthPluginInfoViewModel pluginInfoViewModel = allWebBasedAuthorizationConfigs.get(0);
        assertThat(pluginInfoViewModel.pluginId(), is(githubPluginId));
        assertThat(pluginInfoViewModel.name(), is("GitHub Auth Plugin"));
        assertThat(pluginInfoViewModel.imageUrl(), is("/go/api/plugin_images/cd.go.github/hash"));
    }

    private AuthorizationPluginInfo pluginInfo(String githubPluginId, String name, SupportedAuthType supportedAuthType) {
        GoPluginDescriptor.About about = new GoPluginDescriptor.About(name, "1.0", null, null, null, null);
        GoPluginDescriptor descriptor = new GoPluginDescriptor(githubPluginId, "1.0", about, null, null, false);
        return new AuthorizationPluginInfo(descriptor, null, null, new Image("svg", "data", "hash"), new Capabilities(supportedAuthType, true, true), null);
    }
}
