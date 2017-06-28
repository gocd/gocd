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

package com.thoughtworks.go.plugin.access.authorization;

import com.thoughtworks.go.config.PluginRoleConfig;
import com.thoughtworks.go.config.SecurityAuthConfig;
import com.thoughtworks.go.config.SecurityAuthConfigs;
import com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother;
import com.thoughtworks.go.plugin.access.authentication.models.User;
import com.thoughtworks.go.plugin.access.authorization.models.AuthenticationResponse;
import com.thoughtworks.go.plugin.access.authorization.models.SupportedAuthType;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
import com.thoughtworks.go.plugin.api.response.validation.ValidationError;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.domain.common.Metadata;
import com.thoughtworks.go.plugin.domain.common.PluginConfiguration;
import com.thoughtworks.go.plugin.infra.PluginManager;
import org.hamcrest.core.Is;
import org.json.JSONException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.thoughtworks.go.plugin.access.authorization.AuthorizationPluginConstants.*;
import static com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse.SUCCESS_RESPONSE_CODE;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class AuthorizationExtensionTest {
    public static final String PLUGIN_ID = "plugin-id";

    @Mock
    private PluginManager pluginManager;
    private ArgumentCaptor<GoPluginApiRequest> requestArgumentCaptor;
    private AuthorizationExtension authorizationExtension;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        when(pluginManager.resolveExtensionVersion(PLUGIN_ID, Arrays.asList("1.0"))).thenReturn("1.0");
        when(pluginManager.isPluginOfType(AuthorizationPluginConstants.EXTENSION_NAME, PLUGIN_ID)).thenReturn(true);

        authorizationExtension = new AuthorizationExtension(pluginManager);

        requestArgumentCaptor = ArgumentCaptor.forClass(GoPluginApiRequest.class);
    }

    @Test
    public void shouldTalkToPlugin_To_GetCapabilities() throws Exception {
        String responseBody = "{\"supported_auth_type\":\"password\",\"can_search\":true}";
        when(pluginManager.submitTo(eq(PLUGIN_ID), requestArgumentCaptor.capture())).thenReturn(new DefaultGoPluginApiResponse(SUCCESS_RESPONSE_CODE, responseBody));

        com.thoughtworks.go.plugin.domain.authorization.Capabilities capabilities = authorizationExtension.getCapabilities(PLUGIN_ID);

        assertRequest(requestArgumentCaptor.getValue(), AuthorizationPluginConstants.EXTENSION_NAME, "1.0", REQUEST_GET_CAPABILITIES, null);
        assertThat(capabilities.getSupportedAuthType().toString(), is(SupportedAuthType.Password.toString()));
        assertThat(capabilities.canSearch(), is(true));
    }

    @Test
    public void shouldTalkToPlugin_To_GetPluginConfigurationMetadata() throws Exception {
        String responseBody = "[{\"key\":\"username\",\"metadata\":{\"required\":true,\"secure\":false}},{\"key\":\"password\",\"metadata\":{\"required\":true,\"secure\":true}}]";
        when(pluginManager.submitTo(eq(PLUGIN_ID), requestArgumentCaptor.capture())).thenReturn(new DefaultGoPluginApiResponse(SUCCESS_RESPONSE_CODE, responseBody));

        List<PluginConfiguration> authConfigMetadata = authorizationExtension.getAuthConfigMetadata(PLUGIN_ID);

        assertRequest(requestArgumentCaptor.getValue(), AuthorizationPluginConstants.EXTENSION_NAME, "1.0", REQUEST_GET_AUTH_CONFIG_METADATA, null);

        assertThat(authConfigMetadata.size(), is(2));
        assertThat(authConfigMetadata, containsInAnyOrder(
                new PluginConfiguration("username", new Metadata(true, false)),
                new PluginConfiguration("password", new Metadata(true, true))
        ));
    }

    @Test
    public void shouldTalkToPlugin_To_GetAuthConfigView() throws Exception {
        String responseBody = "{ \"template\": \"<div>This is view snippet</div>\" }";
        when(pluginManager.submitTo(eq(PLUGIN_ID), requestArgumentCaptor.capture())).thenReturn(new DefaultGoPluginApiResponse(SUCCESS_RESPONSE_CODE, responseBody));

        String pluginConfigurationView = authorizationExtension.getAuthConfigView(PLUGIN_ID);

        assertRequest(requestArgumentCaptor.getValue(), AuthorizationPluginConstants.EXTENSION_NAME, "1.0", REQUEST_GET_AUTH_CONFIG_VIEW, null);

        assertThat(pluginConfigurationView, is("<div>This is view snippet</div>"));
    }

    @Test
    public void shouldTalkToPlugin_To_ValidateAuthConfig() throws Exception {
        String responseBody = "[{\"message\":\"Url must not be blank.\",\"key\":\"Url\"},{\"message\":\"SearchBase must not be blank.\",\"key\":\"SearchBase\"}]";
        when(pluginManager.submitTo(eq(PLUGIN_ID), requestArgumentCaptor.capture())).thenReturn(new DefaultGoPluginApiResponse(SUCCESS_RESPONSE_CODE, responseBody));

        ValidationResult validationResult = authorizationExtension.validateAuthConfig(PLUGIN_ID, Collections.emptyMap());

        assertRequest(requestArgumentCaptor.getValue(), AuthorizationPluginConstants.EXTENSION_NAME, "1.0", REQUEST_VALIDATE_AUTH_CONFIG, "{}");

        assertThat(validationResult.isSuccessful(), is(false));
        assertThat(validationResult.getErrors(), containsInAnyOrder(
                new ValidationError("Url", "Url must not be blank."),
                new ValidationError("SearchBase", "SearchBase must not be blank.")
        ));
    }

    @Test
    public void shouldTalkToPlugin_To_VerifyConnection() throws Exception {
        String responseBody = "{\"status\":\"success\"}";
        when(pluginManager.submitTo(eq(PLUGIN_ID), requestArgumentCaptor.capture())).thenReturn(new DefaultGoPluginApiResponse(SUCCESS_RESPONSE_CODE, responseBody));
        AuthorizationExtension authorizationExtensionSpy = spy(authorizationExtension);

        authorizationExtensionSpy.verifyConnection(PLUGIN_ID, Collections.emptyMap());

        assertRequest(requestArgumentCaptor.getValue(), AuthorizationPluginConstants.EXTENSION_NAME, "1.0", REQUEST_VERIFY_CONNECTION, "{}");

        verify(authorizationExtensionSpy).verifyConnection(PLUGIN_ID, Collections.emptyMap());
    }

    @Test
    public void shouldTalkToPlugin_To_GetRoleConfigurationMetadata() throws Exception {
        String responseBody = "[{\"key\":\"memberOf\",\"metadata\":{\"required\":true,\"secure\":false}}]";
        when(pluginManager.submitTo(eq(PLUGIN_ID), requestArgumentCaptor.capture())).thenReturn(new DefaultGoPluginApiResponse(SUCCESS_RESPONSE_CODE, responseBody));

        List<PluginConfiguration> roleConfigurationMetadata = authorizationExtension.getRoleConfigurationMetadata(PLUGIN_ID);

        assertRequest(requestArgumentCaptor.getValue(), AuthorizationPluginConstants.EXTENSION_NAME, "1.0", REQUEST_GET_ROLE_CONFIG_METADATA, null);

        assertThat(roleConfigurationMetadata.size(), is(1));
        assertThat(roleConfigurationMetadata, containsInAnyOrder(
                new PluginConfiguration("memberOf", new Metadata(true, false))
        ));
    }

    @Test
    public void shouldTalkToPlugin_To_GetRoleConfigurationView() throws Exception {
        String responseBody = "{ \"template\": \"<div>This is view snippet</div>\" }";
        when(pluginManager.submitTo(eq(PLUGIN_ID), requestArgumentCaptor.capture())).thenReturn(new DefaultGoPluginApiResponse(SUCCESS_RESPONSE_CODE, responseBody));

        String pluginConfigurationView = authorizationExtension.getRoleConfigurationView(PLUGIN_ID);

        assertRequest(requestArgumentCaptor.getValue(), AuthorizationPluginConstants.EXTENSION_NAME, "1.0", REQUEST_GET_ROLE_CONFIG_VIEW, null);

        assertThat(pluginConfigurationView, is("<div>This is view snippet</div>"));
    }

    @Test
    public void shouldTalkToPlugin_To_ValidateRoleConfiguration() throws Exception {
        String responseBody = "[{\"message\":\"memberOf must not be blank.\",\"key\":\"memberOf\"}]";
        when(pluginManager.submitTo(eq(PLUGIN_ID), requestArgumentCaptor.capture())).thenReturn(new DefaultGoPluginApiResponse(SUCCESS_RESPONSE_CODE, responseBody));

        ValidationResult validationResult = authorizationExtension.validateRoleConfiguration(PLUGIN_ID, Collections.emptyMap());

        assertRequest(requestArgumentCaptor.getValue(), AuthorizationPluginConstants.EXTENSION_NAME, "1.0", REQUEST_VALIDATE_ROLE_CONFIG, "{}");

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

        when(pluginManager.submitTo(eq(PLUGIN_ID), requestArgumentCaptor.capture())).thenReturn(new DefaultGoPluginApiResponse(SUCCESS_RESPONSE_CODE, responseBody));

        final PluginRoleConfig roleConfig = new PluginRoleConfig("foo", "ldap", ConfigurationPropertyMother.create("memberOf", false, "ou=some-value"));
        final List<PluginRoleConfig> pluginRoleConfigs = Collections.singletonList(roleConfig);

        final SecurityAuthConfigs authConfigs = new SecurityAuthConfigs();
        authConfigs.add(new SecurityAuthConfig("ldap", "cd.go.ldap", ConfigurationPropertyMother.create("url", false, "some-url")));

        AuthenticationResponse authenticationResponse = authorizationExtension.authenticateUser(PLUGIN_ID, "bob", "secret", authConfigs, pluginRoleConfigs);

        assertRequest(requestArgumentCaptor.getValue(), AuthorizationPluginConstants.EXTENSION_NAME, "1.0", REQUEST_AUTHENTICATE_USER, requestBody);
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

        when(pluginManager.submitTo(eq(PLUGIN_ID), requestArgumentCaptor.capture())).thenReturn(new DefaultGoPluginApiResponse(SUCCESS_RESPONSE_CODE, responseBody));

        final SecurityAuthConfigs authConfigs = new SecurityAuthConfigs();
        authConfigs.add(new SecurityAuthConfig("ldap", "cd.go.ldap", ConfigurationPropertyMother.create("url", false, "some-url")));

        AuthenticationResponse authenticationResponse = authorizationExtension.authenticateUser(PLUGIN_ID, "bob", "secret", authConfigs, null);

        assertRequest(requestArgumentCaptor.getValue(), AuthorizationPluginConstants.EXTENSION_NAME, "1.0", REQUEST_AUTHENTICATE_USER, requestBody);
        assertThat(authenticationResponse.getUser(), is(new User("bob", "Bob", "bob@example.com")));
        assertThat(authenticationResponse.getRoles().get(0), is("blackbird"));
    }

    @Test
    public void authenticateUser_shouldErrorOutInAbsenceOfSecurityAuthConfigs() throws Exception {
        thrown.expect(MissingAuthConfigsException.class);
        thrown.expectMessage("No AuthConfigs configured for plugin: plugin-id, Plugin would need at-least one auth_config to authenticate user.");

        authorizationExtension.authenticateUser(PLUGIN_ID, "bob", "secret", null, null);

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
        when(pluginManager.submitTo(eq(PLUGIN_ID), requestArgumentCaptor.capture())).thenReturn(new DefaultGoPluginApiResponse(SUCCESS_RESPONSE_CODE, responseBody));

        List<User> users = authorizationExtension.searchUsers(PLUGIN_ID, "bob", Collections.singletonList(new SecurityAuthConfig("ldap", "cd.go.ldap", ConfigurationPropertyMother.create("foo", false, "bar"))));

        assertRequest(requestArgumentCaptor.getValue(), AuthorizationPluginConstants.EXTENSION_NAME, "1.0", REQUEST_SEARCH_USERS, requestBody);
        assertThat(users, hasSize(1));
        assertThat(users, hasItem(new User("bob", "Bob", "bob@example.com")));
    }

    @Test
    public void shouldTalkToPlugin_To_GetAuthorizationServerUrl() throws JSONException {
        String requestBody = "{\n" +
                "  \"auth_configs\": [\n" +
                "    {\n" +
                "      \"id\": \"github\",\n" +
                "      \"configuration\": {\n" +
                "        \"url\": \"some-url\"\n" +
                "      }\n" +
                "    }\n" +
                "  ],\n" +
                "  \"authorization_server_callback_url\": \"http://go.site.url/go/plugin/plugin-id/authenticate\"\n"+
                "}";
        String responseBody = "{\"authorization_server_url\":\"url_to_authorization_server\"}";
        SecurityAuthConfig authConfig = new SecurityAuthConfig("github", "cd.go.github", ConfigurationPropertyMother.create("url", false, "some-url"));

        when(pluginManager.submitTo(eq(PLUGIN_ID), requestArgumentCaptor.capture())).thenReturn(new DefaultGoPluginApiResponse(SUCCESS_RESPONSE_CODE, responseBody));

        String authorizationServerRedirectUrl = authorizationExtension.getAuthorizationServerUrl(PLUGIN_ID, Collections.singletonList(authConfig), "http://go.site.url");

        assertRequest(requestArgumentCaptor.getValue(), AuthorizationPluginConstants.EXTENSION_NAME, "1.0", REQUEST_AUTHORIZATION_SERVER_URL, requestBody);
        assertThat(authorizationServerRedirectUrl, is("url_to_authorization_server"));
    }

    private void assertRequest(GoPluginApiRequest goPluginApiRequest, String extensionName, String version, String requestName, String requestBody) throws JSONException {
        Assert.assertThat(goPluginApiRequest.extension(), Is.is(extensionName));
        Assert.assertThat(goPluginApiRequest.extensionVersion(), Is.is(version));
        Assert.assertThat(goPluginApiRequest.requestName(), Is.is(requestName));
        JSONAssert.assertEquals(requestBody, goPluginApiRequest.requestBody(), true);
    }
}