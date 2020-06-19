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

import com.thoughtworks.go.config.materials.ScmMaterialConfig;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.ConfigReposConfig;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.config.rules.Allow;
import com.thoughtworks.go.config.rules.Rules;
import com.thoughtworks.go.config.rules.SupportedEntity;
import com.thoughtworks.go.config.update.PartialConfigUpdateCommand;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.server.service.ConfigRepoService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;

import static com.thoughtworks.go.helper.MaterialConfigsMother.git;
import static com.thoughtworks.go.helper.PartialConfigMother.*;
import static java.util.Optional.ofNullable;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class PartialConfigServiceTest {
    File folder = new File("dir");
    private GoConfigWatchList configWatchList;
    private PartialConfigProvider plugin;
    private GoConfigRepoConfigDataSource repoConfigDataSource;
    private BasicCruiseConfig cruiseConfig;
    private PartialConfigService service;
    private ConfigRepoConfig configRepoConfig;
    private CachedGoPartials cachedGoPartials;
    private GoConfigService goConfigService;
    private ServerHealthService serverHealthService;
    private PartialConfigUpdateCommand updateCommand;
    private PartialConfigHelper partialConfigHelper;

    @BeforeEach
    void setUp() {
        ConfigRepoService configRepoService = mock(ConfigRepoService.class);
        serverHealthService = mock(ServerHealthService.class);
        GoConfigPluginService configPluginService = mock(GoConfigPluginService.class);
        partialConfigHelper = mock(PartialConfigHelper.class);
        plugin = mock(PartialConfigProvider.class);
        when(configPluginService.partialConfigProviderFor(any(ConfigRepoConfig.class))).thenReturn(plugin);

        cruiseConfig = new BasicCruiseConfig();
        configRepoConfig = ConfigRepoConfig.createConfigRepoConfig(git("url"), "plugin", "id");
        cruiseConfig.setConfigRepos(new ConfigReposConfig(configRepoConfig));
        CachedGoConfig cachedGoConfig = mock(CachedGoConfig.class);
        when(cachedGoConfig.currentConfig()).thenReturn(cruiseConfig);

        configWatchList = new GoConfigWatchList(cachedGoConfig, mock(GoConfigService.class));
        goConfigService = mock(GoConfigService.class);
        repoConfigDataSource = new GoConfigRepoConfigDataSource(configWatchList, configPluginService, serverHealthService, configRepoService, goConfigService);
        cachedGoPartials = new CachedGoPartials(serverHealthService);
        serverHealthService = mock(ServerHealthService.class);

        updateCommand = null;
        service = new PartialConfigService(repoConfigDataSource, configWatchList, goConfigService, cachedGoPartials, serverHealthService, partialConfigHelper) {
            @Override
            public PartialConfigUpdateCommand buildUpdateCommand(PartialConfig partial, String fingerprint, ConfigRepoConfig repoConfig) {
                if (null == updateCommand) {
                    return super.buildUpdateCommand(partial, fingerprint, configRepoConfig);
                }
                return updateCommand;
            }
        };

        when(configRepoService.findByFingerprint(anyString())).thenReturn(configRepoConfig);
    }

    @Test
    void listensForConfigRepoListChanges() {
        assertTrue(repoConfigDataSource.hasListener(service));
    }

    @Test
    void listensForCompletedParsing() {
        assertTrue(configWatchList.hasListener(service));
    }

    @Test
    void mergeAppliesUpdateToConfig() {
        updateCommand = mock(PartialConfigUpdateCommand.class);
        PartialConfig partial = new PartialConfig();

        service.merge(partial, "finger", cruiseConfig, configRepoConfig);
        verify(updateCommand, times(1)).update(cruiseConfig);
    }

    @Test
    void mergesRemoteGroupToMain() {
        when(goConfigService.updateConfig(any(UpdateConfigCommand.class))).thenAnswer(invocationOnMock -> {
            UpdateConfigCommand command = (UpdateConfigCommand) invocationOnMock.getArguments()[0];
            command.update(cruiseConfig);
            return cruiseConfig;
        });
        service.onSuccessPartialConfig(configRepoConfig, withPipeline("p1"));
        assertEquals(1, cruiseConfig.getPartials().size());
        assertEquals("group", cruiseConfig.getPartials().get(0).getGroups().first().getGroup());
    }

    @Test
    void mergesRemoteEnvironmentToMain() {
        when(goConfigService.updateConfig(any(UpdateConfigCommand.class))).thenAnswer(invocationOnMock -> {
            UpdateConfigCommand command = (UpdateConfigCommand) invocationOnMock.getArguments()[0];
            command.update(cruiseConfig);
            return cruiseConfig;
        });
        service.onSuccessPartialConfig(configRepoConfig, withEnvironment("env1"));
        assertEquals(1, cruiseConfig.getPartials().size());
        assertEquals(new CaseInsensitiveString("env1"), cruiseConfig.getPartials().get(0).getEnvironments().first().name());
    }

    @Test
    void mergesWhenPartialHasChanged() {
        cachedGoPartials = mock(CachedGoPartials.class);
        when(cachedGoPartials.getKnown(any(String.class))).thenReturn(mock(PartialConfig.class));

        configWatchList = mock(GoConfigWatchList.class);
        when(configWatchList.hasConfigRepoWithFingerprint(any(String.class))).thenReturn(true);

        when(partialConfigHelper.isEquivalent(any(PartialConfig.class), any(PartialConfig.class))).thenReturn(false);

        service = new PartialConfigService(repoConfigDataSource, configWatchList, goConfigService, cachedGoPartials, serverHealthService, partialConfigHelper);

        final PartialConfig partial = mock(PartialConfig.class);
        service.onSuccessPartialConfig(configRepoConfig, partial);
        verify(cachedGoPartials).cacheAsLastKnown(configRepoConfig.getRepo().getFingerprint(), partial);
    }

    @Test
    void skipsMergeWhenPartialHasNotChanged() {
        cachedGoPartials = mock(CachedGoPartials.class);
        when(cachedGoPartials.getKnown(any(String.class))).thenReturn(mock(PartialConfig.class));

        configWatchList = mock(GoConfigWatchList.class);
        when(configWatchList.hasConfigRepoWithFingerprint(any(String.class))).thenReturn(true);

        when(partialConfigHelper.isEquivalent(any(PartialConfig.class), any(PartialConfig.class))).thenReturn(true);

        service = new PartialConfigService(repoConfigDataSource, configWatchList, goConfigService, cachedGoPartials, serverHealthService, partialConfigHelper);

        service.onSuccessPartialConfig(configRepoConfig, mock(PartialConfig.class));
        verify(cachedGoPartials, never()).cacheAsLastKnown(any(String.class), any(PartialConfig.class));
    }


    @Test
    void clearsLastValidPartialOnFailureWhenFailsRuleValidations() {
        when(goConfigService.updateConfig(any(UpdateConfigCommand.class))).thenThrow(new RuntimeException("Nope"));
        when(partialConfigHelper.isEquivalent(any(PartialConfig.class), any(PartialConfig.class))).thenReturn(false);

        // an empty set guarantees violations
        configRepoConfig.setRules(new Rules());

        final PartialConfig lastValid = withPipeline("p1", new RepoConfigOrigin(configRepoConfig, "1"));
        final PartialConfig incoming = withPipeline("p1", new RepoConfigOrigin(configRepoConfig, "2"));

        cachedGoPartials.cacheAsLastKnown(configRepoConfig.getRepo().getFingerprint(), lastValid);
        cachedGoPartials.markAllKnownAsValid();

        // baseline
        assertEquals(1, cachedGoPartials.lastValidPartials().size());
        assertEquals(lastValid, cachedGoPartials.lastValidPartials().get(0));
        assertFalse(lastValid.hasErrors());
        assertFalse(incoming.hasErrors());

        service.onSuccessPartialConfig(configRepoConfig, incoming);

        final String violationMessage = "Not allowed to refer to pipeline group 'group'. Check the 'Rules' of this config repository.";

        assertTrue(incoming.hasErrors(), "should have rule violations");
        assertEquals(violationMessage, incoming.errors().on("pipeline_group"));

        assertTrue(lastValid.hasErrors(), "should have rule violations");
        assertEquals(violationMessage, lastValid.errors().on("pipeline_group"));

        assertTrue(cachedGoPartials.lastValidPartials().isEmpty());

        verify(goConfigService).updateConfig(any(UpdateConfigCommand.class));
    }

    @Test
    void keepsLastValidPartialOnFailureWhenRulesAllow() {
        when(goConfigService.updateConfig(any(UpdateConfigCommand.class))).thenThrow(new RuntimeException("Nope"));
        when(partialConfigHelper.isEquivalent(any(PartialConfig.class), any(PartialConfig.class))).thenReturn(false);

        // an empty set guarantees violations
        final Rules rules = new Rules();
        rules.add(new Allow("refer", SupportedEntity.PIPELINE_GROUP.getType(), "two"));
        configRepoConfig.setRules(rules);

        final PartialConfig lastValid = withPipelineInGroup("p1", "two");
        lastValid.setOrigins(new RepoConfigOrigin(configRepoConfig, "1"));

        final PartialConfig incoming = withPipelineInGroup("p1", "one");
        incoming.setOrigins(new RepoConfigOrigin(configRepoConfig, "2"));

        cachedGoPartials.cacheAsLastKnown(configRepoConfig.getRepo().getFingerprint(), lastValid);
        cachedGoPartials.markAllKnownAsValid();

        // baseline
        assertEquals(1, cachedGoPartials.lastValidPartials().size());
        assertEquals(lastValid, cachedGoPartials.lastValidPartials().get(0));
        assertFalse(lastValid.hasErrors());
        assertFalse(incoming.hasErrors());

        service.onSuccessPartialConfig(configRepoConfig, incoming);

        final String violationMessage = "Not allowed to refer to pipeline group 'one'. Check the 'Rules' of this config repository.";

        assertTrue(incoming.hasErrors(), "should have rule violations");
        assertEquals(violationMessage, incoming.errors().on("pipeline_group"));

        assertFalse(lastValid.hasErrors(), "should not have rule violations");

        assertEquals(1, cachedGoPartials.lastValidPartials().size());
        assertEquals(lastValid, cachedGoPartials.lastValidPartials().get(0));

        verify(goConfigService).updateConfig(any(UpdateConfigCommand.class));
    }

    @Nested
    class WatchList {
        private ScmMaterialConfig material;

        @BeforeEach
        void setup() {
            material = (ScmMaterialConfig) configRepoConfig.getRepo();
        }

        @Test
        void removesCachedPartialsWhenWatchListIsEmpty() {
            final PartialConfig p1 = withPipeline("p1", new RepoConfigOrigin(configRepoConfig, ""));

            cachedGoPartials.cacheAsLastKnown(material.getFingerprint(), p1);
            cachedGoPartials.markAllKnownAsValid();

            assertEquals(1, cachedGoPartials.lastKnownPartials().size());
            assertEquals(1, cachedGoPartials.lastValidPartials().size());

            notifyWatchList();

            assertTrue(cachedGoPartials.lastKnownPartials().isEmpty());
            assertTrue(cachedGoPartials.lastValidPartials().isEmpty());
        }

        @Test
        void cachedPartialsRemainEmptyWhenConfigReposNotParsedYetButWatchListIsPopulated() {
            assertTrue(cachedGoPartials.lastKnownPartials().isEmpty());
            assertTrue(cachedGoPartials.lastValidPartials().isEmpty());

            notifyWatchList(configRepoConfig);

            assertTrue(cachedGoPartials.lastKnownPartials().isEmpty());
            assertTrue(cachedGoPartials.lastValidPartials().isEmpty());
        }

        @Test
        void cachesLatestPartialAfterCheckoutWhileWatchListIsPopulated() {
            notifyWatchList(configRepoConfig);
            ScmMaterialConfig material = (ScmMaterialConfig) configRepoConfig.getRepo();

            PartialConfig part = stubConfigRepoParseWith(new PartialConfig());

            repoConfigDataSource.onCheckoutComplete(material, folder, mock(Modification.class));

            assertEquals(1, cachedGoPartials.lastValidPartials().size());
            assertEquals(part, cachedGoPartials.lastValidPartials().get(0));
        }

        @Test
        void neverCachesValidPartialWhenFirstParsingFailed() {
            notifyWatchList(configRepoConfig);
            assertTrue(cachedGoPartials.lastValidPartials().isEmpty());

            stubConfigRepoParseFailure(new RuntimeException("Failed parsing"));

            repoConfigDataSource.onCheckoutComplete(material, folder, mock(Modification.class));

            assertFalse(isLastParseResultSuccessful());
            assertTrue(cachedGoPartials.lastValidPartials().isEmpty());
        }

        @Test
        void newPartialIsNotPromotedToValidAfterFailureAndLastValidPartialRemainsUnchanged() {
            notifyWatchList(configRepoConfig);

            PartialConfig part = stubConfigRepoParseWith(new PartialConfig());

            repoConfigDataSource.onCheckoutComplete(material, folder, mock(Modification.class));

            stubConfigRepoParseFailure(new RuntimeException("Failed parsing"));
            repoConfigDataSource.onCheckoutComplete(material, folder, mock(Modification.class));

            assertFalse(isLastParseResultSuccessful());
            assertEquals(1, cachedGoPartials.lastValidPartials().size());
            assertEquals(part, cachedGoPartials.lastValidPartials().get(0));
        }

        @Test
        void removesPartialFromCachesWhenNoLongerInWatchList() {
            notifyWatchList(configRepoConfig);

            PartialConfig part = stubConfigRepoParseWith(new PartialConfig());

            repoConfigDataSource.onCheckoutComplete(material, folder, mock(Modification.class));

            assertEquals(1, cachedGoPartials.lastValidPartials().size());
            assertEquals(part, cachedGoPartials.lastValidPartials().get(0));

            notifyWatchList(mock(ConfigRepoConfig.class));

            assertTrue(cachedGoPartials.lastValidPartials().isEmpty());
        }

        @Test
        void notifiesListenersAfterUpdatingMapOfLatestValidConfig() {
            notifyWatchList(configRepoConfig);
            ScmMaterialConfig material = (ScmMaterialConfig) configRepoConfig.getRepo();

            stubConfigRepoParseWith(new PartialConfig());

            PartialConfigUpdateCompletedListener listener = mock(PartialConfigUpdateCompletedListener.class);
            repoConfigDataSource.registerListener(listener);

            repoConfigDataSource.onCheckoutComplete(material, folder, mock(Modification.class));
            verify(listener).onSuccessPartialConfig(any(ConfigRepoConfig.class), any(PartialConfig.class));
        }

        private void notifyWatchList(ConfigRepoConfig... repos) {
            cruiseConfig.setConfigRepos(new ConfigReposConfig(repos));
            configWatchList.onConfigChange(cruiseConfig);
        }

        private boolean isLastParseResultSuccessful() {
            return ofNullable(repoConfigDataSource.getLastParseResult(material)).
                    map(PartialConfigParseResult::isSuccessful).
                    orElse(false);
        }

        private PartialConfig stubConfigRepoParseWith(PartialConfig part) {
            when(plugin.load(any(File.class), any(PartialConfigLoadContext.class))).thenReturn(part);
            return part;
        }

        private void stubConfigRepoParseFailure(Throwable e) {
            when(plugin.load(any(File.class), any(PartialConfigLoadContext.class))).thenThrow(e);
        }
    }
}
