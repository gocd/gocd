/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.metrics.service;

import com.thoughtworks.go.metrics.domain.probes.ProbeType;
import com.thoughtworks.go.metrics.domain.context.Context;
import com.thoughtworks.go.metrics.domain.probes.TimerProbe;
import com.thoughtworks.go.metrics.domain.probes.DoNothingProbe;
import com.thoughtworks.go.metrics.domain.probes.MessageQueueCounterProbe;
import com.thoughtworks.go.util.ReflectionUtil;
import com.thoughtworks.go.util.SystemEnvironment;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Timer;
import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MetricsProbeServiceTest {
    private MetricsProbeService metricsProbeService;
    private SystemEnvironment systemEnvironment;

    @Before
    public void setUp() throws Exception {
        systemEnvironment = mock(SystemEnvironment.class);
        when(systemEnvironment.get(SystemEnvironment.CAPTURE_METRICS)).thenReturn(true);
    }

    @Test
    public void shouldHaveAllTheProbesInMap() {
        metricsProbeService = new MetricsProbeService(systemEnvironment);
        assertThat(metricsProbeService.getProbe(ProbeType.SAVE_CONFIG_XML_THROUGH_API) instanceof TimerProbe, Is.is(true));
        assertThat(metricsProbeService.getProbe(ProbeType.SAVE_CONFIG_XML_THROUGH_XML_TAB) instanceof TimerProbe, Is.is(true));
        assertThat(metricsProbeService.getProbe(ProbeType.PREPROCESS_AND_VALIDATE) instanceof TimerProbe, Is.is(true));
        assertThat(metricsProbeService.getProbe(ProbeType.CONVERTING_CONFIG_XML_TO_OBJECT) instanceof TimerProbe, Is.is(true));
        assertThat(metricsProbeService.getProbe(ProbeType.VALIDATING_CONFIG) instanceof TimerProbe, Is.is(true));
        assertThat(metricsProbeService.getProbe(ProbeType.WRITE_CONFIG_TO_FILE_SYSTEM) instanceof TimerProbe, Is.is(true));
        assertThat(metricsProbeService.getProbe(ProbeType.SAVE_CONFIG_XML_THROUGH_CLICKY_ADMIN) instanceof TimerProbe, Is.is(true));
        assertThat(metricsProbeService.getProbe(ProbeType.SAVE_CONFIG_XML_THROUGH_SERVER_CONFIGURATION_TAB) instanceof TimerProbe, Is.is(true));
        assertThat(metricsProbeService.getProbe(ProbeType.UPDATE_CONFIG) instanceof TimerProbe, Is.is(true));
        assertThat(metricsProbeService.getProbe(ProbeType.MATERIAL_UPDATE_QUEUE_COUNTER) instanceof MessageQueueCounterProbe, Is.is(true));
        assertThat(((java.util.concurrent.ConcurrentMap) ReflectionUtil.getField(ReflectionUtil.getField(Metrics.defaultRegistry(), "threadPools"), "threadPools")).size() > 0, is(true));
    }

    @Test
    public void shouldStartAndStopAMetricProbe() throws InterruptedException {
        metricsProbeService = new MetricsProbeService(systemEnvironment);
        Timer timer = ((TimerProbe) metricsProbeService.getProbe(ProbeType.SAVE_CONFIG_XML_THROUGH_API)).getTimer();
        timer.clear();

        Context context = metricsProbeService.begin(ProbeType.SAVE_CONFIG_XML_THROUGH_API);
        Thread.sleep(500L);
        metricsProbeService.end(ProbeType.SAVE_CONFIG_XML_THROUGH_API, context);

        context = metricsProbeService.begin(ProbeType.SAVE_CONFIG_XML_THROUGH_API);
        Thread.sleep(1000L);
        metricsProbeService.end(ProbeType.SAVE_CONFIG_XML_THROUGH_API, context);

        assertThat(timer.count(), is(2L));
    }

    @Test
    public void shouldNotCaptureMetricsWhenProbesAreTurnedOff() throws InterruptedException {
        when(systemEnvironment.get(SystemEnvironment.CAPTURE_METRICS)).thenReturn(false);
        metricsProbeService = new MetricsProbeService(systemEnvironment);

        assertThat(metricsProbeService.getProbe(ProbeType.SAVE_CONFIG_XML_THROUGH_API) instanceof DoNothingProbe, is(true));
        assertThat(((java.util.List) ReflectionUtil.getField(Metrics.defaultRegistry(), "listeners")).size(), is(0));
        assertThat(((java.util.concurrent.ConcurrentMap) ReflectionUtil.getField(ReflectionUtil.getField(Metrics.defaultRegistry(), "threadPools"), "threadPools")).size(), is(0));
    }
}
