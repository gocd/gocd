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

package com.thoughtworks.go.config.serialization;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.MagicalGoConfigXmlLoader;
import com.thoughtworks.go.config.MagicalGoConfigXmlWriter;
import com.thoughtworks.go.metrics.service.MetricsProbeService;
import com.thoughtworks.go.util.ConfigElementImplementationRegistryMother;
import com.thoughtworks.go.util.FileUtil;
import org.junit.Before;
import org.junit.Test;

import static com.thoughtworks.go.helper.ConfigFileFixture.CONFIG_WITH_TRACKINGTOOL;
import static com.thoughtworks.go.helper.ConfigFileFixture.ONE_PIPELINE;
import static com.thoughtworks.go.helper.ConfigFileFixture.PIPELINE_WITH_TRACKINGTOOL;
import static org.apache.commons.io.IOUtils.toInputStream;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class TrackingToolTest {
    private MagicalGoConfigXmlLoader loader;
    private MagicalGoConfigXmlWriter writer;
    private ConfigCache configCache = new ConfigCache();

    @Before
    public void setUp() throws Exception {
        MetricsProbeService metricsProbeService = mock(MetricsProbeService.class);
        loader = new MagicalGoConfigXmlLoader(configCache, ConfigElementImplementationRegistryMother.withNoPlugins(), metricsProbeService);
        writer = new MagicalGoConfigXmlWriter(configCache, ConfigElementImplementationRegistryMother.withNoPlugins(), metricsProbeService);
    }

    @Test
    public void shouldLoadTrackingtool() throws Exception {
        CruiseConfig cruiseConfig = loader.loadConfigHolder(FileUtil.readToEnd(toInputStream(CONFIG_WITH_TRACKINGTOOL))).config;
        PipelineConfig pipelineConfig = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("pipeline1"));
        TrackingTool trackingTool = pipelineConfig.trackingTool();

        assertThat(trackingTool.getLink(), is("http://mingle05/projects/cce/cards/${ID}"));
        assertThat(trackingTool.getRegex(), is("(evo-\\d+)"));
    }

    @Test
    public void shouldWriteTrackingToolToConfig() throws Exception {
        CruiseConfig cruiseConfig = loader.loadConfigHolder(FileUtil.readToEnd(toInputStream(CONFIG_WITH_TRACKINGTOOL))).config;
        PipelineConfig pipelineConfig = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("pipeline1"));

        assertThat(writer.toXmlPartial(pipelineConfig), is(PIPELINE_WITH_TRACKINGTOOL));
    }

    @Test
    public void shouldNotReturnNullWhenTrackingToolIsNotConfigured() throws Exception {
        CruiseConfig cruiseConfig = loader.loadConfigHolder(FileUtil.readToEnd(toInputStream(ONE_PIPELINE))).config;
        PipelineConfig pipelineConfig = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("pipeline1"));
        TrackingTool trackingTool = pipelineConfig.trackingTool();

        assertThat(trackingTool, is(not(nullValue())));
        assertThat(trackingTool.getLink(), is(""));
        assertThat(trackingTool.getRegex(), is(""));
    }
}
