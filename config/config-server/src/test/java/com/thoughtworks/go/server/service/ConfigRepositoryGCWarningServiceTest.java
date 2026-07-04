/*
 * Copyright Thoughtworks, Inc.
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

import com.thoughtworks.go.config.ConfigRepository;
import com.thoughtworks.go.config.ConfigRepositoryGCWarningService;
import com.thoughtworks.go.serverhealth.*;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ConfigRepositoryGCWarningServiceTest {

    private ConfigRepository configRepository;
    private ServerHealthService serverHealthService;
    private SystemEnvironment systemEnvironment;
    private ConfigRepositoryGCWarningService service;

    @BeforeEach
    public void setUp() {
        configRepository = mock(ConfigRepository.class);
        serverHealthService = new ServerHealthService(mock());
        systemEnvironment = mock(SystemEnvironment.class);
        service = new ConfigRepositoryGCWarningService(configRepository, serverHealthService, systemEnvironment);
    }

    @Test
    public void shouldAddWarningWhenConfigRepoLooseObjectCountGoesBeyondTheConfiguredThreshold() throws Exception {
        when(systemEnvironment.get(SystemEnvironment.GO_CONFIG_REPO_GC_LOOSE_OBJECT_WARNING_THRESHOLD)).thenReturn(10L);
        when(configRepository.getLooseObjectCount()).thenReturn(20L);

        service.checkRepoAndAddWarningIfRequired();
        List<ServerHealthState> healthStates = serverHealthService.logsSortedForScope(HealthStateScope.forConfigRepo("GC"));
        assertThat(healthStates.getFirst().getMessage()).isEqualTo("Action required: Run 'git gc' on config.git repo");
        assertThat(healthStates.getFirst().getDescription())
            .contains("&lt;GoCD server installation directory&gt;")
            .contains("<a target='_blank' href='");
        assertThat(healthStates.getFirst().getLogLevel()).isEqualTo(HealthStateLevel.WARNING);
    }

    @Test
    public void shouldNotAddWarningWhenConfigRepoLooseObjectCountIsBelowTheConfiguredThreshold() throws Exception {
        when(systemEnvironment.get(SystemEnvironment.GO_CONFIG_REPO_GC_LOOSE_OBJECT_WARNING_THRESHOLD)).thenReturn(10L);
        when(configRepository.getLooseObjectCount()).thenReturn(1L);

        service.checkRepoAndAddWarningIfRequired();
        List<ServerHealthState> healthStates = serverHealthService.logsSortedForScope(HealthStateScope.forConfigRepo("GC"));
        assertThat(healthStates.isEmpty()).isTrue();
    }

    @Test
    public void shouldRemoteExistingWarningAboutGCIfLooseObjectCountGoesBelowTheSetThreshold() throws Exception {
        serverHealthService.update(ServerHealthState.warning("message", "description", HealthStateType.general(HealthStateScope.forConfigRepo("GC"))));
        assertThat(serverHealthService.logsSortedForScope(HealthStateScope.forConfigRepo("GC")).isEmpty()).isFalse();

        when(systemEnvironment.get(SystemEnvironment.GO_CONFIG_REPO_GC_LOOSE_OBJECT_WARNING_THRESHOLD)).thenReturn(10L);
        when(configRepository.getLooseObjectCount()).thenReturn(1L);

        service.checkRepoAndAddWarningIfRequired();
        assertThat(serverHealthService.logsSortedForScope(HealthStateScope.forConfigRepo("GC")).isEmpty()).isTrue();
    }

}
