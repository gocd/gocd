/*
 * Copyright 2018 ThoughtWorks, Inc.
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
import com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother;
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
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.thoughtworks.go.plugin.access.authorization.AuthorizationPluginConstants.*;
import static com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse.SUCCESS_RESPONSE_CODE;
import static com.thoughtworks.go.plugin.domain.common.PluginConstants.AUTHORIZATION_EXTENSION;
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class AuthorizationExtensionTest {
    public static final String PLUGIN_ID = "plugin-id";

    @Mock
    private PluginManager pluginManager;
    private ArgumentCaptor<GoPluginApiRequest> requestArgumentCaptor;
    private AuthorizationExtension authorizationExtension;

    @BeforeEach
    public void setUp() throws Exception {
        initMocks(this);
        when(pluginManager.resolveExtensionVersion(PLUGIN_ID, AUTHORIZATION_EXTENSION, Arrays.asList("1.0", "2.0"))).thenReturn("1.0");
        when(pluginManager.isPluginOfType(AUTHORIZATION_EXTENSION, PLUGIN_ID)).thenReturn(true);

        authorizationExtension = new AuthorizationExtension(pluginManager);

        requestArgumentCaptor = ArgumentCaptor.forClass(GoPluginApiRequest.class);
    }

    @Test
    public void shouldExtendAbstractExtension() throws Exception {
        assertTrue(authorizationExtension instanceof AbstractExtension);
    }

    @Test
    public void shouldTalkToPlugin_To_GetCapabilities() throws Exception {
        String responseBody = "{\"supported_auth_type\":\"password\",\"can_search\":true}";
        when(pluginManager.submitTo(eq(PLUGIN_ID), eq(AUTHORIZATION_EXTENSION), requestArgumentCaptor.capture())).thenReturn(new DefaultGoPluginApiResponse(SUCCESS_RESPONSE_CODE, responseBody));

        com.thoughtworks.go.plugin.domain.authorization.Capabilities capabilities = authorizationExtension.getCapabilities(PLUGIN_ID);

        assertRequest(requestArgumentCaptor.getValue(), AUTHORIZATION_EXTENSION, "1.0", REQUEST_GET_CAPABILITIES, null);
        assertThat(capabilities.getSupportedAuthType().toString(), is(SupportedAuthType.Password.toString()));
        assertThat(capabilities.canSearch(), is(true));
    }

    @Test
    public void shouldTalkToPlugin_To_GetPluginConfigurationMetadata() throws Exception {
        String responseBody = "[{\"key\":\"username\",\"metadata\":{\"required\":true,\"secure\":false}},{\"key\":\"password\",\"metadata\":{\"required\":true,\"secure\":true}}]";
        when(pluginManager.submitTo(eq(PLUGIN_ID), eq(AUTHORIZATION_EXTENSION), requestArgumentCaptor.capture())).thenReturn(new DefaultGoPluginApiResponse(SUCCESS_RESPONSE_CODE, responseBody));

        List<PluginConfiguration> authConfigMetadata = authorizationExtension.getAuthConfigMetadata(PLUGIN_ID);

        assertRequest(requestArgumentCaptor.getValue(), AUTHORIZATION_EXTENSION, "1.0", REQUEST_GET_AUTH_CONFIG_METADATA, null);

        assertThat(authConfigMetadata.size(), is(2));
        assertThat(authConfigMetadata, containsInAnyOrder(
                new PluginConfiguration("username", new Metadata(true, false)),
                new PluginConfiguration("password", new Metadata(true, true))
        ));
    }

    @Test
    public void shouldTalkToPlugin_To_GetAuthConfigView() throws Exception {
        String responseBody = "{ \"template\": \"<div>This is view snippet</div>\" }";
        when(pluginManager.submitTo(eq(PLUGIN_ID), eq(AUTHORIZATION_EXTENSION), requestArgumentCaptor.capture())).thenReturn(new DefaultGoPluginApiResponse(SUCCESS_RESPONSE_CODE, responseBody));

        String pluginConfigurationView = authorizationExtension.getAuthConfigView(PLUGIN_ID);

        assertRequest(requestArgumentCaptor.getValue(), AUTHORIZATION_EXTENSION, "1.0", REQUEST_GET_AUTH_CONFIG_VIEW, null);

        assertThat(pluginConfigurationView, is("<div>This is view snippet</div>"));
    }

    @Test
    public void shouldTalkToPlugin_To_ValidateAuthConfig() throws Exception {
        String responseBody = "[{\"message\":\"Url must not be blank.\",\"key\":\"Url\"},{\"message\":\"SearchBase must not be blank.\",\"key\":\"SearchBase\"}]";
        when(pluginManager.submitTo(eq(PLUGIN_ID), eq(AUTHORIZATION_EXTENSION), requestArgumentCaptor.capture())).thenReturn(new DefaultGoPluginApiResponse(SUCCESS_RESPONSE_CODE, responseBody));

        ValidationResult validationResult = authorizationExtension.validateAuthConfig(PLUGIN_ID, Collections.emptyMap());

        assertRequest(requestArgumentCaptor.getValue(), AUTHORIZATION_EXTENSION, "1.0", REQUEST_VALIDATE_AUTH_CONFIG, "{}");

        assertThat(validationResult.isSuccessful(), is(false));
        assertThat(validationResult.getErrors(), containsInAnyOrder(
                new ValidationError("Url", "Url must not be blank."),
                new ValidationError("SearchBase", "SearchBase must not be blank.")
        ));
    }

    @Test
    public void shouldTalkToPlugin_To_VerifyConnection() throws Exception {
        String responseBody = "{\"status\":\"success\"}";
        when(pluginManager.submitTo(eq(PLUGIN_ID), eq(AUTHORIZATION_EXTENSION), requestArgumentCaptor.capture())).thenReturn(new DefaultGoPluginApiResponse(SUCCESS_RESPONSE_CODE, responseBody));
        AuthorizationExtension authorizationExtensionSpy = spy(authorizationExtension);

        authorizationExtensionSpy.verifyConnection(PLUGIN_ID, Collections.emptyMap());

        assertRequest(requestArgumentCaptor.getValue(), AUTHORIZATION_EXTENSION, "1.0", REQUEST_VERIFY_CONNECTION, "{}");

        verify(authorizationExtensionSpy).verifyConnection(PLUGIN_ID, Collections.emptyMap());
    }

    @Test
    public void shouldTalkToPlugin_To_GetRoleConfigurationMetadata() throws Exception {
        String responseBody = "[{\"key\":\"memberOf\",\"metadata\":{\"required\":true,\"secure\":false}}]";
        when(pluginManager.submitTo(eq(PLUGIN_ID), eq(AUTHORIZATION_EXTENSION), requestArgumentCaptor.capture())).thenReturn(new DefaultGoPluginApiResponse(SUCCESS_RESPONSE_CODE, responseBody));

        List<PluginConfiguration> roleConfigurationMetadata = authorizationExtension.getRoleConfigurationMetadata(PLUGIN_ID);

        assertRequest(requestArgumentCaptor.getValue(), AUTHORIZATION_EXTENSION, "1.0", REQUEST_GET_ROLE_CONFIG_METADATA, null);

        assertThat(roleConfigurationMetadata.size(), is(1));
        assertThat(roleConfigurationMetadata, containsInAnyOrder(
                new PluginConfiguration("memberOf", new Metadata(true, false))
        ));
    }

    @Test
    public void shouldTalkToPlugin_To_GetRoleConfigurationView() throws Exception {
        String responseBody = "{ \"template\": \"<div>This is view snippet</div>\" }";
        when(pluginManager.submitTo(eq(PLUGIN_ID), eq(AUTHORIZATION_EXTENSION), requestArgumentCaptor.capture())).thenReturn(new DefaultGoPluginApiResponse(SUCCESS_RESPONSE_CODE, responseBody));

        String pluginConfigurationView = authorizationExtension.getRoleConfigurationView(PLUGIN_ID);

        assertRequest(requestArgumentCaptor.getValue(), AUTHORIZATION_EXTENSION, "1.0", REQUEST_GET_ROLE_CONFIG_VIEW, null);

        assertThat(pluginConfigurationView, is("<div>This is view snippet</div>"));
    }

    @Test
    public void shouldTalkToPlugin_To_ValidateRoleConfiguration() throws Exception {
        String responseBody = "[{\"message\":\"memberOf must not be blank.\",\"key\":\"memberOf\"}]";
        when(pluginManager.submitTo(eq(PLUGIN_ID), eq(AUTHORIZATION_EXTENSION), requestArgumentCaptor.capture())).thenReturn(new DefaultGoPluginApiResponse(SUCCESS_RESPONSE_CODE, responseBody));

        ValidationResult validationResult = authorizationExtension.validateRoleConfiguration(PLUGIN_ID, Collections.emptyMap());

        assertRequest(requestArgumentCaptor.getValue(), AUTHORIZATION_EXTENSION, "1.0", REQUEST_VALIDATE_ROLE_CONFIG, "{}");

        assertThat(validationResult.isSuccessful(), is(false));
        assertThat(validationResult.getErrors(), containsInAnyOrder(
                new ValidationError("memberOf", "memberOf must not be blank.")
        ));
    }

    @Test
    public void shouldTalkToPlugin_To_AuthenticateUser() throws Exception {
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

        final PluginRoleConfig roleConfig = new PluginRoleConfig("foo", "ldap", ConfigurationPropertyMother.create("memberOf", false, "ou=some-value"));
        final List<PluginRoleConfig> pluginRoleConfigs = Collections.singletonList(roleConfig);

        final SecurityAuthConfigs authConfigs = new SecurityAuthConfigs();
        authConfigs.add(new SecurityAuthConfig("ldap", "cd.go.ldap", ConfigurationPropertyMother.create("url", false, "some-url")));

        AuthenticationResponse authenticationResponse = authorizationExtension.authenticateUser(PLUGIN_ID, "bob", "secret", authConfigs, pluginRoleConfigs);

        assertRequest(requestArgumentCaptor.getValue(), AUTHORIZATION_EXTENSION, "1.0", REQUEST_AUTHENTICATE_USER, requestBody);
        assertThat(authenticationResponse.getUser(), is(new User("bob", "Bob", "bob@example.com")));
        assertThat(authenticationResponse.getRoles().get(0), is("blackbird"));
    }

    @Test
    public void shouldTalkToPlugin_To_AuthenticateUserWithEmptyListIfRoleConfigsAreNotProvided() throws Exception {
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
        authConfigs.add(new SecurityAuthConfig("ldap", "cd.go.ldap", ConfigurationPropertyMother.create("url", false, "some-url")));

        AuthenticationResponse authenticationResponse = authorizationExtension.authenticateUser(PLUGIN_ID, "bob", "secret", authConfigs, null);

        assertRequest(requestArgumentCaptor.getValue(), AUTHORIZATION_EXTENSION, "1.0", REQUEST_AUTHENTICATE_USER, requestBody);
        assertThat(authenticationResponse.getUser(), is(new User("bob", "Bob", "bob@example.com")));
        assertThat(authenticationResponse.getRoles().get(0), is("blackbird"));
    }

    @Test
    void authenticateUser_shouldErrorOutInAbsenceOfSecurityAuthConfigs() {
        Executable codeThatShouldThrowError = () -> {
            authorizationExtension.authenticateUser(PLUGIN_ID, "bob", "secret", null, null);
        };

        MissingAuthConfigsException exception = assertThrows(MissingAuthConfigsException.class, codeThatShouldThrowError);

        assertThat(exception.getMessage(), equalTo("No AuthConfigs configured for plugin: plugin-id, Plugin would need at-least one auth_config to authenticate user."));
        verifyNoMoreInteractions(pluginManager);
    }

    @Test
    public void shouldTalkToPlugin_To_SearchUsers() throws Exception {
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

        List<User> users = authorizationExtension.searchUsers(PLUGIN_ID, "bob", Collections.singletonList(new SecurityAuthConfig("ldap", "cd.go.ldap", ConfigurationPropertyMother.create("foo", false, "bar"))));

        assertRequest(requestArgumentCaptor.getValue(), AUTHORIZATION_EXTENSION, "1.0", REQUEST_SEARCH_USERS, requestBody);
        assertThat(users, hasSize(1));
        assertThat(users, hasItem(new User("bob", "Bob", "bob@example.com")));
    }

    @Test
    public void shouldTalkToPlugin_To_GetAuthorizationServerUrl() {
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
        SecurityAuthConfig authConfig = new SecurityAuthConfig("github", "cd.go.github", ConfigurationPropertyMother.create("url", false, "some-url"));

        when(pluginManager.submitTo(eq(PLUGIN_ID), eq(AUTHORIZATION_EXTENSION), requestArgumentCaptor.capture())).thenReturn(new DefaultGoPluginApiResponse(SUCCESS_RESPONSE_CODE, responseBody));

        String authorizationServerRedirectUrl = authorizationExtension.getAuthorizationServerUrl(PLUGIN_ID, Collections.singletonList(authConfig), "http://go.site.url");

        assertRequest(requestArgumentCaptor.getValue(), AUTHORIZATION_EXTENSION, "1.0", REQUEST_AUTHORIZATION_SERVER_URL, requestBody);
        assertThat(authorizationServerRedirectUrl, is("url_to_authorization_server"));
    }


    @Nested
    class AuthorizationExtension_v2 {
        @BeforeEach
        public void setup() {
            when(pluginManager.resolveExtensionVersion(PLUGIN_ID, AUTHORIZATION_EXTENSION, Arrays.asList("1.0", "2.0"))).thenReturn("2.0");
        }

        @Test
        public void shouldTalkToPlugin_To_GetUserRoles() {
            String requestBody = "{\"auth_configs\":[{\"configuration\":{\"foo\":\"bar\"},\"id\":\"ldap\"}],\"role_configs\":[],\"username\":\"fooUser\"}";
            String responseBody = "{\n" +
                    "  \"user\": {\n" +
                    "      \"username\":\"bob\",\n" +
                    "      \"display_name\": \"Bob\",\n" +
                    "      \"email\": \"bob@example.com\"\n" +
                    "  },\n" +
                    "  \"roles\": [\"super-admin\", \"view-only\", \"operator\"] \n" +
                    "}";

            when(pluginManager.submitTo(eq(PLUGIN_ID), eq(AUTHORIZATION_EXTENSION), requestArgumentCaptor.capture())).thenReturn(new DefaultGoPluginApiResponse(SUCCESS_RESPONSE_CODE, responseBody));


            AuthenticationResponse authenticationResponse = authorizationExtension.getUserRoles(PLUGIN_ID, "fooUser", Collections.singletonList(new SecurityAuthConfig("ldap", "cd.go.ldap", ConfigurationPropertyMother.create("foo", false, "bar"))), Collections.emptyList());

            assertRequest(requestArgumentCaptor.getValue(), AUTHORIZATION_EXTENSION, "2.0", REQUEST_GET_USER_ROLES, requestBody);
            assertThat(authenticationResponse.getUser(), is(new User("bob", "Bob", "bob@example.com")));
            assertThat(authenticationResponse.getRoles(), hasSize(3));
            assertThat(authenticationResponse.getRoles(), equalTo(Arrays.asList("super-admin", "view-only", "operator")));
        }

        @Test
        void authenticateUser_shouldErrorOutInAbsenceOfSecurityAuthConfigs() {
            Executable codeThatShouldThrowError = () -> {
                authorizationExtension.getUserRoles(PLUGIN_ID, "fooUser", null, null);
            };

            MissingAuthConfigsException exception = assertThrows(MissingAuthConfigsException.class, codeThatShouldThrowError);

            assertThat(exception.getMessage(), equalTo("No AuthConfigs configured for plugin: plugin-id, Plugin would need at-least one auth_config to authenticate user."));
            verifyNoMoreInteractions(pluginManager);
        }

        @Test
        public void shouldTalkToPlugin_To_GetCapabilities() {
            String responseBody = "{\"supported_auth_type\":\"password\",\"can_search\":true,\"can_get_user_roles\":true}";
            when(pluginManager.submitTo(eq(PLUGIN_ID), eq(AUTHORIZATION_EXTENSION), requestArgumentCaptor.capture())).thenReturn(new DefaultGoPluginApiResponse(SUCCESS_RESPONSE_CODE, responseBody));

            com.thoughtworks.go.plugin.domain.authorization.Capabilities capabilities = authorizationExtension.getCapabilities(PLUGIN_ID);

            assertRequest(requestArgumentCaptor.getValue(), AUTHORIZATION_EXTENSION, "2.0", REQUEST_GET_CAPABILITIES, null);
            assertThat(capabilities.getSupportedAuthType().toString(), is(SupportedAuthType.Password.toString()));
            assertThat(capabilities.canSearch(), is(true));
            assertThat(capabilities.canGetUserRoles(), is(true));
        }
    }

    private void assertRequest(GoPluginApiRequest goPluginApiRequest, String extensionName, String version, String requestName, String requestBody) {
        Assert.assertThat(goPluginApiRequest.extension(), is(extensionName));
        Assert.assertThat(goPluginApiRequest.extensionVersion(), is(version));
        Assert.assertThat(goPluginApiRequest.requestName(), is(requestName));
        assertThatJson(requestBody).isEqualTo(goPluginApiRequest.requestBody());
    }
}
