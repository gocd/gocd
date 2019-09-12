/*
 * Copyright 2020 ThoughtWorks, Inc.
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

import com.google.gson.JsonObject;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.domain.NullPlugin;
import com.thoughtworks.go.domain.Plugin;
import com.thoughtworks.go.domain.config.ConfigurationKey;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.config.EncryptedConfigurationValue;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationExtension;
import com.thoughtworks.go.plugin.access.common.settings.GoPluginExtension;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsConfiguration;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsMetadataStore;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsProperty;
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentExtension;
import com.thoughtworks.go.plugin.api.response.validation.ValidationError;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.domain.authorization.AuthorizationPluginInfo;
import com.thoughtworks.go.plugin.domain.common.*;
import com.thoughtworks.go.plugin.domain.elastic.ElasticAgentPluginInfo;
import com.thoughtworks.go.plugin.domain.notification.NotificationPluginInfo;
import com.thoughtworks.go.plugin.domain.scm.SCMPluginInfo;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.dao.PluginSqlMapDao;
import com.thoughtworks.go.server.domain.PluginSettings;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.plugins.builder.DefaultPluginInfoFinder;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.serverhealth.HealthStateType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.List;

import static com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl;
import static com.thoughtworks.go.plugin.domain.common.PluginConstants.AUTHORIZATION_EXTENSION;
import static com.thoughtworks.go.plugin.domain.common.PluginConstants.ELASTIC_AGENT_EXTENSION;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class PluginServiceTest {
    private PluginService pluginService;
    private Username currentUser;
    @Mock
    private HttpLocalizedOperationResult result;
    @Mock
    private PluginSqlMapDao pluginDao;
    @Mock
    private SecurityService securityService;
    @Mock
    private EntityHashingService entityHashingService;
    @Mock
    private DefaultPluginInfoFinder defaultPluginInfoFinder;

    @Mock
    private AuthorizationExtension authorizationExtension;
    @Mock
    private ElasticAgentExtension elasticAgentExtension;
    @Mock
    private PluginSettings pluginSettings;
    @Mock
    private PluginManager pluginManager;

    private List<GoPluginExtension> extensions;
    private final String authorizationPluginId = "cd.go.authorization.ldap";
    private final String elasticAgentPluginId = "cd.go.elastic-agent.docker";

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        currentUser = new Username("bob");

        when(authorizationExtension.extensionName()).thenReturn(AUTHORIZATION_EXTENSION);
        when(elasticAgentExtension.extensionName()).thenReturn(ELASTIC_AGENT_EXTENSION);
        extensions = Arrays.asList(authorizationExtension, elasticAgentExtension);

        pluginService = new PluginService(extensions, pluginDao, securityService, entityHashingService, defaultPluginInfoFinder, pluginManager);
    }

    @After
    public void tearDown() throws Exception {
        PluginSettingsMetadataStore.getInstance().clear();
    }

    @Test
    public void shouldAskPluginMangerForPluginIsLoadedOrNot() {
        pluginService.isPluginLoaded(elasticAgentPluginId);

        verify(pluginManager).isPluginLoaded(elasticAgentPluginId);
    }

    @Test
    public void shouldReturnNullIfPluginSettingsDoesNotExistInDb() {
        when(pluginDao.findPlugin(elasticAgentPluginId)).thenReturn(new NullPlugin());

        PluginSettings pluginSettings = pluginService.getPluginSettings(elasticAgentPluginId);

        assertNull(pluginSettings);
    }

    @Test
    public void getPluginSettings_shouldReturnPluginSettingsFromDbIfItExists() {
        setUpElasticPluginForTheTest(true);
        when(pluginDao.findPlugin(elasticAgentPluginId)).thenReturn(getPlugin(elasticAgentPluginId));

        PluginSettings pluginSettings = pluginService.getPluginSettings(elasticAgentPluginId);

        assertThat(pluginSettings.getSettingsAsKeyValuePair().keySet().size(), is(3));
        assertThat(pluginSettings.getSettingsAsKeyValuePair(), hasEntry("key-1", "v1"));
        assertThat(pluginSettings.getSettingsAsKeyValuePair(), hasEntry("key-2", ""));
        assertThat(pluginSettings.getSettingsAsKeyValuePair(), hasEntry("key-3", null));
    }

    @Test
    public void shouldNotifyTheExtensionWhichHandlesSettingsInAPluginWithMultipleExtensions_WhenPluginSettingsHaveChanged() {
        setUpElasticPluginForTheTest(true);
        when(pluginDao.findPlugin(elasticAgentPluginId)).thenReturn(new NullPlugin());
        when(pluginSettings.toPluginSettingsConfiguration()).thenReturn(mock(PluginSettingsConfiguration.class));

        when(elasticAgentExtension.validatePluginSettings(eq(elasticAgentPluginId), any(PluginSettingsConfiguration.class))).thenReturn(new ValidationResult());
        when(pluginSettings.hasErrors()).thenReturn(false);

        pluginService.createPluginSettings(pluginSettings, currentUser, new HttpLocalizedOperationResult());

        verify(elasticAgentExtension).notifyPluginSettingsChange(elasticAgentPluginId, pluginSettings.getSettingsAsKeyValuePair());

        verify(authorizationExtension, never()).notifyPluginSettingsChange(elasticAgentPluginId, pluginSettings.getSettingsAsKeyValuePair());
    }

    @Test
    public void shouldIgnoreErrorsWhileNotifyingPluginSettingChange() {
        setUpElasticPluginForTheTest(true);
        when(pluginDao.findPlugin(elasticAgentPluginId)).thenReturn(new NullPlugin());
        when(pluginSettings.toPluginSettingsConfiguration()).thenReturn(mock(PluginSettingsConfiguration.class));

        when(result.isSuccessful()).thenReturn(true);
        when(pluginSettings.hasErrors()).thenReturn(false);
        when(elasticAgentExtension.validatePluginSettings(eq(elasticAgentPluginId), any(PluginSettingsConfiguration.class))).thenReturn(new ValidationResult());

        doThrow(new RuntimeException()).when(elasticAgentExtension).notifyPluginSettingsChange(eq(elasticAgentPluginId), anyMap());

        pluginService.createPluginSettings(pluginSettings, currentUser, result);

        verify(pluginDao, times(1)).saveOrUpdate(any());
        verify(elasticAgentExtension).notifyPluginSettingsChange(eq(elasticAgentPluginId), anyMap());
        assertTrue(result.isSuccessful());
    }

    @Test
    public void validatePluginSettingsFor_shouldTalkToPluginToValidatePluginSettings() {
        setUpElasticPluginForTheTest(true);
        when(pluginDao.findPlugin(elasticAgentPluginId)).thenReturn(new NullPlugin());

        when(elasticAgentExtension.validatePluginSettings(eq(elasticAgentPluginId), any(PluginSettingsConfiguration.class))).thenReturn(new ValidationResult());

        PluginSettings pluginSettings = new PluginSettings(elasticAgentPluginId);
        pluginService.validatePluginSettings(pluginSettings);

        verify(elasticAgentExtension).validatePluginSettings(eq(elasticAgentPluginId), any(PluginSettingsConfiguration.class));
    }

    @Test
    public void validatePluginSettingsFor_shouldCheckIfSecureValuesSetByUserAreValid() {
        String secureKey = "secure-key";
        setUpElasticPluginForTheTest(true);
        when(pluginDao.findPlugin(elasticAgentPluginId)).thenReturn(new NullPlugin());

        PluginSettings pluginSettings = new PluginSettings(elasticAgentPluginId);
        final PluginInfo pluginInfo = mock(PluginInfo.class);
        when(pluginInfo.isSecure(secureKey)).thenReturn(true);
        pluginSettings.addConfigurations(pluginInfo, Arrays.asList(new ConfigurationProperty(new ConfigurationKey(secureKey), new EncryptedConfigurationValue("value_encrypted_by_a_different_cipher"))));

        pluginService.validatePluginSettings(pluginSettings);

        assertThat(pluginSettings.hasErrors(), is(true));
        List<String> allErrorsOnProperty = pluginSettings.getPluginSettingsProperties().get(0).errors().getAll();
        assertThat(allErrorsOnProperty.size(), is(1));
        assertTrue(allErrorsOnProperty.contains("Encrypted value for property with key 'secure-key' is invalid. This usually happens when the cipher text is modified to have an invalid value."));
        verify(elasticAgentExtension, never()).validatePluginSettings(eq(elasticAgentPluginId), any(PluginSettingsConfiguration.class));
    }

    @Test
    public void validatePluginSettingsFor_shouldPopulateValidationErrorsInPluginSettingsObject() {
        final ElasticAgentPluginInfo elasticAgentPluginInfo = mockElasticAgentPluginInfo(getPluggableInstanceSettings());
        final ValidationResult validationResult = new ValidationResult();
        validationResult.addError(new ValidationError("key-1", "m1"));
        validationResult.addError(new ValidationError("key-3", "m3"));

        addPluginSettingsMetadataToStore(elasticAgentPluginId, ELASTIC_AGENT_EXTENSION, getPluginSettingsConfiguration());
        when(elasticAgentExtension.extensionName()).thenReturn(ELASTIC_AGENT_EXTENSION);
        when(elasticAgentExtension.canHandlePlugin(elasticAgentPluginId)).thenReturn(true);
        when(elasticAgentExtension.validatePluginSettings(eq(elasticAgentPluginId), any(PluginSettingsConfiguration.class)))
                .thenReturn(validationResult);

        PluginSettings pluginSettings = PluginSettings.from(getPlugin(elasticAgentPluginId), elasticAgentPluginInfo);

        pluginService.validatePluginSettings(pluginSettings);

        assertThat(pluginSettings.hasErrors(), is(true));
        assertThat(pluginSettings.getErrorFor("key-1"), is(Arrays.asList("m1")));
        assertThat(pluginSettings.getErrorFor("key-3"), is(Arrays.asList("m3")));
    }

    @Test
    public void validatePluginSettingsFor_shouldNotAddAnyErrorInPluginSettingsObjectWhenThereIsNoValidationError() {
        final ElasticAgentPluginInfo elasticAgentPluginInfo = mockElasticAgentPluginInfo(getPluggableInstanceSettings());

        addPluginSettingsMetadataToStore(elasticAgentPluginId, ELASTIC_AGENT_EXTENSION, getPluginSettingsConfiguration());
        when(elasticAgentExtension.extensionName()).thenReturn(ELASTIC_AGENT_EXTENSION);
        when(elasticAgentExtension.canHandlePlugin(elasticAgentPluginId)).thenReturn(true);
        when(elasticAgentExtension.validatePluginSettings(eq(elasticAgentPluginId), any(PluginSettingsConfiguration.class)))
                .thenReturn(new ValidationResult());

        PluginSettings pluginSettings = PluginSettings.from(getPlugin(elasticAgentPluginId), elasticAgentPluginInfo);

        pluginService.validatePluginSettings(pluginSettings);

        assertThat(pluginSettings.hasErrors(), is(false));
    }

    @Test
    public void shouldStorePluginSettingsToDBIfItDoesNotExist() {
        final ArgumentCaptor<Plugin> pluginArgumentCaptor = ArgumentCaptor.forClass(Plugin.class);
        final ElasticAgentPluginInfo elasticAgentPluginInfo = mockElasticAgentPluginInfo(getPluggableInstanceSettings());
        final PluginSettings pluginSettings = PluginSettings.from(getPlugin(elasticAgentPluginId), elasticAgentPluginInfo);

        when(pluginDao.findPlugin(elasticAgentPluginId)).thenReturn(new NullPlugin());

        pluginService.saveOrUpdatePluginSettingsInDB(pluginSettings);

        verify(pluginDao).saveOrUpdate(pluginArgumentCaptor.capture());

        final Plugin plugin = pluginArgumentCaptor.getValue();

        assertThat(plugin.getPluginId(), is(elasticAgentPluginId));
        assertThat(plugin.getConfigurationValue("key-1"), is("v1"));
        assertThat(plugin.getConfigurationValue("key-2"), is(""));
        assertThat(plugin.getConfigurationValue("key-3"), nullValue());
    }

    @Test
    public void shouldUpdatePluginSettingsToDBIfItExists() {
        JsonObject configuration = new JsonObject();
        configuration.addProperty("key-1", "old-value");

        final Plugin pluginInDB = new Plugin().setPluginId(elasticAgentPluginId).setConfiguration(configuration.toString());
        final ArgumentCaptor<Plugin> pluginArgumentCaptor = ArgumentCaptor.forClass(Plugin.class);
        final ElasticAgentPluginInfo elasticAgentPluginInfo = mockElasticAgentPluginInfo(getPluggableInstanceSettings());

        when(pluginDao.findPlugin(elasticAgentPluginId)).thenReturn(pluginInDB);

        final PluginSettings newPluginSettings = PluginSettings.from(getPlugin(elasticAgentPluginId), elasticAgentPluginInfo);
        pluginService.saveOrUpdatePluginSettingsInDB(newPluginSettings);

        verify(pluginDao).saveOrUpdate(pluginArgumentCaptor.capture());

        final Plugin plugin = pluginArgumentCaptor.getValue();
        assertThat(plugin.getPluginId(), is(elasticAgentPluginId));
        assertThat(plugin.getConfigurationValue("key-1"), is("v1"));
        assertThat(plugin.getConfigurationValue("key-2"), is(""));
        assertThat(plugin.getConfigurationValue("key-3"), nullValue());
    }

    @Test
    public void shouldGetPluginInfoFromTheExtensionWhichImplementsPluginSettingsIfThePluginImplementsMultipleExtensions() {
        final String pluginId = "plugin-id";
        final CombinedPluginInfo combinedPluginInfo = mock(CombinedPluginInfo.class);
        final ElasticAgentPluginInfo elasticAgentPluginInfo = mockElasticAgentPluginInfo(null);
        final AuthorizationPluginInfo authorizationPluginInfo = mock(AuthorizationPluginInfo.class);

        PluginSettingsMetadataStore.getInstance().addMetadataFor(pluginId, PluginConstants.ELASTIC_AGENT_EXTENSION, new PluginSettingsConfiguration(), "template-1");
        when(defaultPluginInfoFinder.pluginInfoFor(pluginId)).thenReturn(combinedPluginInfo);
        when(combinedPluginInfo.extensionFor(ELASTIC_AGENT_EXTENSION)).thenReturn(elasticAgentPluginInfo);
        when(combinedPluginInfo.extensionFor(AUTHORIZATION_EXTENSION)).thenReturn(authorizationPluginInfo);
        when(defaultPluginInfoFinder.pluginInfoFor(pluginId)).thenReturn(combinedPluginInfo);
        when(elasticAgentExtension.canHandlePlugin(pluginId)).thenReturn(true);
        when(authorizationExtension.canHandlePlugin(pluginId)).thenReturn(true);

        PluginInfo pluginInfo = pluginService.pluginInfoForExtensionThatHandlesPluginSettings(pluginId);

        assertTrue(pluginInfo instanceof ElasticAgentPluginInfo);
        assertThat(pluginInfo, is(elasticAgentPluginInfo));
    }

    @Test
    public void shouldReturnNullForGetPluginInfoIfDoesNotImplementPluginSettings_MultipleExtensionImpl() {
        String pluginId = "plugin-id";
        CombinedPluginInfo combinedPluginInfo = new CombinedPluginInfo();
        GoPluginDescriptor pluginDescriptor = GoPluginDescriptor.builder().id(pluginId).build();
        NotificationPluginInfo notificationPluginInfo = new NotificationPluginInfo(pluginDescriptor, null);
        combinedPluginInfo.add(notificationPluginInfo);
        SCMPluginInfo scmPluginInfo = new SCMPluginInfo(pluginDescriptor, "display_name", new PluggableInstanceSettings(null), null);
        combinedPluginInfo.add(scmPluginInfo);

        when(elasticAgentExtension.canHandlePlugin(pluginId)).thenReturn(true);
        when(authorizationExtension.canHandlePlugin(pluginId)).thenReturn(true);

        assertNull(pluginService.pluginInfoForExtensionThatHandlesPluginSettings(pluginId));
    }

    @Test
    public void createPluginSettings_shouldReturnUnprocessableEntityStatusCodeWhenUserIsNotAnAdmin() {
        when(securityService.isUserAdmin(currentUser)).thenReturn(false);
        when(pluginSettings.getPluginId()).thenReturn(authorizationPluginId);

        pluginService.createPluginSettings(pluginSettings, currentUser, result);

        verify(pluginDao, times(0)).saveOrUpdate(any());
        verify(authorizationExtension, times(0)).notifyPluginSettingsChange(eq(authorizationPluginId), anyMap());
        verify(result, times(1)).forbidden(eq(EntityType.PluginSettings.forbiddenToEdit(pluginSettings.getPluginId(), currentUser.getUsername())), eq(HealthStateType.forbidden()));
        verifyNoMoreInteractions(result);
    }

    @Test
    public void createPluginSettings_shouldReturnUnprocessableEntityStatusCodeWhenPluginSettingsIsAlreadyCreatedForThePlugin() {
        setUpElasticPluginForTheTest(true);
        when(pluginDao.findPlugin(elasticAgentPluginId)).thenReturn(getPlugin(elasticAgentPluginId));

        pluginService.createPluginSettings(pluginSettings, currentUser, result);

        verify(pluginDao, times(0)).saveOrUpdate(any());
        verify(elasticAgentExtension, times(0)).notifyPluginSettingsChange(eq(elasticAgentPluginId), anyMap());
        verify(result, times(1)).unprocessableEntity("Save failed. Plugin settings for the plugin `cd.go.elastic-agent.docker` already exist. In order to update the plugin settings refer the " + apiDocsUrl("#update-plugin-settings") + ".");
        verifyNoMoreInteractions(result);
    }

    @Test
    public void createPluginSettings_shouldValidatePluginSettingUsingExtensionAndReturnUnprocessableEntityWhenThereIsAValidationError() {
        setUpElasticPluginForTheTest(true);
        when(pluginDao.findPlugin(elasticAgentPluginId)).thenReturn(new NullPlugin());

        final ValidationResult validationResult = new ValidationResult();
        validationResult.addError(new ValidationError("key-1", "error-message"));
        when(elasticAgentExtension.validatePluginSettings(eq(elasticAgentPluginId), any(PluginSettingsConfiguration.class))).thenReturn(validationResult);
        when(pluginSettings.hasErrors()).thenReturn(true);

        pluginService.createPluginSettings(pluginSettings, currentUser, result);

        verify(pluginDao, times(0)).saveOrUpdate(any());
        verify(elasticAgentExtension, times(0)).notifyPluginSettingsChange(eq(elasticAgentPluginId), anyMap());
        verify(result, times(1)).unprocessableEntity("Save failed. There are errors in the plugin settings. Please fix them and resubmit.");
        verifyNoMoreInteractions(result);
    }

    @Test
    public void createPluginSettings_shouldSavePluginSettingsAndNotifyExtensionsWhenThereIsNoValidationFailure() {
        setUpElasticPluginForTheTest(true);
        when(pluginDao.findPlugin(elasticAgentPluginId)).thenReturn(new NullPlugin());
        when(pluginSettings.toPluginSettingsConfiguration()).thenReturn(mock(PluginSettingsConfiguration.class));

        when(elasticAgentExtension.validatePluginSettings(eq(elasticAgentPluginId), any(PluginSettingsConfiguration.class))).thenReturn(new ValidationResult());
        when(pluginSettings.hasErrors()).thenReturn(false);

        pluginService.createPluginSettings(pluginSettings, currentUser, result);

        verify(pluginDao, times(1)).saveOrUpdate(any());
        verify(elasticAgentExtension, times(1)).notifyPluginSettingsChange(eq(elasticAgentPluginId), anyMap());
        verifyNoMoreInteractions(result);
    }

    @Test
    public void updatePluginSettings_shouldReturnUnprocessableEntityStatusCodeWhenUserIsNotAnAdmin() {
        when(securityService.isUserAdmin(currentUser)).thenReturn(false);
        when(pluginSettings.getPluginId()).thenReturn(authorizationPluginId);

        pluginService.updatePluginSettings(pluginSettings, currentUser, result, null);

        verify(pluginDao, times(0)).saveOrUpdate(any());
        verify(authorizationExtension, times(0)).notifyPluginSettingsChange(eq(authorizationPluginId), anyMap());
        verify(result, times(1)).forbidden(eq(EntityType.PluginSettings.forbiddenToEdit(authorizationPluginId, currentUser.getUsername())), eq(HealthStateType.forbidden()));
        verifyNoMoreInteractions(result);

    }

    @Test
    public void updatePluginSettings_shouldReturnUnprocessableEntityStatusCodeWhenPluginSettingsDoesNotExistForThePlugin() {
        setUpElasticPluginForTheTest(true);
        when(pluginDao.findPlugin(elasticAgentPluginId)).thenReturn(new NullPlugin());

        pluginService.updatePluginSettings(pluginSettings, currentUser, result, null);

        verify(pluginDao, times(0)).saveOrUpdate(any());
        verify(elasticAgentExtension, times(0)).notifyPluginSettingsChange(eq(elasticAgentPluginId), anyMap());
        verify(result, times(1)).notFound(eq(EntityType.PluginSettings.notFoundMessage(elasticAgentPluginId)), eq(HealthStateType.notFound()));
        verifyNoMoreInteractions(result);
    }

    @Test
    public void updatePluginSettings_shouldCheckForStaleRequest() {
        setUpElasticPluginForTheTest(true);
        when(pluginDao.findPlugin(elasticAgentPluginId)).thenReturn(getPlugin(elasticAgentPluginId));
        when(entityHashingService.md5ForEntity(any(PluginSettings.class))).thenReturn("bar");

        pluginService.updatePluginSettings(pluginSettings, currentUser, result, "foo");

        verify(pluginDao, times(0)).saveOrUpdate(any());
        verify(elasticAgentExtension, times(0)).notifyPluginSettingsChange(eq(elasticAgentPluginId), anyMap());
        verify(result, times(1)).stale(EntityType.PluginSettings.staleConfig(elasticAgentPluginId));
        verifyNoMoreInteractions(result);
    }

    @Test
    public void updatePluginSettings_shouldValidatePluginSettingUsingExtensionAndReturnUnprocessableEntityWhenThereIsAValidationFailure() {
        setUpElasticPluginForTheTest(true);
        when(pluginDao.findPlugin(elasticAgentPluginId)).thenReturn(getPlugin(elasticAgentPluginId));

        final ValidationResult validationResult = new ValidationResult();
        validationResult.addError(new ValidationError("key-1", "error-message"));
        when(elasticAgentExtension.validatePluginSettings(eq(elasticAgentPluginId), any(PluginSettingsConfiguration.class))).thenReturn(validationResult);
        when(pluginSettings.hasErrors()).thenReturn(true);
        when(entityHashingService.md5ForEntity(any(PluginSettings.class))).thenReturn("foo");
        when(result.isSuccessful()).thenReturn(false);

        pluginService.updatePluginSettings(pluginSettings, currentUser, result, "foo");

        verify(pluginDao, times(0)).saveOrUpdate(any());
        verify(elasticAgentExtension, times(0)).notifyPluginSettingsChange(eq(elasticAgentPluginId), anyMap());
        verify(result, times(1)).unprocessableEntity("Save failed. There are errors in the plugin settings. Please fix them and resubmit.");
    }

    @Test
    public void updatePluginSettings_shouldUpdateThePluginSettingsWhenThereIsNoValidationFailure() {
        final Plugin plugin = getPlugin(elasticAgentPluginId);
        setUpElasticPluginForTheTest(true);
        when(pluginDao.findPlugin(elasticAgentPluginId)).thenReturn(plugin);
        when(entityHashingService.md5ForEntity(any(PluginSettings.class))).thenReturn("foo");
        when(pluginSettings.toPluginSettingsConfiguration()).thenReturn(mock(PluginSettingsConfiguration.class));

        when(elasticAgentExtension.validatePluginSettings(eq(elasticAgentPluginId), any(PluginSettingsConfiguration.class)))
                .thenReturn(new ValidationResult());

        when(pluginSettings.hasErrors()).thenReturn(false);
        when(result.isSuccessful()).thenReturn(true);

        pluginService.updatePluginSettings(pluginSettings, currentUser, result, "foo");

        verify(pluginDao, times(1)).saveOrUpdate(plugin);
        verify(elasticAgentExtension, times(1)).notifyPluginSettingsChange(eq(elasticAgentPluginId), anyMap());
        verify(result, times(1)).isSuccessful();
        verify(entityHashingService, times(1)).removeFromCache(pluginSettings, pluginSettings.getPluginId());
        verifyNoMoreInteractions(result);
    }

    private void setUpElasticPluginForTheTest(boolean isCurrentUserAdmin) {
        final CombinedPluginInfo combinedPluginInfo = mock(CombinedPluginInfo.class);
        final ElasticAgentPluginInfo elasticAgentPluginInfo = mockElasticAgentPluginInfo(getPluggableInstanceSettings());

        when(defaultPluginInfoFinder.pluginInfoFor(elasticAgentPluginId)).thenReturn(combinedPluginInfo);
        when(combinedPluginInfo.extensionFor(ELASTIC_AGENT_EXTENSION)).thenReturn(elasticAgentPluginInfo);

        addPluginSettingsMetadataToStore(elasticAgentPluginId, ELASTIC_AGENT_EXTENSION, getPluginSettingsConfiguration());
        when(securityService.isUserAdmin(currentUser)).thenReturn(isCurrentUserAdmin);
        when(pluginSettings.getPluginId()).thenReturn(elasticAgentPluginId);
        when(elasticAgentExtension.canHandlePlugin(elasticAgentPluginId)).thenReturn(true);
    }

    private ElasticAgentPluginInfo mockElasticAgentPluginInfo(PluggableInstanceSettings pluggableInstanceSettings) {
        final ElasticAgentPluginInfo elasticAgentPluginInfo = mock(ElasticAgentPluginInfo.class);
        when(elasticAgentPluginInfo.getPluginSettings()).thenReturn(pluggableInstanceSettings);
        return elasticAgentPluginInfo;
    }

    private void addPluginSettingsMetadataToStore(String pluginId, String extensionName, PluginSettingsConfiguration configuration) {
        PluginSettingsMetadataStore.getInstance().addMetadataFor(pluginId, extensionName, configuration, "template-1");
    }

    private PluginSettingsConfiguration getPluginSettingsConfiguration() {
        PluginSettingsConfiguration configuration = new PluginSettingsConfiguration();
        configuration.add(new PluginSettingsProperty("key-1"));
        configuration.add(new PluginSettingsProperty("key-2"));
        configuration.add(new PluginSettingsProperty("key-3"));
        return configuration;
    }

    private PluggableInstanceSettings getPluggableInstanceSettings() {
        final List<PluginConfiguration> pluginConfigurations = Arrays.asList(
                new PluginConfiguration("key-1", new Metadata(false, false)),
                new PluginConfiguration("key-2", new Metadata(false, false)),
                new PluginConfiguration("key-3", new Metadata(false, false))
        );
        return new PluggableInstanceSettings(pluginConfigurations);
    }

    private Plugin getPlugin(String pluginId) {
        return new Plugin().setPluginId(pluginId).setConfiguration(getPluginConfigurationJson().toString());
    }

    private JsonObject getPluginConfigurationJson() {
        final JsonObject configuration = new JsonObject();
        configuration.addProperty("key-1", "v1");
        configuration.addProperty("key-2", "");
        configuration.addProperty("key-3", (String) null);
        return configuration;
    }

}
