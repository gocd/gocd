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
package com.thoughtworks.go.config;

import com.thoughtworks.go.config.elastic.ElasticProfile;
import com.thoughtworks.go.config.materials.ScmMaterialConfig;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.scm.SCM;
import com.thoughtworks.go.helper.ModificationsMother;
import com.thoughtworks.go.listener.ConfigChangedListener;
import com.thoughtworks.go.server.service.ConfigRepoService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;

import static com.thoughtworks.go.config.ConfigReposMaterialParseResultManager.ConfigRepoReparseListener;
import static com.thoughtworks.go.helper.MaterialConfigsMother.git;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

class ConfigReposMaterialParseResultManagerTest {
    @Mock
    ServerHealthService serverHealthService;

    @Mock
    ConfigRepoService configRepoService;
    private ConfigReposMaterialParseResultManager manager;

    @BeforeEach
    void setUp() {
        initMocks(this);

        when(serverHealthService.filterByScope(any())).thenReturn(Collections.emptyList());
        ScmMaterialConfig material = git("http://my.git");
        ConfigRepoConfig configRepoConfig = ConfigRepoConfig.createConfigRepoConfig(material, "myplugin", "id");
        when(configRepoService.findByFingerprint(anyString())).thenReturn(configRepoConfig);

        manager = new ConfigReposMaterialParseResultManager(serverHealthService, configRepoService);
    }

    @Test
    void markForReparseShouldClearLastModificationAndHealthErrors() {
        String fingerprint = "repo1";
        HealthStateScope scope = HealthStateScope.forPartialConfigRepo(fingerprint);
        ServerHealthState error = ServerHealthState.error("boom", "bang!", HealthStateType.general(scope));

        when(serverHealthService.filterByScope(scope)).thenReturn(Collections.singletonList(error));

        Modification modification = modificationFor("rev1");
        manager.parseFailed(fingerprint, modification, new Exception());

        PartialConfigParseResult result = manager.get(fingerprint);

        assertNotNull(result);
        assertFalse(result.isSuccessful());
        assertNotNull(result.getLatestParsedModification());
        assertEquals(result.getLatestParsedModification(), modification);

        manager.markFailedResultsForReparse();
        assertNull(result.getLatestParsedModification());
        verify(serverHealthService, never()).removeByScope(scope);
    }

    @Test
    void shouldAddResultForAConfigRepoMaterialUponSuccessfulParse() {
        String fingerprint = "repo1";


        Modification modification = modificationFor("rev1");
        PartialConfig partialConfig = new PartialConfig();
        manager.parseSuccess(fingerprint, modification, partialConfig);

        PartialConfigParseResult expectedParseResult = PartialConfigParseResult.parseSuccess(modification, partialConfig);

        assertEquals(expectedParseResult, manager.get(fingerprint));
        assertTrue(manager.get(fingerprint).isSuccessful());
    }

    @Test
    void shouldCheckForErrorsRelatedToConfigRepoMaterialWithtinServerHealthScope() {
        ServerHealthState serverHealthState = ServerHealthState.error("Error Happened!", "Boom!", HealthStateType.general(HealthStateScope.GLOBAL));
        when(serverHealthService.filterByScope(any())).thenReturn(Arrays.asList(serverHealthState));
        String fingerprint = "repo1";


        Modification modification = modificationFor("rev1");
        PartialConfig partialConfig = new PartialConfig();
        manager.parseSuccess(fingerprint, modification, partialConfig);

        String expectedBeautifiedMessage = String.format("%s\n%s", serverHealthState.getMessage().toUpperCase(), serverHealthState.getDescription());

        assertFalse(manager.get(fingerprint).isSuccessful());
        assertEquals(expectedBeautifiedMessage, manager.get(fingerprint).getLastFailure().getMessage());
    }

    @Test
    void shouldClearGoodModificationInCaseGoodModificationIsSameAsOfLastParsedModificationAsHealthServiceHasErrors() {
        ServerHealthState serverHealthState = ServerHealthState.error("Error Happened!", "Boom!", HealthStateType.general(HealthStateScope.GLOBAL));
        when(serverHealthService.filterByScope(any())).thenReturn(Arrays.asList(serverHealthState));
        String fingerprint = "repo1";


        Modification modification = modificationFor("rev1");
        PartialConfig partialConfig = new PartialConfig();
        manager.parseSuccess(fingerprint, modification, partialConfig);

        assertFalse(manager.get(fingerprint).isSuccessful());
        assertNull(manager.get(fingerprint).getGoodModification());
        assertNull(manager.get(fingerprint).lastGoodPartialConfig());
    }

    @Test
    void shouldAddAnErrorFromHealthStateWhenNoResultExistsForTheConfigRepo() {
        ServerHealthState serverHealthState = ServerHealthState.error("Error Happened!", "Boom!", HealthStateType.general(HealthStateScope.GLOBAL));
        when(serverHealthService.filterByScope(any())).thenReturn(Arrays.asList(serverHealthState));
        String fingerprint = "repo1";

        String expectedBeautifiedMessage = String.format("%s\n%s", serverHealthState.getMessage().toUpperCase(), serverHealthState.getDescription());

        assertFalse(manager.get(fingerprint).isSuccessful());
        assertEquals(expectedBeautifiedMessage, manager.get(fingerprint).getLastFailure().getMessage());
    }

