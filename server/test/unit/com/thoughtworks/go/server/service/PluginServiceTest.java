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

import com.google.gson.GsonBuilder;
import com.thoughtworks.go.domain.NullPlugin;
import com.thoughtworks.go.domain.Plugin;
import com.thoughtworks.go.plugin.access.authentication.AuthenticationExtension;
import com.thoughtworks.go.plugin.access.common.settings.GoPluginExtension;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsConfiguration;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsMetadataStore;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsProperty;
import com.thoughtworks.go.plugin.access.configrepo.ConfigRepoExtension;
import com.thoughtworks.go.plugin.access.notification.NotificationExtension;
import com.thoughtworks.go.plugin.access.packagematerial.PackageRepositoryExtension;
import com.thoughtworks.go.plugin.access.pluggabletask.TaskExtension;
import com.thoughtworks.go.plugin.access.scm.SCMExtension;
import com.thoughtworks.go.plugin.api.response.validation.ValidationError;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.dao.PluginSqlMapDao;
import com.thoughtworks.go.server.domain.PluginSettings;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.plugins.builder.PluginInfoBuilder;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.ui.plugins.PluginInfo;
import org.hamcrest.core.Is;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.*;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class PluginServiceTest {
    @Mock
    private PackageRepositoryExtension packageRepositoryExtension;
    @Mock
    private SCMExtension scmExtension;
    @Mock
    private TaskExtension taskExtension;
    @Mock
    private NotificationExtension notificationExtension;
    @Mock
    private AuthenticationExtension authenticationExtension;
    @Mock
    private ConfigRepoExtension configRepoExtension;
    @Mock
    private PluginSqlMapDao pluginDao;
    @Mock
    private PluginInfoBuilder builder;
    @Mock
    private SecurityService securityService;
    @Mock
    private EntityHashingService entityHashingService;

    private PluginService pluginService;
    private List<GoPluginExtension> extensions;

    @Before
    public void setUp() {
        initMocks(this);

        PluginSettingsMetadataStore.getInstance().clear();

        Map<String, String> configuration = new HashMap<>();
        configuration.put("p1-k1", "v1");
        configuration.put("p1-k2", "");
        configuration.put("p1-k3", null);
        Plugin plugin = new Plugin("plugin-id-1", toJSON(configuration));
        plugin.setId(1L);
        when(pluginDao.findPlugin("plugin-id-1")).thenReturn(plugin);

        when(pluginDao.findPlugin("plugin-id-2")).thenReturn(new NullPlugin());

        PluginSettingsConfiguration configuration1 = new PluginSettingsConfiguration();
        configuration1.add(new PluginSettingsProperty("p1-k1"));
        configuration1.add(new PluginSettingsProperty("p1-k2"));
        configuration1.add(new PluginSettingsProperty("p1-k3"));
        PluginSettingsMetadataStore.getInstance().addMetadataFor("plugin-id-1", configuration1, "template-1");

        PluginSettingsConfiguration configuration2 = new PluginSettingsConfiguration();
        configuration2.add(new PluginSettingsProperty("p2-k1"));
        configuration2.add(new PluginSettingsProperty("p2-k2"));
        configuration2.add(new PluginSettingsProperty("p2-k3"));
        PluginSettingsMetadataStore.getInstance().addMetadataFor("plugin-id-2", configuration2, "template-2");

        extensions = Arrays.asList(packageRepositoryExtension, scmExtension, taskExtension, notificationExtension, configRepoExtension, authenticationExtension);
        pluginService = new PluginService(extensions, pluginDao, builder, securityService, entityHashingService);
    }

    @After
    public void tearDown() {
        PluginSettingsMetadataStore.getInstance().clear();
    }

    @Test
    public void shouldGetSettingsFromDBIfExists() {
        PluginSettings pluginSettings = pluginService.getPluginSettingsFor("plugin-id-1");

        assertThat(pluginSettings.getPluginSettingsKeys().size(), is(3));
        assertThat(pluginSettings.getValueFor("p1-k1"), is("v1"));
        assertThat(pluginSettings.getValueFor("p1-k2"), is(""));
        assertThat(pluginSettings.getValueFor("p1-k3"), is(nullValue()));
    }

    @Test
    public void shouldGetSettingsFromConfigurationIfItDoesNotExistInDB() {
        PluginSettings pluginSettings = pluginService.getPluginSettingsFor("plugin-id-2");

        assertThat(pluginSettings.getPluginSettingsKeys().size(), is(3));
        assertThat(pluginSettings.getValueFor("p2-k1"), is(""));
        assertThat(pluginSettings.getValueFor("p2-k2"), is(""));
        assertThat(pluginSettings.getValueFor("p2-k3"), is(""));

    }

    @Test
    public void shouldReturnPluginSettingsFromDbIfItExists() {
        PluginSettings pluginSettings = pluginService.getPluginSettings("plugin-id-1");

        assertThat(pluginSettings.getPluginSettingsKeys().size(), is(3));
        assertThat(pluginSettings.getValueFor("p1-k1"), is("v1"));
        assertThat(pluginSettings.getValueFor("p1-k2"), is(""));
        assertThat(pluginSettings.getValueFor("p1-k3"), is(nullValue()));
    }

    @Test
    public void shouldReturnNullIfPluginSettingsDoesNotExistInDb() {
        PluginSettings pluginSettings = pluginService.getPluginSettings("plugin-id-2");

        assertNull(pluginSettings);
    }

    @Test
    public void shouldNotSavePluginSettingsIfUserIsNotAnAdmin() {
        PluginSettings pluginSettings = new PluginSettings("some-plugin");
        Username currentUser = new Username("non-admin");
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        when(securityService.isUserAdmin(currentUser)).thenReturn(false);

        pluginService.createPluginSettings(currentUser, result, pluginSettings);

        assertThat(result.httpCode(), is(401));
        assertThat(result.toString(), containsString("UNAUTHORIZED_TO_EDIT"));
    }

    @Test
    public void shouldNotSavePluginSettingsIfPluginDoesNotExist() {
        PluginSettings pluginSettings = new PluginSettings("non-existent-plugin");
        Username currentUser = new Username("admin");
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        when(securityService.isUserAdmin(currentUser)).thenReturn(true);
        for (GoPluginExtension extension : extensions) {
            when(extension.canHandlePlugin("non-existent-plugin")).thenReturn(false);
        }

        pluginService.createPluginSettings(currentUser, result, pluginSettings);

        assertThat(result.httpCode(), is(422));
        assertThat(result.toString(), containsString("Plugin 'non-existent-plugin' is not supported by any extension point"));

    }

    @Test
    public void shouldNotSavePluginSettingsIfPluginReturnsValidationErrors() {
        PluginSettings pluginSettings = new PluginSettings("some-plugin");
        Username currentUser = new Username("admin");
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        when(securityService.isUserAdmin(currentUser)).thenReturn(true);

        when(configRepoExtension.canHandlePlugin("some-plugin")).thenReturn(true);
        ValidationResult validationResult = new ValidationResult();
        validationResult.addError(new ValidationError("foo", "foo is a required field"));
        when(configRepoExtension.validatePluginSettings(eq("some-plugin"), any(PluginSettingsConfiguration.class))).thenReturn(validationResult);

        pluginService.createPluginSettings(currentUser, result, pluginSettings);

        assertThat(result.httpCode(), is(422));
        assertThat(pluginSettings.errors().size(), is(1));
        assertThat(pluginSettings.getErrorFor("foo"), is(Arrays.asList("foo is a required field")));
    }

    @Test
    public void shouldSavePluginSettingsToDbIfPluginSettingsAreValidated() {
        Map<String, String> parameterMap = new HashMap<>();
        parameterMap.put("p2-k1", "v1");

        PluginSettings pluginSettings = new PluginSettings("plugin-id-2");
        pluginSettings.populateSettingsMap(parameterMap);

        Username currentUser = new Username("admin");
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        when(securityService.isUserAdmin(currentUser)).thenReturn(true);

        when(configRepoExtension.canHandlePlugin("plugin-id-2")).thenReturn(true);
        when(configRepoExtension.validatePluginSettings(eq("plugin-id-2"), any(PluginSettingsConfiguration.class))).thenReturn(new ValidationResult());

        pluginService.createPluginSettings(currentUser, result, pluginSettings);

        Plugin plugin = new Plugin("plugin-id-2", toJSON(parameterMap));
        verify(pluginDao).saveOrUpdate(plugin);

    }

    @Test
    public void shouldCheckForStaleRequestBeforeUpdatingPluginSettings() {
        PluginSettings pluginSettings = new PluginSettings("plugin-id-1");
        Username currentUser = new Username("admin");
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        String md5 = "md5";

        when(entityHashingService.md5ForEntity(pluginSettings)).thenReturn("foo");

        pluginService.updatePluginSettings(currentUser, result, pluginSettings, md5);

        assertThat(result.httpCode(), is(412));
        assertThat(result.toString(), containsString("STALE_RESOURCE_CONFIG"));
    }

    @Test
    public void shouldPopulateSettingsMapFromKeyValueMap() {
        Map<String, String> parameterMap = new HashMap<>();
        parameterMap.put("p3-k1", "v1");
        parameterMap.put("p3-k2", "");
        parameterMap.put("p3-k3", null);

        PluginSettings pluginSettings = pluginService.getPluginSettingsFor("plugin-id-3", parameterMap);

        assertThat(pluginSettings.getPluginSettingsKeys().size(), is(3));
        assertThat(pluginSettings.getValueFor("p3-k1"), is("v1"));
        assertThat(pluginSettings.getValueFor("p3-k2"), is(""));
        assertThat(pluginSettings.getValueFor("p3-k3"), is(nullValue()));
    }

    @Test
    public void shouldCallValidationOnPlugin() throws Exception {
        for (GoPluginExtension extension : extensions) {
            String pluginId = UUID.randomUUID().toString();
            when(extension.canHandlePlugin(pluginId)).thenReturn(true);
            when(extension.validatePluginSettings(eq(pluginId), any(PluginSettingsConfiguration.class))).thenReturn(new ValidationResult());

            PluginSettings pluginSettings = new PluginSettings(pluginId);
            pluginService.validatePluginSettingsFor(pluginSettings);

            verify(extension).validatePluginSettings(eq(pluginId), any(PluginSettingsConfiguration.class));
        }
    }

    @Test
    public void shouldTalkToPluginForPluginSettingsValidation_ConfigRepo() {
        when(configRepoExtension.isConfigRepoPlugin("plugin-id-4")).thenReturn(true);
        when(configRepoExtension.canHandlePlugin("plugin-id-4")).thenReturn(true);
        when(configRepoExtension.validatePluginSettings(eq("plugin-id-4"), any(PluginSettingsConfiguration.class))).thenReturn(new ValidationResult());

        PluginSettings pluginSettings = new PluginSettings("plugin-id-4");
        pluginService.validatePluginSettingsFor(pluginSettings);

        verify(configRepoExtension).validatePluginSettings(eq("plugin-id-4"), any(PluginSettingsConfiguration.class));
    }

    @Test
    public void shouldUpdatePluginSettingsWithErrorsIfExists() {
        when(notificationExtension.canHandlePlugin("plugin-id-4")).thenReturn(true);
        ValidationResult validationResult = new ValidationResult();
        validationResult.addError(new ValidationError("p4-k1", "m1"));
        validationResult.addError(new ValidationError("p4-k3", "m3"));
        when(notificationExtension.validatePluginSettings(eq("plugin-id-4"), any(PluginSettingsConfiguration.class))).thenReturn(validationResult);

        Map<String, String> parameterMap = new HashMap<>();
        parameterMap.put("p4-k1", "v1");
        parameterMap.put("p4-k2", "v2");
        parameterMap.put("p4-k3", "v3");

        PluginSettings pluginSettings = new PluginSettings("plugin-id-4");
        pluginSettings.populateSettingsMap(parameterMap);
        pluginService.validatePluginSettingsFor(pluginSettings);

        assertThat(pluginSettings.hasErrors(), is(true));
        assertThat(pluginSettings.getErrorFor("p4-k1"), is(Arrays.asList("m1")));
        assertThat(pluginSettings.getErrorFor("p4-k3"), is(Arrays.asList("m3")));
    }

    @Test
    public void shouldNotUpdatePluginSettingsWithErrorsIfNotExists() {
        when(notificationExtension.canHandlePlugin("plugin-id-4")).thenReturn(true);
        when(notificationExtension.validatePluginSettings(eq("plugin-id-4"), any(PluginSettingsConfiguration.class))).thenReturn(new ValidationResult());

        Map<String, String> parameterMap = new HashMap<>();
        parameterMap.put("p4-k1", "v1");
        parameterMap.put("p4-k2", "v2");
        PluginSettings pluginSettings = new PluginSettings("plugin-id-4");
        pluginSettings.populateSettingsMap(parameterMap);
        pluginService.validatePluginSettingsFor(pluginSettings);

        assertThat(pluginSettings.hasErrors(), is(false));
    }

    @Test
    public void shouldStorePluginSettingsToDBIfItDoesNotExist() {
        Map<String, String> parameterMap = new HashMap<>();
        parameterMap.put("p2-k1", "v1");
        parameterMap.put("p2-k2", "");
        parameterMap.put("p2-k3", null);

        PluginSettings pluginSettings = new PluginSettings("plugin-id-2");
        pluginSettings.populateSettingsMap(parameterMap);

        pluginService.savePluginSettingsFor(pluginSettings);

        Plugin plugin = new Plugin("plugin-id-2", toJSON(parameterMap));
        verify(pluginDao).saveOrUpdate(plugin);
    }

    @Test
    public void shouldUpdatePluginSettingsToDBIfItExists() {
        Map<String, String> parameterMap = new HashMap<>();
        parameterMap.put("p1-k1", "v1");
        parameterMap.put("p1-k2", "v2");
        parameterMap.put("p1-k3", null);

        PluginSettings pluginSettings = new PluginSettings("plugin-id-1");
        pluginSettings.populateSettingsMap(parameterMap);

        pluginService.savePluginSettingsFor(pluginSettings);

        Plugin plugin = new Plugin("plugin-id-1", toJSON(parameterMap));
        plugin.setId(1L);
        verify(pluginDao).saveOrUpdate(plugin);
    }

    @Test
    public void shouldFetchAllPluginInfos() {
        ArrayList<PluginInfo> pluginInfos = new ArrayList<>();

        when(builder.allPluginInfos(null)).thenReturn(pluginInfos);

        assertThat(pluginService.pluginInfos(null), Is.<List<PluginInfo>>is(pluginInfos));
    }

    @Test
    public void pluginInfosShouldApplyFilterIfTypeSpecified() {
        pluginService.pluginInfos("scm");

        verify(builder).allPluginInfos("scm");
    }

    @Test
    public void shouldFetchPluginInfoForTheSpecifiedId() {
        GoPluginDescriptor.About about = new GoPluginDescriptor.About("asdfasdf", "1.0", null, null, null, null);
        PluginInfo pluginInfo = new PluginInfo(new GoPluginDescriptor("github.pr", "1.0", about, null, null, false), null, null, null, null);
        when(builder.pluginInfoFor("github.pr")).thenReturn(pluginInfo);

        assertThat(pluginService.pluginInfo("github.pr"), is(pluginInfo));
    }


    private String toJSON(Map<String, String> configuration) {
        return new GsonBuilder().serializeNulls().create().toJson(configuration);
    }
}
