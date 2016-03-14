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
import com.thoughtworks.go.plugin.access.notification.NotificationExtension;
import com.thoughtworks.go.plugin.access.packagematerial.PackageAsRepositoryExtension;
import com.thoughtworks.go.plugin.access.packagematerial.PackageConfigurations;
import com.thoughtworks.go.plugin.access.packagematerial.PackageMetadataStore;
import com.thoughtworks.go.plugin.access.packagematerial.RepositoryMetadataStore;
import com.thoughtworks.go.plugin.access.pluggabletask.TaskExtension;
import com.thoughtworks.go.plugin.access.scm.SCMConfigurations;
import com.thoughtworks.go.plugin.access.scm.SCMExtension;
import com.thoughtworks.go.plugin.access.scm.SCMMetadataStore;
import com.thoughtworks.go.plugin.api.response.validation.ValidationError;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.infra.DefaultPluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.dao.PluginSqlMapDao;
import com.thoughtworks.go.server.domain.PluginSettings;
import com.thoughtworks.go.server.ui.PackageRepositoryPluginViewModel;
import com.thoughtworks.go.server.ui.PluginViewModel;
import com.thoughtworks.go.server.ui.SCMPluginViewModel;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.*;

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
    private PluginSqlMapDao pluginDao;

    private PluginService pluginService;
    private List<GoPluginExtension> extensions;
    private DefaultPluginManager defaultPluginManager;
    private GoPluginDescriptor goPluginDescriptor1;
    private GoPluginDescriptor goPluginDescriptor2;

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

        extensions = Arrays.asList(packageAsRepositoryExtension, scmExtension, taskExtension, notificationExtension, authenticationExtension);

        defaultPluginManager = mock(DefaultPluginManager.class);

        goPluginDescriptor1 = new GoPluginDescriptor("plugin-id1", null, new GoPluginDescriptor.About("plugin", "version", null, null, null, null), null, null, false);
        goPluginDescriptor2 = new GoPluginDescriptor("plugin-id2", null, new GoPluginDescriptor.About("plugin", "version", null, null, null, null), null, null, false);
        List<GoPluginDescriptor> pluginDescriptors = Arrays.asList(goPluginDescriptor1, goPluginDescriptor2);
        when(defaultPluginManager.plugins()).thenReturn(pluginDescriptors);

        when(defaultPluginManager.getPluginDescriptorFor(anyString())).thenReturn(goPluginDescriptor1);
        when(defaultPluginManager.hasReferenceFor((Class) anyObject(), anyString())).thenReturn(true);

        pluginService = new PluginService(extensions, pluginDao, defaultPluginManager);
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
    public void shouldPopulatePluginViewModelOfType() throws Exception {
        when(defaultPluginManager.isPluginOfType("scm","plugin-id1")).thenReturn(true);
        when(defaultPluginManager.isPluginOfType("scm","plugin-id2")).thenReturn(true);
        SCMConfigurations scmConfigurations = new SCMConfigurations();
        SCMMetadataStore.getInstance().addMetadataFor("plugin-id1", scmConfigurations, null);
        SCMMetadataStore.getInstance().addMetadataFor("plugin-id2", scmConfigurations, null);

        List<PluginViewModel> scmViewModels = pluginService.populatePluginViewModelsOfType("scm");
        SCMPluginViewModel scmPluginViewModel1 = (SCMPluginViewModel) scmViewModels.get(0);
        SCMPluginViewModel scmPluginViewModel2 = (SCMPluginViewModel) scmViewModels.get(1);

        assertThat(scmPluginViewModel1.getPluginId(), is("plugin-id1"));
        assertThat(scmPluginViewModel2.getPluginId(), is("plugin-id2"));
        assertThat(scmPluginViewModel1.getVersion(), is("version"));
        assertThat(scmPluginViewModel1.getConfigurations(), is(scmConfigurations.list()));
    }

    @Test
    public void shouldPopulatePluginViewModelsOfGivenType() throws Exception {
        when(defaultPluginManager.isPluginOfType("package-repository","plugin-id1")).thenReturn(true);
        when(defaultPluginManager.isPluginOfType("package-repository","plugin-id2")).thenReturn(true);
        PackageConfigurations packageConfigurations = new PackageConfigurations();
        PackageConfigurations repositoryConfigurations = new PackageConfigurations();
        RepositoryMetadataStore.getInstance().addMetadataFor("plugin-id1", repositoryConfigurations);
        RepositoryMetadataStore.getInstance().addMetadataFor("plugin-id2", repositoryConfigurations);
        PackageMetadataStore.getInstance().addMetadataFor("plugin-id1", packageConfigurations);
        PackageMetadataStore.getInstance().addMetadataFor("plugin-id2", packageConfigurations);

        List<PluginViewModel> packageRepositoryViewModels = pluginService.populatePluginViewModelsOfType("package-repository");
        PackageRepositoryPluginViewModel packageRepositoryPluginViewModel1 = (PackageRepositoryPluginViewModel) packageRepositoryViewModels.get(0);
        PackageRepositoryPluginViewModel packageRepositoryPluginViewModel2 = (PackageRepositoryPluginViewModel) packageRepositoryViewModels.get(1);

        assertThat(packageRepositoryPluginViewModel1.getPluginId(), is("plugin-id1"));
        assertThat(packageRepositoryPluginViewModel2.getPluginId(), is("plugin-id2"));
        assertThat(packageRepositoryPluginViewModel1.getVersion(), is("version"));
        assertThat(packageRepositoryPluginViewModel1.getPackageConfigurations(), is(packageConfigurations.list()));
        assertThat(packageRepositoryPluginViewModel1.getRepositoryConfigurations(), is(repositoryConfigurations.list()));
    }

    @Test
    public void shouldpopulatePluginViewModelGivenPluginIdAndTypeOfPlugin() throws Exception {
        when(defaultPluginManager.isPluginOfType("package-repository","plugin-id1")).thenReturn(true);
        PackageConfigurations packageConfigurations = new PackageConfigurations();
        PackageConfigurations repositoryConfigurations = new PackageConfigurations();
        RepositoryMetadataStore.getInstance().addMetadataFor("plugin-id1", repositoryConfigurations);
        PackageMetadataStore.getInstance().addMetadataFor("plugin-id1", packageConfigurations);

        PluginViewModel pluginViewModel = pluginService.populatePluginViewModel("package-repository", "plugin-id1");
        PackageRepositoryPluginViewModel packageRepositoryPluginViewModel = (PackageRepositoryPluginViewModel) pluginViewModel;

        assertThat(packageRepositoryPluginViewModel.getPluginId(), is("plugin-id1"));
        assertThat(packageRepositoryPluginViewModel.getVersion(), is("version"));
        assertThat(packageRepositoryPluginViewModel.getPackageConfigurations(), is(packageConfigurations.list()));
        assertThat(packageRepositoryPluginViewModel.getRepositoryConfigurations(), is(repositoryConfigurations.list()));
    }

    @Test
    public void shouldReturnNullForInvalidPluginId() throws Exception {
        when(defaultPluginManager.hasReferenceFor((Class)anyObject(), anyString())).thenReturn(false);
        PackageConfigurations packageConfigurations = new PackageConfigurations();
        PackageConfigurations repositoryConfigurations = new PackageConfigurations();
        RepositoryMetadataStore.getInstance().addMetadataFor("plugin-id", repositoryConfigurations);
        PackageMetadataStore.getInstance().addMetadataFor("plugin-id", packageConfigurations);

        PluginViewModel pluginViewModel = pluginService.populatePluginViewModel("scm", "invalid-plugin-id");
        SCMPluginViewModel scmPluginViewModel = (SCMPluginViewModel) pluginViewModel;

        assertThat(scmPluginViewModel, is(nullValue()));
    }

    private String toJSON(Map<String, String> configuration) {
        return new GsonBuilder().serializeNulls().create().toJson(configuration);
    }
}
