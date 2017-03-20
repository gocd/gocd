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
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationExtension;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationMetadataStore;
import com.thoughtworks.go.plugin.api.response.validation.ValidationError;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.domain.authorization.AuthorizationPluginInfo;
import com.thoughtworks.go.plugin.domain.authorization.Capabilities;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import org.junit.Before;
import org.junit.Test;

import static com.thoughtworks.go.plugin.domain.authorization.SupportedAuthType.Password;
import static org.mockito.Mockito.*;

public class SecurityAuthConfigServiceTest {

    private AuthorizationMetadataStore store;
    private AuthorizationExtension extension;
    private EntityHashingService hashingService;
    private GoConfigService goConfigService;
    private SecurityAuthConfigService securityAuthConfigService;
    private LocalizedOperationResult resultSpy;
    private SecurityAuthConfig ldap;
    private AuthorizationPluginInfo pluginInfo;

    @Before
    public void setUp() throws Exception {
        store = mock(AuthorizationMetadataStore.class);
        extension = mock(AuthorizationExtension.class);
        hashingService = mock(EntityHashingService.class);
        goConfigService = mock(GoConfigService.class);
        securityAuthConfigService = new SecurityAuthConfigService(goConfigService, hashingService, extension, store);
        resultSpy = spy(new HttpLocalizedOperationResult());
        ldap = new SecurityAuthConfig("ldap", "cd.go.ldap");
        pluginInfo = mock(AuthorizationPluginInfo.class);
    }

    @Test
    public void verifyConnection_shouldSendSuccessResponseOnSuccessfulVerification() throws Exception {
        final Capabilities capabilities = new Capabilities(Password, true, true);
        final ValidationResult validationResult = new ValidationResult();

        when(store.getPluginInfo("cd.go.ldap")).thenReturn(pluginInfo);
        when(pluginInfo.getCapabilities()).thenReturn(capabilities);
        when(extension.verifyConnection("cd.go.ldap", ldap.getConfigurationAsMap(true))).thenReturn(validationResult);

        securityAuthConfigService.verifyConnection(ldap, resultSpy);

        verifyNoMoreInteractions(resultSpy);
    }

    @Test
    //Authorization extension validates the security auth config before verify connection call
    public void verifyConnection_shouldSendConnectionFailedResponseOnValidationFailed() throws Exception {
        final Capabilities capabilities = new Capabilities(Password, true, true);
        final ValidationResult validationResult = new ValidationResult();
        validationResult.addError(new ValidationError("url", "some-error"));

        when(store.getPluginInfo("cd.go.ldap")).thenReturn(pluginInfo);
        when(pluginInfo.getCapabilities()).thenReturn(capabilities);
        when(extension.verifyConnection("cd.go.ldap", ldap.getConfigurationAsMap(true))).thenReturn(validationResult);

        securityAuthConfigService.verifyConnection(ldap, resultSpy);

        verify(resultSpy).unprocessableEntity(LocalizedMessage.string("CHECK_CONNECTION_FAILED", "ldap", "Could not verify connection!"));
    }

    @Test
    public void verifyConnection_shouldSendConnectionFailedResponseOnUnSuccessfulVerification() throws Exception {
        final Capabilities capabilities = new Capabilities(Password, true, true);
        final ValidationResult validationResult = new ValidationResult();
        validationResult.addError(new ValidationError("", "some-error"));

        when(store.getPluginInfo("cd.go.ldap")).thenReturn(pluginInfo);
        when(pluginInfo.getCapabilities()).thenReturn(capabilities);
        when(extension.verifyConnection("cd.go.ldap", ldap.getConfigurationAsMap(true))).thenReturn(validationResult);

        securityAuthConfigService.verifyConnection(ldap, resultSpy);

        verify(resultSpy).unprocessableEntity(LocalizedMessage.string("CHECK_CONNECTION_FAILED", "ldap", "some-error"));
    }
}