    @Test
    void shouldAddAnErrorFromHealthStateWhenNoResultExistsForTheConfigRepoWithoutAddingModification() {
        ServerHealthState serverHealthState = ServerHealthState.error("Error Happened!", "Boom!", HealthStateType.general(HealthStateScope.GLOBAL));
        when(serverHealthService.filterByScope(any())).thenReturn(Arrays.asList(serverHealthState));
        String fingerprint = "repo1";

        assertFalse(manager.get(fingerprint).isSuccessful());
        assertNull(manager.get(fingerprint).getGoodModification());
        assertNull(manager.get(fingerprint).getLatestParsedModification());
        assertNull(manager.get(fingerprint).lastGoodPartialConfig());
    }

    @Test
    void shouldAddResultForAConfigRepoMaterialUponUnsuccessfulParse() {
        String fingerprint = "repo1";


        Modification modification = modificationFor("rev1");
        Exception exception = new Exception("Boom!");
        manager.parseFailed(fingerprint, modification, exception);

        PartialConfigParseResult expectedParseResult = PartialConfigParseResult.parseFailed(modification, exception);

        assertEquals(expectedParseResult, manager.get(fingerprint));
        assertFalse(manager.get(fingerprint).isSuccessful());
    }

    @Test
    void shouldReturnNullIfManagerDoesNotContainResultForProvidedConfigRepoMaterialFingerprint() {
        String fingerprint = "repo1";

        assertNull(manager.get(fingerprint));
    }

    @Test
    void shouldUpdateTheResultForAConfigRepoMaterialIfResultAlreadyExists_WhenMaterialParseFails() {
        String fingerprint = "repo1";

        Modification modification = modificationFor("rev1");
        PartialConfig partialConfig = new PartialConfig();
        manager.parseSuccess(fingerprint, modification, partialConfig);

        PartialConfigParseResult expectedParseResult = PartialConfigParseResult.parseSuccess(modification, partialConfig);

        assertEquals(expectedParseResult, manager.get(fingerprint));
        assertTrue(manager.get(fingerprint).isSuccessful());

        Modification modification2 = modificationFor("rev2");
        Exception exception = new Exception("Boom!");
        manager.parseFailed(fingerprint, modification2, exception);

        PartialConfigParseResult parseResult = manager.get(fingerprint);

        assertFalse(parseResult.isSuccessful());
        assertEquals(modification2, parseResult.getLatestParsedModification());
        assertEquals(modification, parseResult.getGoodModification());
        assertEquals(partialConfig, parseResult.lastGoodPartialConfig());
        assertEquals(exception, parseResult.getLastFailure());
    }

    @Test
    void shouldUpdateTheResultForAConfigRepoMaterialIfResultAlreadyExists_WhenMaterialParseIsFixed() {
        String fingerprint = "repo1";

        Modification modification = modificationFor("rev1");
        Exception exception = new Exception("Boom!");
        manager.parseFailed(fingerprint, modification, exception);

        PartialConfigParseResult expectedParseResult = PartialConfigParseResult.parseFailed(modification, exception);

        assertEquals(expectedParseResult, manager.get(fingerprint));
        assertFalse(manager.get(fingerprint).isSuccessful());

        Modification modification2 = modificationFor("rev2");
        PartialConfig partialConfig = new PartialConfig();
        manager.parseSuccess(fingerprint, modification2, partialConfig);

        PartialConfigParseResult parseResult = manager.get(fingerprint);

        assertTrue(parseResult.isSuccessful());
        assertEquals(modification2, parseResult.getLatestParsedModification());
        assertEquals(modification2, parseResult.getGoodModification());
        assertEquals(partialConfig, parseResult.lastGoodPartialConfig());
        assertNull(parseResult.getLastFailure());
    }

    @Test
    void shouldAddEntityConfigChangedListeners() {
        final GoConfigService goConfigService = mock(GoConfigService.class);

        manager.attachConfigUpdateListeners(goConfigService);

        final ArgumentCaptor<ConfigChangedListener> listenerArgumentCaptor = ArgumentCaptor.forClass(ConfigChangedListener.class);
        verify(goConfigService).register(listenerArgumentCaptor.capture());
        assertTrue(listenerArgumentCaptor.getValue() instanceof ConfigRepoReparseListener);
    }

    @Nested
    class ConfigRepoReparseListenerTest {
        @ParameterizedTest
        @ValueSource(classes = {PipelineConfig.class, EnvironmentConfig.class, PipelineTemplateConfig.class, SCM.class, ConfigRepoConfig.class, ElasticProfile.class})
        void shouldCareAboutSpecifiedConfigClasses(Class<? extends Validatable> configClassToCareAbout) {
            final ConfigReposMaterialParseResultManager manager = mock(ConfigReposMaterialParseResultManager.class);
            final ConfigRepoReparseListener configRepoReparseListener = new ConfigRepoReparseListener(manager);

            assertTrue(configRepoReparseListener.shouldCareAbout(mock(configClassToCareAbout)));
        }

        @ParameterizedTest
        @ValueSource(classes = {Role.class, SecurityAuthConfig.class, Agent.class})
        void shouldNotCareAboutOtherConfigEntities(Class<? extends Validatable> configClassToCareAbout) {
            final ConfigReposMaterialParseResultManager manager = mock(ConfigReposMaterialParseResultManager.class);
            final ConfigRepoReparseListener configRepoReparseListener = new ConfigRepoReparseListener(manager);

            assertFalse(configRepoReparseListener.shouldCareAbout(mock(configClassToCareAbout)));
        }
    }

    private Modification modificationFor(String revision) {
        return ModificationsMother.oneModifiedFile(revision);
    }
}
