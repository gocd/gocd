/*
 * Copyright 2024 Thoughtworks, Inc.
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
package com.thoughtworks.go.agent;

import com.thoughtworks.go.agent.service.AgentUpgradeService;
import com.thoughtworks.go.agent.service.SslInfrastructureService;
import com.thoughtworks.go.agent.statusapi.AgentHealthHolder;
import com.thoughtworks.go.config.AgentRegistry;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.monitor.PluginJarLocationMonitor;
import com.thoughtworks.go.util.SubprocessLogger;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TestingClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AgentControllerTest {
    @Mock
    private SslInfrastructureService sslInfrastructureService;
    @Mock
    private SubprocessLogger subprocessLogger;
    @Mock
    private SystemEnvironment systemEnvironment;
    @Mock
    private AgentUpgradeService agentUpgradeService;
    @Mock
    private PluginManager pluginManager;
    @Mock
    private AgentRegistry agentRegistry;
    @Mock
    private PluginJarLocationMonitor pluginJarLocationMonitor;
    private AgentController agentController;

    private final TestingClock clock = new TestingClock();
    private final int pingInterval = 5000;
    private final AgentHealthHolder agentHealthHolder = new AgentHealthHolder(clock, pingInterval);

    @BeforeEach
    public void setUp() {
        agentController = createAgentController();
    }

    @Test
    void shouldReturnTrueIfCausedBySecurity() {
        Exception exception = new Exception(new RuntimeException(new GeneralSecurityException()));
        assertThat(agentController.isCausedBySecurity(exception)).isTrue();
    }

    @Test
    void shouldReturnFalseIfNotCausedBySecurity() {
        Exception exception = new Exception(new IOException());
        assertThat(agentController.isCausedBySecurity(exception)).isFalse();
    }

    @Test
    void shouldUpgradeAgentBeforeAgentRegistration() throws Exception {
        when(pluginJarLocationMonitor.hasRunAtLeastOnce()).thenReturn(true);
        assertThat(agentController.performWork()).isEqualTo(WorkAttempt.OK);
        InOrder inOrder = inOrder(agentUpgradeService, sslInfrastructureService, pluginJarLocationMonitor);
        inOrder.verify(agentUpgradeService).checkForUpgradeAndExtraProperties();
        inOrder.verify(sslInfrastructureService).registerIfNecessary(agentController.getAgentAutoRegistrationProperties());
        inOrder.verify(pluginJarLocationMonitor).hasRunAtLeastOnce();
    }

    @Test
    void shouldNotTryWorkIfPluginMonitorHasNotRun() {
        when(pluginJarLocationMonitor.hasRunAtLeastOnce()).thenReturn(false);
        assertThat(agentController.performWork()).isEqualTo(WorkAttempt.FAILED);
        verify(pluginJarLocationMonitor).hasRunAtLeastOnce();
    }

    @Test
    void remembersLastPingTime() {
        // initial time
        Date now = new Date(42);
        clock.setTime(now);
        agentController.pingSuccess();

        assertThat(agentHealthHolder.hasLostContact()).isFalse();
        clock.addMillis(pingInterval);
        assertThat(agentHealthHolder.hasLostContact()).isFalse();
        clock.addMillis(pingInterval);
        assertThat(agentHealthHolder.hasLostContact()).isTrue();
    }

    private AgentController createAgentController() {
        return new AgentController(sslInfrastructureService, systemEnvironment, agentRegistry, pluginManager,
            subprocessLogger, agentUpgradeService, agentHealthHolder, pluginJarLocationMonitor) {
            @Override
            public void ping() {
            }

            @Override
            public void execute() {
            }

            @Override
            protected WorkAttempt tryDoWork() {
                return WorkAttempt.OK;
            }
        };
    }
}
