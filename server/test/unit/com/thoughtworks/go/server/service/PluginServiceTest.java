/*
 * Copyright 2016 ThoughtWorks, Inc.
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
import com.thoughtworks.go.plugin.access.packagematerial.PackageAsRepositoryExtension;
import com.thoughtworks.go.plugin.access.pluggabletask.TaskExtension;
import com.thoughtworks.go.plugin.access.scm.SCMConfiguration;
import com.thoughtworks.go.plugin.access.scm.SCMConfigurations;
import com.thoughtworks.go.plugin.access.scm.SCMExtension;
import com.thoughtworks.go.plugin.access.scm.SCMMetadataStore;
import com.thoughtworks.go.plugin.api.GoPluginIdentifier;
import com.thoughtworks.go.plugin.api.response.validation.ValidationError;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.infra.DefaultPluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.dao.PluginSqlMapDao;
import com.thoughtworks.go.server.domain.PluginSettings;
import com.thoughtworks.go.server.ui.PluginViewModel;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.*;

import static java.util.Arrays.asList;
import static junit.framework.TestCase.assertNull;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class PluginServiceTest {
    @Mock
    private PackageAsRepositoryExtension packageAsRepositoryExtension;
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
    private DefaultPluginManager pluginManager;

    private PluginService pluginService;
    private List<GoPluginExtension> extensions;

    @Before
    public void setUp() {
        initMocks(this);

        PluginSettingsMetadataStore.getInstance().clear();

        Map<String, String> configuration = new HashMap<String, String>();
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


        extensions = Arrays.asList(packageAsRepositoryExtension, scmExtension, taskExtension, notificationExtension, configRepoExtension, authenticationExtension);
        pluginService = new PluginService(extensions, pluginDao, pluginManager);
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
    public void shouldPopulateSettingsMapFromKeyValueMap() {
        Map<String, String> parameterMap = new HashMap<String, String>();
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

        Map<String, String> parameterMap = new HashMap<String, String>();
        parameterMap.put("p4-k1", "v1");
        parameterMap.put("p4-k2", "v2");
        PluginSettings pluginSettings = new PluginSettings("plugin-id-4");
        pluginSettings.populateSettingsMap(parameterMap);
        pluginService.validatePluginSettingsFor(pluginSettings);

        assertThat(pluginSettings.hasErrors(), is(true));
        assertThat(pluginSettings.getErrorFor("p4-k1"), is("m1"));
        assertThat(pluginSettings.getErrorFor("p4-k2"), is(nullValue()));
        assertThat(pluginSettings.getErrorFor("p4-k3"), is("m3"));
    }

    @Test
    public void shouldNotUpdatePluginSettingsWithErrorsIfNotExists() {
        when(notificationExtension.canHandlePlugin("plugin-id-4")).thenReturn(true);
        when(notificationExtension.validatePluginSettings(eq("plugin-id-4"), any(PluginSettingsConfiguration.class))).thenReturn(new ValidationResult());

        Map<String, String> parameterMap = new HashMap<String, String>();
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
    public void pluginsShouldFetchAllPlugins() {
        GoPluginIdentifier taskPluginIdentifier = new GoPluginIdentifier("task", asList("1.0"));
        GoPluginIdentifier authPluginIdentifier = new GoPluginIdentifier("authentication", asList("1.0"));

        GoPluginDescriptor.About taskPluginAbout = new GoPluginDescriptor.About("name", "1.2.3", "target", "desc", null, null);
        GoPluginDescriptor taskDescriptor = new GoPluginDescriptor("task_plugin_id", "do_not_use_version", taskPluginAbout, null, null, false);

        GoPluginDescriptor.About authPluginAbout = new GoPluginDescriptor.About("name", "1.5.8", "target", "desc", null, null);
        GoPluginDescriptor authDescriptor = new GoPluginDescriptor("auth_plugin_id", "do_not_use_version", authPluginAbout, null, null, false);

        when(pluginManager.plugins()).thenReturn(Arrays.asList(taskDescriptor, authDescriptor));
        when(pluginManager.pluginFor("task_plugin_id")).thenReturn(taskPluginIdentifier);
        when(pluginManager.pluginFor("auth_plugin_id")).thenReturn(authPluginIdentifier);

        ArrayList<PluginViewModel> pluginViewModels = pluginService.plugins(null);

        assertThat(pluginViewModels.size(), is(2));
        assertThat(pluginViewModels.get(0), is(new PluginViewModel("task_plugin_id", "name", "1.2.3", "task")));
        assertThat(pluginViewModels.get(1), is(new PluginViewModel("auth_plugin_id", "name", "1.5.8", "authentication")));
    }

    @Test
    public void pluginsShouldApplyFilterIfTypeSpecified() {
        GoPluginIdentifier taskPluginIdentifier = new GoPluginIdentifier("task", asList("1.0"));

        GoPluginDescriptor.About taskPluginAbout = new GoPluginDescriptor.About("name", "1.2.3", "target", "desc", null, null);
        GoPluginDescriptor taskDescriptor = new GoPluginDescriptor("task_plugin_id", "do_not_use_version", taskPluginAbout, null, null, false);

        GoPluginDescriptor.About authPluginAbout = new GoPluginDescriptor.About("name", "1.5.8", "target", "desc", null, null);
        GoPluginDescriptor authDescriptor = new GoPluginDescriptor("auth_plugin_id", "do_not_use_version", authPluginAbout, null, null, false);

        when(pluginManager.plugins()).thenReturn(Arrays.asList(taskDescriptor, authDescriptor));
        when(pluginManager.pluginFor("task_plugin_id")).thenReturn(taskPluginIdentifier);
        when(pluginManager.isPluginOfType("task", "task_plugin_id")).thenReturn(true);
        when(pluginManager.isPluginOfType("task", "auth_plugin_id")).thenReturn(false);


        ArrayList<PluginViewModel> pluginViewModels = pluginService.plugins("task");

        assertThat(pluginViewModels.size(), is(1));
        assertThat(pluginViewModels.get(0), is(new PluginViewModel("task_plugin_id", "name", "1.2.3", "task")));
    }

    @Test
    public void pluginShouldFetchPluginForTheSpecifiedId() {
        SCMConfigurations scmConfigurations = new SCMConfigurations();
        scmConfigurations.add(new SCMConfiguration("key1"));
        SCMMetadataStore.getInstance().addMetadataFor("scm_plugin_id", scmConfigurations, null);


        GoPluginIdentifier taskPluginIdentifier = new GoPluginIdentifier("scm", asList("1.0"));
        GoPluginDescriptor.About taskPluginAbout = new GoPluginDescriptor.About("name", "1.2.3", "target", "desc", null, null);
        GoPluginDescriptor taskDescriptor = new GoPluginDescriptor("scm_plugin_id", "do_not_use_version", taskPluginAbout, null, null, false);

        when(pluginManager.getPluginDescriptorFor("scm_plugin_id")).thenReturn(taskDescriptor);
        when(pluginManager.pluginFor("scm_plugin_id")).thenReturn(taskPluginIdentifier);

        PluginViewModel taskPlugin = pluginService.plugin("scm_plugin_id");

        assertThat(taskPlugin.getId(), is("scm_plugin_id"));
        assertThat(taskPlugin.getConfigurations().size(), is(1));
    }

    @Test
    public void pluginShouldBeNullInAbsenceOfDescriptor() {
        when(pluginManager.getPluginDescriptorFor("id")).thenReturn(null);

        assertNull(pluginService.plugin("id"));
    }

    @Test
    public void pluginShouldBeNullInAbsenceOfIdentifier() {
        GoPluginDescriptor taskDescriptor = new GoPluginDescriptor("id", "version", null, null, null, false);

        when(pluginManager.getPluginDescriptorFor("id")).thenReturn(taskDescriptor);
        when(pluginManager.pluginFor("id")).thenReturn(null);

        assertNull(pluginService.plugin("id"));
    }

    private String toJSON(Map<String, String> configuration) {
        return new GsonBuilder().serializeNulls().create().toJson(configuration);
    }
}
