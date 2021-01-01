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
package com.thoughtworks.go.plugin.access.secrets.v1;

import com.thoughtworks.go.config.SecretConfig;
import com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother;
import com.thoughtworks.go.plugin.access.PluginRequestHelper;
import com.thoughtworks.go.plugin.access.exceptions.SecretResolutionFailureException;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
import com.thoughtworks.go.plugin.api.response.validation.ValidationError;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.domain.common.Image;
import com.thoughtworks.go.plugin.domain.common.Metadata;
import com.thoughtworks.go.plugin.domain.common.PluginConfiguration;
import com.thoughtworks.go.plugin.domain.secrets.Secret;
import com.thoughtworks.go.plugin.infra.PluginManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.util.HashSet;
import java.util.List;

import static com.thoughtworks.go.plugin.access.secrets.SecretsPluginConstants.*;
import static com.thoughtworks.go.plugin.domain.common.PluginConstants.SECRETS_EXTENSION;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class SecretsExtensionV1Test {
    @Mock
    private PluginManager pluginManager;
    protected ArgumentCaptor<GoPluginApiRequest> requestArgumentCaptor;
    private PluginRequestHelper pluginRequestHelper;
    private String PLUGIN_ID = "cd.go.example.secrets_plugin";
    private SecretsExtensionV1 secretsExtensionV1;

    @BeforeEach
    void setUp() {
        initMocks(this);

        this.pluginRequestHelper = new PluginRequestHelper(pluginManager, asList("1.0"), SECRETS_EXTENSION);
        this.requestArgumentCaptor = ArgumentCaptor.forClass(GoPluginApiRequest.class);
        this.secretsExtensionV1 = new SecretsExtensionV1(pluginRequestHelper);

        when(pluginManager.isPluginOfType(SECRETS_EXTENSION, PLUGIN_ID)).thenReturn(true);
        when(pluginManager.resolveExtensionVersion(PLUGIN_ID, SECRETS_EXTENSION, asList("1.0"))).thenReturn("1.0");
    }

    @Test
    void shouldTalkToPlugin_toGetIcon() {
        when(pluginManager.submitTo(eq(PLUGIN_ID), eq(SECRETS_EXTENSION), requestArgumentCaptor.capture()))
                .thenReturn(DefaultGoPluginApiResponse.success("{\"content_type\":\"image/png\",\"data\":\"Zm9vYmEK\"}"));

        final Image icon = secretsExtensionV1.getIcon(PLUGIN_ID);

        assertThat(icon.getContentType()).isEqualTo("image/png");
        assertThat(icon.getData()).isEqualTo("Zm9vYmEK");
        assertExtensionRequest(REQUEST_GET_PLUGIN_ICON, null);
    }

    @Test
    void shouldTalkToPlugin_toFetchSecretsConfigMetadata() {
        String responseBody = "[{\"key\":\"Username\",\"metadata\":{\"required\":true,\"secure\":false}},{\"key\":\"Password\",\"metadata\":{\"required\":true,\"secure\":true}}]";
        when(pluginManager.submitTo(eq(PLUGIN_ID), eq(SECRETS_EXTENSION), requestArgumentCaptor.capture())).thenReturn(DefaultGoPluginApiResponse.success(responseBody));

        final List<PluginConfiguration> metadata = secretsExtensionV1.getSecretsConfigMetadata(PLUGIN_ID);

        assertThat(metadata).hasSize(2);
        assertThat(metadata).contains(new PluginConfiguration("Username", new Metadata(true, false)), new PluginConfiguration("Password", new Metadata(true, true)));

        assertExtensionRequest(REQUEST_GET_SECRETS_CONFIG_METADATA, null);
    }

    @Test
    void shouldTalkToPlugin_toFetchSecretsConfigView() {
        String responseBody = "{ \"template\": \"<div>This is secrets config view snippet</div>\" }";
        when(pluginManager.submitTo(eq(PLUGIN_ID), eq(SECRETS_EXTENSION), requestArgumentCaptor.capture())).thenReturn(DefaultGoPluginApiResponse.success(responseBody));

        final String view = secretsExtensionV1.getSecretsConfigView(PLUGIN_ID);

        assertThat(view).isEqualTo("<div>This is secrets config view snippet</div>");

        assertExtensionRequest(REQUEST_GET_SECRETS_CONFIG_VIEW, null);
    }

    @Test
    void shouldTalkToPlugin_toValidateSecretsConfig() {
        String responseBody = "[{\"message\":\"Vault Url cannot be blank.\",\"key\":\"Url\"},{\"message\":\"Path cannot be blank.\",\"key\":\"Path\"}]";
        when(pluginManager.submitTo(eq(PLUGIN_ID), eq(SECRETS_EXTENSION), requestArgumentCaptor.capture())).thenReturn(DefaultGoPluginApiResponse.success(responseBody));

        final ValidationResult result = secretsExtensionV1.validateSecretsConfig(PLUGIN_ID, singletonMap("username", "some_name"));

        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.getErrors()).contains(new ValidationError("Url", "Vault Url cannot be blank."), new ValidationError("Path", "Path cannot be blank."));

        assertExtensionRequest(REQUEST_VALIDATE_SECRETS_CONFIG, "{\"username\":\"some_name\"}");
    }

    @Nested
    class lookupSecrets {
        @Test
        void shouldTalkToPlugin_toLookupForSecrets() {
            String responseBody = "[{\"key\":\"key1\",\"value\":\"secret1\"},{\"key\":\"key2\",\"value\":\"secret2\"}]";
            when(pluginManager.submitTo(eq(PLUGIN_ID), eq(SECRETS_EXTENSION), requestArgumentCaptor.capture())).thenReturn(DefaultGoPluginApiResponse.success(responseBody));

            final SecretConfig secretConfig = new SecretConfig();
            secretConfig.getConfiguration().add(ConfigurationPropertyMother.create("AWS_ACCESS_KEY", false, "some-access-key"));
            secretConfig.getConfiguration().add(ConfigurationPropertyMother.create("AWS_SECRET_KEY", true, "some-secret-value"));

            List<Secret> secrets = secretsExtensionV1.lookupSecrets(PLUGIN_ID, secretConfig, new HashSet<>(asList("key1", "key2")));

            assertThat(secrets.size()).isEqualTo(2);
            assertThat(secrets).contains(new Secret("key1", "secret1"), new Secret("key2", "secret2"));

            assertExtensionRequest(REQUEST_LOOKUP_SECRETS, "{\"configuration\":{\"AWS_ACCESS_KEY\":\"some-access-key\",\"AWS_SECRET_KEY\":\"some-secret-value\"},\"keys\":[ \"key1\", \"key2\"]}");
        }

        @Test
        void shouldErrorOutIfPluginReturnsAnErrorResponse() {
            String responseBody = "{\"message\":\"Error looking up for keys 'key1'\"}";
            when(pluginManager.submitTo(eq(PLUGIN_ID), eq(SECRETS_EXTENSION), requestArgumentCaptor.capture())).thenReturn(DefaultGoPluginApiResponse.error(responseBody));

            final SecretConfig secretConfig = new SecretConfig();
            secretConfig.getConfiguration().add(ConfigurationPropertyMother.create("AWS_ACCESS_KEY", false, "some-access-key"));

            assertThatCode(() -> secretsExtensionV1.lookupSecrets(PLUGIN_ID, secretConfig, new HashSet<>(asList("key1", "key2"))))
                    .isInstanceOf(SecretResolutionFailureException.class)
                    .hasMessage("Error looking up secrets, plugin returned error code '500' with response: 'Error looking up for keys 'key1''");
        }
    }

    private void assertExtensionRequest(String requestName, String requestBody) {
        final GoPluginApiRequest request = requestArgumentCaptor.getValue();
        assertThat(request.requestName()).isEqualTo(requestName);
        assertThat(request.extensionVersion()).isEqualTo("1.0");
        assertThat(request.extension()).isEqualTo(SECRETS_EXTENSION);
        assertThatJson(requestBody).isEqualTo(request.requestBody());
    }
}
