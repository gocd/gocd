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
import static org.assertj.core.api.Assertions.assertThat;
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
        ConfigRepoConfig configRepoConfig = new ConfigRepoConfig(material, "myplugin");
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

        assertThat(result).isNotNull();
        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.getLatestParsedModification()).isNotNull();
        assertThat(modification).isEqualTo(result.getLatestParsedModification());

        manager.markFailedResultsForReparse();
        assertThat(result.getLatestParsedModification()).isNull();
        verify(serverHealthService, times(1)).removeByScope(scope);
    }

    @Test
    void shouldAddResultForAConfigRepoMaterialUponSuccessfulParse() {
        String fingerprint = "repo1";


        Modification modification = modificationFor("rev1");
        PartialConfig partialConfig = new PartialConfig();
        manager.parseSuccess(fingerprint, modification, partialConfig);

        PartialConfigParseResult expectedParseResult = PartialConfigParseResult.parseSuccess(modification, partialConfig);

        assertThat(manager.get(fingerprint)).isEqualTo(expectedParseResult);
        assertThat(manager.get(fingerprint).isSuccessful()).isTrue();
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

        assertThat(manager.get(fingerprint).isSuccessful()).isFalse();
        assertThat(manager.get(fingerprint).getLastFailure().getMessage()).isEqualTo(expectedBeautifiedMessage);
    }

    @Test
    void shouldClearGoodModificationInCaseGoodModificationIsSameAsOfLastParsedModificationAsHealthServiceHasErrors() {
        ServerHealthState serverHealthState = ServerHealthState.error("Error Happened!", "Boom!", HealthStateType.general(HealthStateScope.GLOBAL));
        when(serverHealthService.filterByScope(any())).thenReturn(Arrays.asList(serverHealthState));
        String fingerprint = "repo1";


        Modification modification = modificationFor("rev1");
        PartialConfig partialConfig = new PartialConfig();
        manager.parseSuccess(fingerprint, modification, partialConfig);

        assertThat(manager.get(fingerprint).isSuccessful()).isFalse();
        assertThat(manager.get(fingerprint).getGoodModification()).isNull();
        assertThat(manager.get(fingerprint).lastGoodPartialConfig()).isNull();
    }

    @Test
    void shouldAddAnErrorFromHealthStateWhenNoResultExistsForTheConfigRepo() {
        ServerHealthState serverHealthState = ServerHealthState.error("Error Happened!", "Boom!", HealthStateType.general(HealthStateScope.GLOBAL));
        when(serverHealthService.filterByScope(any())).thenReturn(Arrays.asList(serverHealthState));
        String fingerprint = "repo1";


        String expectedBeautifiedMessage = String.format("%s\n%s", serverHealthState.getMessage().toUpperCase(), serverHealthState.getDescription());

        assertThat(manager.get(fingerprint).isSuccessful()).isFalse();
        assertThat(manager.get(fingerprint).getLastFailure().getMessage()).isEqualTo(expectedBeautifiedMessage);
    }

    @Test
    void shouldAddAnErrorFromHealthStateWhenNoResultExistsForTheConfigRepoWithoutAddingModification() {
        ServerHealthState serverHealthState = ServerHealthState.error("Error Happened!", "Boom!", HealthStateType.general(HealthStateScope.GLOBAL));
        when(serverHealthService.filterByScope(any())).thenReturn(Arrays.asList(serverHealthState));
        String fingerprint = "repo1";


        assertThat(manager.get(fingerprint).isSuccessful()).isFalse();
        assertThat(manager.get(fingerprint).getGoodModification()).isNull();
        assertThat(manager.get(fingerprint).getLatestParsedModification()).isNull();
        assertThat(manager.get(fingerprint).lastGoodPartialConfig()).isNull();
    }

    @Test
    void shouldAddResultForAConfigRepoMaterialUponUnsuccessfulParse() {
        String fingerprint = "repo1";


        Modification modification = modificationFor("rev1");
        Exception exception = new Exception("Boom!");
        manager.parseFailed(fingerprint, modification, exception);

        PartialConfigParseResult expectedParseResult = PartialConfigParseResult.parseFailed(modification, exception);

        assertThat(manager.get(fingerprint)).isEqualTo(expectedParseResult);
        assertThat(manager.get(fingerprint).isSuccessful()).isFalse();
    }

    @Test
    void shouldReturnNullIfManagerDoesNotContainResultForProvidedConfigRepoMaterialFingerprint() {
        String fingerprint = "repo1";


        assertThat(manager.get(fingerprint)).isNull();
    }

    @Test
    void shouldUpdateTheResultForAConfigRepoMaterialIfResultAlreadyExists_WhenMaterialParseFails() {
        String fingerprint = "repo1";


        Modification modification = modificationFor("rev1");
        PartialConfig partialConfig = new PartialConfig();
        manager.parseSuccess(fingerprint, modification, partialConfig);

        PartialConfigParseResult expectedParseResult = PartialConfigParseResult.parseSuccess(modification, partialConfig);

        assertThat(manager.get(fingerprint)).isEqualTo(expectedParseResult);
        assertThat(manager.get(fingerprint).isSuccessful()).isTrue();

        Modification modification2 = modificationFor("rev2");
        Exception exception = new Exception("Boom!");
        manager.parseFailed(fingerprint, modification2, exception);


        PartialConfigParseResult parseResult = manager.get(fingerprint);

        assertThat(parseResult.isSuccessful()).isFalse();
        assertThat(parseResult.getLatestParsedModification()).isEqualTo(modification2);
        assertThat(parseResult.getGoodModification()).isEqualTo(modification);
        assertThat(parseResult.lastGoodPartialConfig()).isEqualTo(partialConfig);
        assertThat(parseResult.getLastFailure()).isEqualTo(exception);
    }

    @Test
    void shouldUpdateTheResultForAConfigRepoMaterialIfResultAlreadyExists_WhenMaterialParseIsFixed() {
        String fingerprint = "repo1";


        Modification modification = modificationFor("rev1");
        Exception exception = new Exception("Boom!");
        manager.parseFailed(fingerprint, modification, exception);

        PartialConfigParseResult expectedParseResult = PartialConfigParseResult.parseFailed(modification, exception);

        assertThat(manager.get(fingerprint)).isEqualTo(expectedParseResult);
        assertThat(manager.get(fingerprint).isSuccessful()).isFalse();

        Modification modification2 = modificationFor("rev2");
        PartialConfig partialConfig = new PartialConfig();
        manager.parseSuccess(fingerprint, modification2, partialConfig);

        PartialConfigParseResult parseResult = manager.get(fingerprint);

        assertThat(parseResult.isSuccessful()).isTrue();
        assertThat(parseResult.getLatestParsedModification()).isEqualTo(modification2);
        assertThat(parseResult.getGoodModification()).isEqualTo(modification2);
        assertThat(parseResult.lastGoodPartialConfig()).isEqualTo(partialConfig);
        assertThat(parseResult.getLastFailure()).isNull();
    }

    @Test
    void shouldAddEntityConfigChangedListeners() {
        final GoConfigService goConfigService = mock(GoConfigService.class);

        manager.attachConfigUpdateListeners(goConfigService);

        final ArgumentCaptor<ConfigChangedListener> listenerArgumentCaptor = ArgumentCaptor.forClass(ConfigChangedListener.class);
        verify(goConfigService).register(listenerArgumentCaptor.capture());
        assertThat(listenerArgumentCaptor.getValue()).isInstanceOf(ConfigRepoReparseListener.class);
    }

    @Nested
    class ConfigRepoReparseListenerTest {
        @ParameterizedTest
        @ValueSource(classes = {PipelineConfig.class, EnvironmentConfig.class, PipelineTemplateConfig.class, SCM.class, ConfigRepoConfig.class, ElasticProfile.class})
        void shouldCareAboutSpecifiedConfigClasses(Class<? extends Validatable> configClassToCareAbout) {
            final ConfigReposMaterialParseResultManager manager = mock(ConfigReposMaterialParseResultManager.class);
            final ConfigRepoReparseListener configRepoReparseListener = new ConfigRepoReparseListener(manager);

            assertThat(configRepoReparseListener.shouldCareAbout(mock(configClassToCareAbout))).isTrue();
        }

        @ParameterizedTest
        @ValueSource(classes = {Role.class, SecurityAuthConfig.class, Agent.class})
        void shouldNotCareAboutOtherConfigEntities(Class<? extends Validatable> configClassToCareAbout) {
            final ConfigReposMaterialParseResultManager manager = mock(ConfigReposMaterialParseResultManager.class);
            final ConfigRepoReparseListener configRepoReparseListener = new ConfigRepoReparseListener(manager);

            assertThat(configRepoReparseListener.shouldCareAbout(mock(configClassToCareAbout))).isFalse();
        }
    }

    private Modification modificationFor(String revision) {
        return ModificationsMother.oneModifiedFile(revision);
    }
}
