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

package com.thoughtworks.go.config;

import com.thoughtworks.go.listener.ConfigChangedListener;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.PipelineConfigService;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
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

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        configHolder = new GoConfigHolder(new BasicCruiseConfig(), new BasicCruiseConfig());
        cachedGoConfig = new CachedGoConfig(serverHealthService, dataSource);
        when(dataSource.load()).thenReturn(configHolder);
    }

    @Test
    public void shouldDelegateWritePipelineConfigCallToDataSource() {
        PipelineConfigService.SaveCommand saveCommand = mock(PipelineConfigService.SaveCommand.class);
        PipelineConfig pipelineConfig = new PipelineConfig();
        CachedGoConfig.PipelineConfigSaveResult saveResult = mock(CachedGoConfig.PipelineConfigSaveResult.class);
        GoConfigHolder savedConfig = new GoConfigHolder(new BasicCruiseConfig(), new BasicCruiseConfig());
        when(saveResult.getConfigHolder()).thenReturn(savedConfig);
        GoConfigHolder holderBeforeUpdate = cachedGoConfig.loadConfigHolder();
        Username user = new Username(new CaseInsensitiveString("user"));
        when(dataSource.writePipelineWithLock(pipelineConfig, holderBeforeUpdate, saveCommand, user)).thenReturn(saveResult);

        cachedGoConfig.writePipelineWithLock(pipelineConfig, saveCommand, user);
        assertThat(cachedGoConfig.loadConfigHolder(), is(savedConfig));
        assertThat(cachedGoConfig.currentConfig(), is(savedConfig.config));
        assertThat(cachedGoConfig.loadForEditing(), is(savedConfig.configForEdit));
        verify(dataSource).writePipelineWithLock(pipelineConfig, holderBeforeUpdate, saveCommand, user);
    }

    @Test
    public void shouldLoadConfigHolderIfNotAvailable() throws Exception {
        cachedGoConfig.forceReload();
        Assert.assertThat(cachedGoConfig.loadConfigHolder(), is(configHolder));
    }

    @Test
    public void shouldNotifyConfigListenersWhenConfigChanges() throws Exception {
        when(dataSource.writeWithLock(any(UpdateConfigCommand.class), any(GoConfigHolder.class))).thenReturn(new GoFileConfigDataSource.GoConfigSaveResult(configHolder, ConfigSaveState.UPDATED));
        final ConfigChangedListener listener = mock(ConfigChangedListener.class);
        cachedGoConfig.registerListener(listener);
        cachedGoConfig.forceReload();

        cachedGoConfig.writeWithLock(new UpdateConfigCommand() {
            @Override
            public CruiseConfig update(CruiseConfig cruiseConfig) throws Exception {
                return cruiseConfig;
            }
        });

        verify(listener, times(2)).onConfigChange(any(BasicCruiseConfig.class));
    }

    @Test
    public void shouldNotNotifyWhenConfigIsNullDuringRegistration() throws Exception {
        final ConfigChangedListener listener = mock(ConfigChangedListener.class);
        cachedGoConfig.registerListener(listener);
        verifyNoMoreInteractions(listener);
    }
}
