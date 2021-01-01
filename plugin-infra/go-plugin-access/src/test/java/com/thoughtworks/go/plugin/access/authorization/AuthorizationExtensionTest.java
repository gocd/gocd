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
package com.thoughtworks.go.plugin.access.authorization;

import com.thoughtworks.go.config.PluginRoleConfig;
import com.thoughtworks.go.config.SecurityAuthConfig;
import com.thoughtworks.go.config.SecurityAuthConfigs;
import com.thoughtworks.go.plugin.access.ExtensionsRegistry;
import com.thoughtworks.go.plugin.access.authorization.v1.AuthorizationMessageConverterV1;
import com.thoughtworks.go.plugin.access.authorization.v2.AuthorizationMessageConverterV2;
import com.thoughtworks.go.plugin.access.common.AbstractExtension;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
import com.thoughtworks.go.plugin.api.response.validation.ValidationError;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.domain.authorization.AuthenticationResponse;
import com.thoughtworks.go.plugin.domain.authorization.SupportedAuthType;
import com.thoughtworks.go.plugin.domain.authorization.User;
import com.thoughtworks.go.plugin.domain.common.Metadata;
import com.thoughtworks.go.plugin.domain.common.PluginConfiguration;
import com.thoughtworks.go.plugin.infra.PluginManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother.*;
import static com.thoughtworks.go.plugin.access.authorization.AuthorizationPluginConstants.*;
import static com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse.BAD_REQUEST;
import static com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse.SUCCESS_RESPONSE_CODE;
import static com.thoughtworks.go.plugin.domain.common.PluginConstants.AUTHORIZATION_EXTENSION;
import static java.util.Collections.emptyList;
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class AuthorizationExtensionTest {
    public static final String PLUGIN_ID = "plugin-id";

    @Mock
    private PluginManager pluginManager;
    @Mock
    ExtensionsRegistry extensionsRegistry;
    private ArgumentCaptor<GoPluginApiRequest> requestArgumentCaptor;
    private AuthorizationExtension authorizationExtension;

    @BeforeEach
    void setUp() {
        initMocks(this);
        when(pluginManager.resolveExtensionVersion(PLUGIN_ID, AUTHORIZATION_EXTENSION, Arrays.asList("1.0", "2.0"))).thenReturn("1.0");
        when(pluginManager.isPluginOfType(AUTHORIZATION_EXTENSION, PLUGIN_ID)).thenReturn(true);

        authorizationExtension = new AuthorizationExtension(pluginManager, extensionsRegistry);

        requestArgumentCaptor = ArgumentCaptor.forClass(GoPluginApiRequest.class);
    }

    @Test
    void shouldExtendAbstractExtension() {
        assertThat(authorizationExtension).isInstanceOf(AbstractExtension.class);
    }

    @Test
    void shouldTalkToPlugin_To_GetCapabilities() {
        String responseBody = "{\"supported_auth_type\":\"password\",\"can_search\":true}";
        when(pluginManager.submitTo(eq(PLUGIN_ID), eq(AUTHORIZATION_EXTENSION), requestArgumentCaptor.capture())).thenReturn(new DefaultGoPluginApiResponse(SUCCESS_RESPONSE_CODE, responseBody));

        com.thoughtworks.go.plugin.domain.authorization.Capabilities capabilities = authorizationExtension.getCapabilities(PLUGIN_ID);

        assertRequest(requestArgumentCaptor.getValue(), AUTHORIZATION_EXTENSION, "1.0", REQUEST_GET_CAPABILITIES, null);
        assertThat(capabilities.getSupportedAuthType().toString()).isEqualTo(SupportedAuthType.Password.toString());
        assertThat(capabilities.canSearch()).isEqualTo(true);
    }

    @Test
    void shouldReturnFalseForSupportsValidatingUserExistenceForAuthorizationExtensionV1() throws Exception {
        String pluginId = "cd.go.ldap";
        when(pluginManager.resolveExtensionVersion(pluginId, AUTHORIZATION_EXTENSION, SUPPORTED_VERSIONS)).thenReturn(AuthorizationMessageConverterV1.VERSION);
        SecurityAuthConfig authConfig = new SecurityAuthConfig("ldap", pluginId, create("url", false, "some-url"));

        boolean expected = authorizationExtension.supportsPluginAPICallsRequiredForAccessToken(authConfig);
        assertThat(expected).isFalse();
    }

    @Test
    void shouldReturnTrueForSupportsValidatingUserExistenceForAuthorizationExtensionV2() throws Exception {
        String pluginId = "cd.go.ldap";
        when(pluginManager.resolveExtensionVersion(pluginId, AUTHORIZATION_EXTENSION, SUPPORTED_VERSIONS)).thenReturn(AuthorizationMessageConverterV2.VERSION);
        SecurityAuthConfig authConfig = new SecurityAuthConfig("ldap", pluginId, create("url", false, "some-url"));

        boolean expected = authorizationExtension.supportsPluginAPICallsRequiredForAccessToken(authConfig);
        assertThat(expected).isTrue();
    }

    @Test
    void shouldTalkToPlugin_To_GetPluginConfigurationMetadata() {
        String responseBody = "[{\"key\":\"username\",\"metadata\":{\"required\":true,\"secure\":false}},{\"key\":\"password\",\"metadata\":{\"required\":true,\"secure\":true}}]";
        when(pluginManager.submitTo(eq(PLUGIN_ID), eq(AUTHORIZATION_EXTENSION), requestArgumentCaptor.capture())).thenReturn(new DefaultGoPluginApiResponse(SUCCESS_RESPONSE_CODE, responseBody));

        List<PluginConfiguration> authConfigMetadata = authorizationExtension.getAuthConfigMetadata(PLUGIN_ID);

        assertRequest(requestArgumentCaptor.getValue(), AUTHORIZATION_EXTENSION, "1.0", REQUEST_GET_AUTH_CONFIG_METADATA, null);

        assertThat(authConfigMetadata.size()).isEqualTo(2);
        assertThat(authConfigMetadata).hasSize(2)
                .contains(
                        new PluginConfiguration("username", new Metadata(true, false)),
                        new PluginConfiguration("password", new Metadata(true, true))
                );
    }

    @Test
    void shouldTalkToPlugin_To_GetAuthConfigView() {
        String responseBody = "{ \"template\": \"<div>This is view snippet</div>\" }";
        when(pluginManager.submitTo(eq(PLUGIN_ID), eq(AUTHORIZATION_EXTENSION), requestArgumentCaptor.capture())).thenReturn(new DefaultGoPluginApiResponse(SUCCESS_RESPONSE_CODE, responseBody));

        String pluginConfigurationView = authorizationExtension.getAuthConfigView(PLUGIN_ID);

        assertRequest(requestArgumentCaptor.getValue(), AUTHORIZATION_EXTENSION, "1.0", REQUEST_GET_AUTH_CONFIG_VIEW, null);

        assertThat(pluginConfigurationView).isEqualTo("<div>This is view snippet</div>");
    }

    @Test
    void shouldTalkToPlugin_To_ValidateAuthConfig() {
        String responseBody = "[{\"message\":\"Url must not be blank.\",\"key\":\"Url\"},{\"message\":\"SearchBase must not be blank.\",\"key\":\"SearchBase\"}]";
        when(pluginManager.submitTo(eq(PLUGIN_ID), eq(AUTHORIZATION_EXTENSION), requestArgumentCaptor.capture())).thenReturn(new DefaultGoPluginApiResponse(SUCCESS_RESPONSE_CODE, responseBody));

        ValidationResult validationResult = authorizationExtension.validateAuthConfig(PLUGIN_ID, Collections.emptyMap());

        assertRequest(requestArgumentCaptor.getValue(), AUTHORIZATION_EXTENSION, "1.0", REQUEST_VALIDATE_AUTH_CONFIG, "{}");

        assertThat(validationResult.isSuccessful()).isEqualTo(false);
        assertThat(validationResult.getErrors()).hasSize(2)
                .contains(
                        new ValidationError("Url", "Url must not be blank."),
                        new ValidationError("SearchBase", "SearchBase must not be blank.")
                );
    }

    @Test
    void shouldTalkToPlugin_To_VerifyConnection() {
        String responseBody = "{\"status\":\"success\"}";
        when(pluginManager.submitTo(eq(PLUGIN_ID), eq(AUTHORIZATION_EXTENSION), requestArgumentCaptor.capture())).thenReturn(new DefaultGoPluginApiResponse(SUCCESS_RESPONSE_CODE, responseBody));
        AuthorizationExtension authorizationExtensionSpy = spy(authorizationExtension);

        authorizationExtensionSpy.verifyConnection(PLUGIN_ID, Collections.emptyMap());

        assertRequest(requestArgumentCaptor.getValue(), AUTHORIZATION_EXTENSION, "1.0", REQUEST_VERIFY_CONNECTION, "{}");

        verify(authorizationExtensionSpy).verifyConnection(PLUGIN_ID, Collections.emptyMap());
    }

    @Test
    void shouldTalkToPlugin_To_GetRoleConfigurationMetadata() {
        String responseBody = "[{\"key\":\"memberOf\",\"metadata\":{\"required\":true,\"secure\":false}}]";
        when(pluginManager.submitTo(eq(PLUGIN_ID), eq(AUTHORIZATION_EXTENSION), requestArgumentCaptor.capture())).thenReturn(new DefaultGoPluginApiResponse(SUCCESS_RESPONSE_CODE, responseBody));

        List<PluginConfiguration> roleConfigurationMetadata = authorizationExtension.getRoleConfigurationMetadata(PLUGIN_ID);

        assertRequest(requestArgumentCaptor.getValue(), AUTHORIZATION_EXTENSION, "1.0", REQUEST_GET_ROLE_CONFIG_METADATA, null);

        assertThat(roleConfigurationMetadata.size()).isEqualTo(1);
        assertThat(roleConfigurationMetadata).contains(
                new PluginConfiguration("memberOf", new Metadata(true, false))
        );
    }

    @Test
    void shouldTalkToPlugin_To_GetRoleConfigurationView() {
        String responseBody = "{ \"template\": \"<div>This is view snippet</div>\" }";
        when(pluginManager.submitTo(eq(PLUGIN_ID), eq(AUTHORIZATION_EXTENSION), requestArgumentCaptor.capture())).thenReturn(new DefaultGoPluginApiResponse(SUCCESS_RESPONSE_CODE, responseBody));

        String pluginConfigurationView = authorizationExtension.getRoleConfigurationView(PLUGIN_ID);

        assertRequest(requestArgumentCaptor.getValue(), AUTHORIZATION_EXTENSION, "1.0", REQUEST_GET_ROLE_CONFIG_VIEW, null);

        assertThat(pluginConfigurationView).isEqualTo("<div>This is view snippet</div>");
    }

    @Test
    void shouldTalkToPlugin_To_ValidateRoleConfiguration() {
        String responseBody = "[{\"message\":\"memberOf must not be blank.\",\"key\":\"memberOf\"}]";
        when(pluginManager.submitTo(eq(PLUGIN_ID), eq(AUTHORIZATION_EXTENSION), requestArgumentCaptor.capture())).thenReturn(new DefaultGoPluginApiResponse(SUCCESS_RESPONSE_CODE, responseBody));

        ValidationResult validationResult = authorizationExtension.validateRoleConfiguration(PLUGIN_ID, Collections.emptyMap());

        assertRequest(requestArgumentCaptor.getValue(), AUTHORIZATION_EXTENSION, "1.0", REQUEST_VALIDATE_ROLE_CONFIG, "{}");

        assertThat(validationResult.isSuccessful()).isEqualTo(false);
        assertThat(validationResult.getErrors()).contains(
                new ValidationError("memberOf", "memberOf must not be blank.")
        );
    }

    @Test
    void shouldTalkToPlugin_To_AuthenticateUser() {
        String requestBody = "{\n" +
                "  \"credentials\": {\n" +
                "    \"username\": \"bob\",\n" +
                "    \"password\": \"secret\"\n" +
                "  },\n" +
                "  \"auth_configs\": [\n" +
                "    {\n" +
                "      \"id\": \"ldap\",\n" +
                "      \"configuration\": {\n" +
                "        \"url\": \"some-url\"\n" +
                "      }\n" +
                "    }\n" +
                "  ],\n" +
                "  \"role_configs\": [\n" +
                "    {\n" +
                "      \"name\": \"foo\",\n" +
                "      \"auth_config_id\": \"ldap\",\n" +
                "      \"configuration\": {\n" +
                "        \"memberOf\": \"ou=some-value\"\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        String responseBody = "{\"user\":{\"username\":\"bob\",\"display_name\":\"Bob\",\"email\":\"bob@example.com\"},\"roles\":[\"blackbird\"]}";

        when(pluginManager.submitTo(eq(PLUGIN_ID), eq(AUTHORIZATION_EXTENSION), requestArgumentCaptor.capture())).thenReturn(new DefaultGoPluginApiResponse(SUCCESS_RESPONSE_CODE, responseBody));

        final PluginRoleConfig roleConfig = new PluginRoleConfig("foo", "ldap", create("memberOf", false, "ou=some-value"));
        final List<PluginRoleConfig> pluginRoleConfigs = Collections.singletonList(roleConfig);

        final SecurityAuthConfigs authConfigs = new SecurityAuthConfigs();
        authConfigs.add(new SecurityAuthConfig("ldap", "cd.go.ldap", create("url", false, "some-url")));

        AuthenticationResponse authenticationResponse = authorizationExtension.authenticateUser(PLUGIN_ID, "bob", "secret", authConfigs, pluginRoleConfigs);

        assertRequest(requestArgumentCaptor.getValue(), AUTHORIZATION_EXTENSION, "1.0", REQUEST_AUTHENTICATE_USER, requestBody);
        assertThat(authenticationResponse.getUser()).isEqualTo(new User("bob", "Bob", "bob@example.com"));
        assertThat(authenticationResponse.getRoles().get(0)).isEqualTo("blackbird");
    }

    @Test
    void shouldTalkToPlugin_To_AuthenticateUserWithEmptyListIfRoleConfigsAreNotProvided() {
        String requestBody = "{\n" +
                "  \"credentials\": {\n" +
                "    \"username\": \"bob\",\n" +
                "    \"password\": \"secret\"\n" +
                "  },\n" +
                "  \"auth_configs\": [\n" +
                "    {\n" +
                "      \"id\": \"ldap\",\n" +
                "      \"configuration\": {\n" +
                "        \"url\": \"some-url\"\n" +
                "      }\n" +
                "    }\n" +
                "  ],\n" +
                "  \"role_configs\": []\n" +
                "}";

        String responseBody = "{\"user\":{\"username\":\"bob\",\"display_name\":\"Bob\",\"email\":\"bob@example.com\"},\"roles\":[\"blackbird\"]}";

        when(pluginManager.submitTo(eq(PLUGIN_ID), eq(AUTHORIZATION_EXTENSION), requestArgumentCaptor.capture())).thenReturn(new DefaultGoPluginApiResponse(SUCCESS_RESPONSE_CODE, responseBody));

        final SecurityAuthConfigs authConfigs = new SecurityAuthConfigs();
        authConfigs.add(new SecurityAuthConfig("ldap", "cd.go.ldap", create("url", false, "some-url")));

        AuthenticationResponse authenticationResponse = authorizationExtension.authenticateUser(PLUGIN_ID, "bob", "secret", authConfigs, null);

        assertRequest(requestArgumentCaptor.getValue(), AUTHORIZATION_EXTENSION, "1.0", REQUEST_AUTHENTICATE_USER, requestBody);
        assertThat(authenticationResponse.getUser()).isEqualTo(new User("bob", "Bob", "bob@example.com"));
        assertThat(authenticationResponse.getRoles().get(0)).isEqualTo("blackbird");
    }

    @Test
    void authenticateUser_shouldErrorOutInAbsenceOfSecurityAuthConfigs() {
        Executable codeThatShouldThrowError = () -> {
            authorizationExtension.authenticateUser(PLUGIN_ID, "bob", "secret", null, null);
        };

        MissingAuthConfigsException exception = assertThrows(MissingAuthConfigsException.class, codeThatShouldThrowError);

        assertThat(exception.getMessage()).isEqualTo("No AuthConfigs configured for plugin: plugin-id, Plugin would need at-least one auth_config to authenticate user.");
        verifyNoMoreInteractions(pluginManager);
    }

    @Test
    void shouldTalkToPlugin_To_SearchUsers() {
        String requestBody = "{\n" +
                "  \"search_term\": \"bob\",\n" +
                "  \"auth_configs\": [\n" +
                "    {\n" +
                "      \"id\": \"ldap\",\n" +
                "      \"configuration\": {\n" +
                "        \"foo\": \"bar\"\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}";
        String responseBody = "[{\"username\":\"bob\",\"display_name\":\"Bob\",\"email\":\"bob@example.com\"}]";
        when(pluginManager.submitTo(eq(PLUGIN_ID), eq(AUTHORIZATION_EXTENSION), requestArgumentCaptor.capture())).thenReturn(new DefaultGoPluginApiResponse(SUCCESS_RESPONSE_CODE, responseBody));

        List<User> users = authorizationExtension.searchUsers(PLUGIN_ID, "bob", Collections.singletonList(new SecurityAuthConfig("ldap", "cd.go.ldap", create("foo", false, "bar"))));

        assertRequest(requestArgumentCaptor.getValue(), AUTHORIZATION_EXTENSION, "1.0", REQUEST_SEARCH_USERS, requestBody);
        assertThat(users).hasSize(1)
                .contains(new User("bob", "Bob", "bob@example.com"));
    }

    @Test
    void shouldTalkToPlugin_To_GetAuthorizationServerUrl() {
        String requestBody = "{\n" +
                "  \"auth_configs\": [\n" +
                "    {\n" +
                "      \"id\": \"github\",\n" +
                "      \"configuration\": {\n" +
                "        \"url\": \"some-url\"\n" +
                "      }\n" +
                "    }\n" +
                "  ],\n" +
                "  \"authorization_server_callback_url\": \"http://go.site.url/go/plugin/plugin-id/authenticate\"\n" +
                "}";
        String responseBody = "{\"authorization_server_url\":\"url_to_authorization_server\"}";
        SecurityAuthConfig authConfig = new SecurityAuthConfig("github", "cd.go.github", create("url", false, "some-url"));

        when(pluginManager.submitTo(eq(PLUGIN_ID), eq(AUTHORIZATION_EXTENSION), requestArgumentCaptor.capture())).thenReturn(new DefaultGoPluginApiResponse(SUCCESS_RESPONSE_CODE, responseBody));

        String authorizationServerRedirectUrl = authorizationExtension.getAuthorizationServerUrl(PLUGIN_ID, Collections.singletonList(authConfig), "http://go.site.url");

        assertRequest(requestArgumentCaptor.getValue(), AUTHORIZATION_EXTENSION, "1.0", REQUEST_AUTHORIZATION_SERVER_URL, requestBody);
        assertThat(authorizationServerRedirectUrl).isEqualTo("url_to_authorization_server");
    }


    @Nested
    class AuthorizationExtension_v2 {
        @BeforeEach
        void setup() {
            when(pluginManager.resolveExtensionVersion(PLUGIN_ID, AUTHORIZATION_EXTENSION, Arrays.asList("1.0", "2.0"))).thenReturn("2.0");
        }

        @Test
        void shouldTalkToPlugin_To_GetUserRoles() {
            String requestBody = "{\"auth_config\":{\"configuration\":{\"foo\":\"bar\"},\"id\":\"ldap\"},\"role_configs\":[],\"username\":\"fooUser\"}";
            String responseBody = "[\"super-admin\", \"view-only\", \"operator\"]";

            when(pluginManager.submitTo(eq(PLUGIN_ID), eq(AUTHORIZATION_EXTENSION), requestArgumentCaptor.capture())).thenReturn(new DefaultGoPluginApiResponse(SUCCESS_RESPONSE_CODE, responseBody));


            SecurityAuthConfig authConfig = new SecurityAuthConfig("ldap", "cd.go.ldap", create("foo", false, "bar"));
            List<String> roles = authorizationExtension.getUserRoles(PLUGIN_ID, "fooUser", authConfig, emptyList());

            assertRequest(requestArgumentCaptor.getValue(), AUTHORIZATION_EXTENSION, "2.0", REQUEST_GET_USER_ROLES, requestBody);
            assertThat(roles).hasSize(3)
                    .contains("super-admin", "view-only", "operator");
        }

        @Test
        void shouldTalkToPlugin_ToCheck_isValidUser() {
            String requestBody = "{\"auth_config\":{\"configuration\":{\"foo\":\"bar\"},\"id\":\"ldap\"},\"username\":\"fooUser\"}";
            String responseBody = "OK";

            when(pluginManager.submitTo(eq(PLUGIN_ID), eq(AUTHORIZATION_EXTENSION), requestArgumentCaptor.capture())).thenReturn(new DefaultGoPluginApiResponse(SUCCESS_RESPONSE_CODE, responseBody));

            boolean isValidUser = authorizationExtension.isValidUser(PLUGIN_ID, "fooUser", new SecurityAuthConfig("ldap", "cd.go.ldap", create("foo", false, "bar")));

            assertRequest(requestArgumentCaptor.getValue(), AUTHORIZATION_EXTENSION, "2.0", IS_VALID_USER, requestBody);
            assertThat(isValidUser).isTrue();
        }

        @Test
        void shouldTalkToPlugin_ToCheck_isValidUser_orNot() {
            String requestBody = "{\"auth_config\":{\"configuration\":{\"foo\":\"bar\"},\"id\":\"ldap\"},\"username\":\"fooUser\"}";
            String responseBody = "OK";

            when(pluginManager.submitTo(eq(PLUGIN_ID), eq(AUTHORIZATION_EXTENSION), requestArgumentCaptor.capture())).thenReturn(new DefaultGoPluginApiResponse(BAD_REQUEST, responseBody));

            boolean isValidUser = authorizationExtension.isValidUser(PLUGIN_ID, "fooUser", new SecurityAuthConfig("ldap", "cd.go.ldap", create("foo", false, "bar")));

            assertRequest(requestArgumentCaptor.getValue(), AUTHORIZATION_EXTENSION, "2.0", IS_VALID_USER, requestBody);
            assertThat(isValidUser).isFalse();
        }

        @Test
        void authenticateUser_shouldErrorOutInAbsenceOfSecurityAuthConfigs() {
            Executable codeThatShouldThrowError = () -> authorizationExtension.getUserRoles(PLUGIN_ID, "fooUser", null, null);

            MissingAuthConfigsException exception = assertThrows(MissingAuthConfigsException.class, codeThatShouldThrowError);

            assertThat(exception.getMessage()).isEqualTo("Request 'go.cd.authorization.get-user-roles' requires an AuthConfig. Make sure Authconfig is configured for the plugin 'plugin-id'.");
            verifyNoMoreInteractions(pluginManager);
        }

        @Test
        void shouldTalkToPlugin_To_GetCapabilities() {
            String responseBody = "{\"supported_auth_type\":\"password\",\"can_search\":true,\"can_get_user_roles\":true}";
            when(pluginManager.submitTo(eq(PLUGIN_ID), eq(AUTHORIZATION_EXTENSION), requestArgumentCaptor.capture())).thenReturn(new DefaultGoPluginApiResponse(SUCCESS_RESPONSE_CODE, responseBody));

            com.thoughtworks.go.plugin.domain.authorization.Capabilities capabilities = authorizationExtension.getCapabilities(PLUGIN_ID);

            assertRequest(requestArgumentCaptor.getValue(), AUTHORIZATION_EXTENSION, "2.0", REQUEST_GET_CAPABILITIES, null);
            assertThat(capabilities.getSupportedAuthType().toString()).isEqualTo(SupportedAuthType.Password.toString());
            assertThat(capabilities.canSearch()).isEqualTo(true);
            assertThat(capabilities.canGetUserRoles()).isEqualTo(true);
        }
    }

    private void assertRequest(GoPluginApiRequest goPluginApiRequest, String extensionName, String version, String requestName, String requestBody) {
        assertThat(goPluginApiRequest.extension()).isEqualTo(extensionName);
        assertThat(goPluginApiRequest.extensionVersion()).isEqualTo(version);
        assertThat(goPluginApiRequest.requestName()).isEqualTo(requestName);
        assertThatJson(requestBody).isEqualTo(goPluginApiRequest.requestBody());
    }
}
