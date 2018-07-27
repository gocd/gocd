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

package com.thoughtworks.go.config;

import com.thoughtworks.go.config.commands.EntityConfigUpdateCommand;
import com.thoughtworks.go.config.update.FullConfigUpdateCommand;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.PartialConfigMother;
import com.thoughtworks.go.listener.ConfigChangedListener;
import com.thoughtworks.go.listener.EntityConfigChangedListener;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import com.thoughtworks.go.util.ReflectionUtil;
import com.thoughtworks.go.util.SystemEnvironment;
import org.hamcrest.core.Is;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class CachedGoConfigTest {
    @Mock
    private CachedGoConfig cachedGoConfig;
    @Mock
    private GoFileConfigDataSource dataSource;
    private GoConfigHolder configHolder;
    @Mock
    private ServerHealthService serverHealthService;
    @Mock
    private GoConfigMigrator goConfigMigrator;
    @Mock
    private SystemEnvironment systemEnvironment;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        BasicCruiseConfig config = new BasicCruiseConfig();
        BasicCruiseConfig configForEdit = new BasicCruiseConfig();
        ReflectionUtil.setField(config, "md5", "md5");
        ReflectionUtil.setField(configForEdit, "md5", "md5");
        configHolder = new GoConfigHolder(config, configForEdit);
        cachedGoConfig = new CachedGoConfig(serverHealthService, dataSource, mock(CachedGoPartials.class), goConfigMigrator, systemEnvironment);
        when(systemEnvironment.optimizeFullConfigSave()).thenReturn(true);
        when(dataSource.load()).thenReturn(configHolder);
    }

    @Test
    public void shouldDelegateWriteEntityConfigCallToDataSource() {
        EntityConfigUpdateCommand saveCommand = mock(EntityConfigUpdateCommand.class);
        GoConfigHolder savedConfig = new GoConfigHolder(new BasicCruiseConfig(), new BasicCruiseConfig());
        GoConfigHolder holderBeforeUpdate = cachedGoConfig.loadConfigHolder();
        Username user = new Username(new CaseInsensitiveString("user"));
        EntityConfigSaveResult entityConfigSaveResult = mock(EntityConfigSaveResult.class);
        when(entityConfigSaveResult.getConfigHolder()).thenReturn(savedConfig);
        when(entityConfigSaveResult.getEntityConfig()).thenReturn(new PipelineConfig());
        when(dataSource.writeEntityWithLock(saveCommand, holderBeforeUpdate, user)).thenReturn(entityConfigSaveResult);

        cachedGoConfig.writeEntityWithLock(saveCommand, user);
        assertThat(cachedGoConfig.loadConfigHolder(), is(savedConfig));
        assertThat(cachedGoConfig.currentConfig(), is(savedConfig.config));
        assertThat(cachedGoConfig.loadForEditing(), is(savedConfig.getConfigForEdit()));
        verify(dataSource).writeEntityWithLock(saveCommand, holderBeforeUpdate, user);
    }

    @Test
    public void shouldLoadConfigHolderIfNotAvailable() throws Exception {
        cachedGoConfig.forceReload();
        Assert.assertThat(cachedGoConfig.loadConfigHolder(), is(configHolder));
    }

    @Test
    public void shouldNotNotifyWhenConfigIsNullDuringRegistration() throws Exception {
        final ConfigChangedListener listener = mock(ConfigChangedListener.class);
        cachedGoConfig.registerListener(listener);
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void shouldNotifyConcernedListenersWhenEntityChanges() {
        final boolean[] pipelineConfigChangeListenerCalled = {false};
        final boolean[] agentConfigChangeListenerCalled = {false};
        final boolean[] cruiseConfigChangeListenerCalled = {false};
        EntityConfigChangedListener<PipelineConfig> pipelineConfigChangeListener = new EntityConfigChangedListener<PipelineConfig>() {
            @Override
            public void onEntityConfigChange(PipelineConfig entity) {
                pipelineConfigChangeListenerCalled[0] = true;
            }
        };
        EntityConfigChangedListener<AgentConfig> agentConfigChangeListener = new EntityConfigChangedListener<AgentConfig>() {
            @Override
            public void onEntityConfigChange(AgentConfig entity) {
                agentConfigChangeListenerCalled[0] = true;
            }
        };
        EntityConfigChangedListener<CruiseConfig> cruiseConfigChangeListener = new EntityConfigChangedListener<CruiseConfig>() {
            @Override
            public void onEntityConfigChange(CruiseConfig entity) {
                cruiseConfigChangeListenerCalled[0] = true;
            }
        };
        cachedGoConfig.registerListener(pipelineConfigChangeListener);
        cachedGoConfig.registerListener(agentConfigChangeListener);
        cachedGoConfig.registerListener(cruiseConfigChangeListener);

        EntityConfigUpdateCommand configCommand = mock(EntityConfigUpdateCommand.class);
        when(configCommand.isValid(any(CruiseConfig.class))).thenReturn(true);
        when(configCommand.getPreprocessedEntityConfig()).thenReturn(mock(PipelineConfig.class));
        EntityConfigSaveResult entityConfigSaveResult = mock(EntityConfigSaveResult.class);
        when(entityConfigSaveResult.getConfigHolder()).thenReturn(configHolder);
        when(entityConfigSaveResult.getEntityConfig()).thenReturn(new PipelineConfig());
        Username user = new Username(new CaseInsensitiveString("user"));
        when(dataSource.writeEntityWithLock(configCommand, configHolder, user)).thenReturn(entityConfigSaveResult);

        cachedGoConfig.loadConfigIfNull();

        cachedGoConfig.writeEntityWithLock(configCommand, user);
        assertThat(pipelineConfigChangeListenerCalled[0], is(true));
        assertThat(agentConfigChangeListenerCalled[0], is(false));
        assertThat(cruiseConfigChangeListenerCalled[0], is(false));
    }

    @Test
    public void shouldWriteFullConfigWithLock() {
        FullConfigUpdateCommand fullConfigUpdateCommand = mock(FullConfigUpdateCommand.class);
        when(dataSource.writeFullConfigWithLock(any(FullConfigUpdateCommand.class), any(GoConfigHolder.class))).thenReturn(new GoFileConfigDataSource.GoConfigSaveResult(null, null));

        cachedGoConfig.forceReload();
        cachedGoConfig.writeFullConfigWithLock(fullConfigUpdateCommand);

        verify(dataSource).writeFullConfigWithLock(fullConfigUpdateCommand, cachedGoConfig.loadConfigHolder());
    }

    @Test
    public void shouldUpdateCachesPostWriteFullConfigWithLock() {
        BasicCruiseConfig config = mock(BasicCruiseConfig.class);
        BasicCruiseConfig configForEdit = mock(BasicCruiseConfig.class);
        BasicCruiseConfig mergedConfigForEdit = mock(BasicCruiseConfig.class);
        when(configForEdit.getMd5()).thenReturn("md5-for-config-for-edit");
        GoConfigHolder goConfigHolder = new GoConfigHolder(config, configForEdit);
        goConfigHolder.setMergedConfigForEdit(mergedConfigForEdit, Arrays.asList(PartialConfigMother.withPipeline("pipeline")));
        ConfigSaveState configSaveState = ConfigSaveState.UPDATED;

        when(dataSource.writeFullConfigWithLock(any(FullConfigUpdateCommand.class), any(GoConfigHolder.class)))
                .thenReturn(new GoFileConfigDataSource.GoConfigSaveResult(goConfigHolder, configSaveState));

        cachedGoConfig.forceReload();
        ConfigSaveState saveState = cachedGoConfig.writeFullConfigWithLock(mock(FullConfigUpdateCommand.class));

        assertThat(saveState, is(configSaveState));
        assertThat(cachedGoConfig.currentConfig(), Is.<CruiseConfig>is(config));
        assertThat(cachedGoConfig.loadForEditing(), Is.<CruiseConfig>is(configForEdit));
        assertThat(cachedGoConfig.loadConfigHolder(), is(goConfigHolder));
        assertThat(cachedGoConfig.loadMergedForEditing(), Is.<CruiseConfig>is(mergedConfigForEdit));
        assertThat(cachedGoConfig.loadConfigHolder().getChecksum().md5SumOfConfigForEdit, is("md5-for-config-for-edit"));
        assertThat(cachedGoConfig.loadConfigHolder().getChecksum().md5SumOfPartials, is(not(nullValue())));
        verify(serverHealthService, times(2)).update(any(ServerHealthState.class));
    }

    @Test
    public void shouldUpgradeConfigFile() throws Exception {
        cachedGoConfig.upgradeConfig();

        verify(goConfigMigrator).migrate();
    }

    @Test
    public void shouldUpdateCachesPostConfigUpgade() throws Exception {
        BasicCruiseConfig config = mock(BasicCruiseConfig.class);
        BasicCruiseConfig configForEdit = mock(BasicCruiseConfig.class);
        BasicCruiseConfig mergedConfigForEdit = mock(BasicCruiseConfig.class);
        GoConfigHolder goConfigHolder = new GoConfigHolder(config, configForEdit);
        goConfigHolder.setMergedConfigForEdit(mergedConfigForEdit, null);

        when(goConfigMigrator.migrate()).thenReturn(goConfigHolder);

        cachedGoConfig.upgradeConfig();

        assertThat(cachedGoConfig.currentConfig(), Is.<CruiseConfig>is(config));
        assertThat(cachedGoConfig.loadForEditing(), Is.<CruiseConfig>is(configForEdit));
        assertThat(cachedGoConfig.loadConfigHolder(), is(goConfigHolder));
        assertThat(cachedGoConfig.loadMergedForEditing(), Is.<CruiseConfig>is(mergedConfigForEdit));
        verify(serverHealthService).update(any(ServerHealthState.class));
    }

    @Test
    public void shouldFallbackToOldConfigUpgradeIfNewFlowIsDisabled() throws Exception {
        when(systemEnvironment.optimizeFullConfigSave()).thenReturn(false);

        cachedGoConfig.upgradeConfig();

        verify(dataSource).upgradeIfNecessary();
    }

    @Test
    public void shouldNotNotifyListenersWhenConfigChecksumHasNotChanged() throws Exception {
        ConfigChangedListenerThatTracksInvocationCount listener = new ConfigChangedListenerThatTracksInvocationCount();
        cachedGoConfig.registerListener(listener);
        cachedGoConfig.forceReload();
        assertThat(listener.invocationCount, is(1));

        BasicCruiseConfig config = GoConfigMother.defaultCruiseConfig();
        ReflectionUtil.setField(config, "md5", "original-md5");
        when(dataSource.writeFullConfigWithLock(any(FullConfigUpdateCommand.class), any(GoConfigHolder.class)))
                .thenReturn(new GoFileConfigDataSource.GoConfigSaveResult(
                        new GoConfigHolder(config, config), ConfigSaveState.UPDATED));

        cachedGoConfig.writeFullConfigWithLock(new FullConfigUpdateCommand(config, config.getMd5()));
        assertThat(listener.invocationCount, is(2));

        when(dataSource.load()).thenReturn(new GoConfigHolder(config, config));
        cachedGoConfig.forceReload();
        assertThat(listener.invocationCount, is(2));

        cachedGoConfig.writeFullConfigWithLock(new FullConfigUpdateCommand(config, config.getMd5()));
        assertThat(listener.invocationCount, is(2));
    }

    @Test
    public void shouldNotifyListenersWhenConfigChecksumHasChanged() throws Exception {
        ConfigChangedListenerThatTracksInvocationCount listener = new ConfigChangedListenerThatTracksInvocationCount();
        cachedGoConfig.registerListener(listener);
        BasicCruiseConfig config = GoConfigMother.defaultCruiseConfig();
        ReflectionUtil.setField(config, "md5", "original-md5");

        when(dataSource.load()).thenReturn(new GoConfigHolder(config, config));
        cachedGoConfig.forceReload();
        assertThat(listener.invocationCount, is(1));

        CruiseConfig updatedConfig = GoConfigMother.simpleDiamond();
        ReflectionUtil.setField(updatedConfig, "md5", "updated-md5");
        when(dataSource.writeFullConfigWithLock(any(FullConfigUpdateCommand.class), any(GoConfigHolder.class)))
                .thenReturn(new GoFileConfigDataSource.GoConfigSaveResult(
                        new GoConfigHolder(updatedConfig, updatedConfig), ConfigSaveState.UPDATED));

        cachedGoConfig.writeFullConfigWithLock(new FullConfigUpdateCommand(updatedConfig, updatedConfig.getMd5()));
        assertThat(listener.invocationCount, is(2));

        updatedConfig = GoConfigMother.simpleDiamond();
        ReflectionUtil.setField(updatedConfig, "md5", "updated-md5-1");
        when(dataSource.writeWithLock(any(UpdateConfigCommand.class), any(GoConfigHolder.class)))
                .thenReturn(new GoFileConfigDataSource.GoConfigSaveResult(
                        new GoConfigHolder(updatedConfig, updatedConfig), ConfigSaveState.UPDATED));
        cachedGoConfig.writeWithLock(new UpdateConfigCommand() {
            @Override
            public CruiseConfig update(CruiseConfig cruiseConfig) throws Exception {
                cruiseConfig.addEnvironment(UUID.randomUUID().toString());
                return cruiseConfig;
            }
        });
        assertThat(listener.invocationCount, is(3));
    }

    private class ConfigChangedListenerThatTracksInvocationCount implements ConfigChangedListener {
        public int invocationCount = 0;

        @Override
        public void onConfigChange(CruiseConfig newCruiseConfig) {
            invocationCount++;
        }
    }
}
