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
package com.thoughtworks.go.config.serialization;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.util.ConfigElementImplementationRegistryMother;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.thoughtworks.go.helper.ConfigFileFixture.ONE_PIPELINE;
import static com.thoughtworks.go.util.GoConstants.CONFIG_SCHEMA_VERSION;
import static org.assertj.core.api.Assertions.assertThat;

public class TrackingToolSerializationTest {
    private MagicalGoConfigXmlLoader loader;
    private MagicalGoConfigXmlWriter writer;

    private static final String PIPELINE_WITH_TRACKINGTOOL = """
        <pipeline name="pipeline1">
          <trackingtool link="http://mingle05/projects/cce/cards/${ID}" regex="(evo-\\d+)" />
          <materials>
            <svn url="foobar" checkexternals="true" />
          </materials>
          <stage name="stage">
            <jobs>
              <job name="functional">
                <tasks>
                  <ant />
                </tasks>
                <artifacts>
                  <artifact type="build" src="artifact1.xml" dest="cruise-output" />
                </artifacts>
              </job>
            </jobs>
          </stage>
        </pipeline>""";

    private static final String CONFIG_WITH_TRACKINGTOOL = """
        <?xml version="1.0" encoding="utf-8"?>
        <cruise xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"  xsi:noNamespaceSchemaLocation="cruise-config.xsd" schemaVersion="%d">
          <server>
             <artifacts>
                   <artifactsDir>other-artifacts</artifactsDir>
             </artifacts>
        </server>
          <pipelines>
            %s
          </pipelines>
        </cruise>
        """.formatted(CONFIG_SCHEMA_VERSION, PIPELINE_WITH_TRACKINGTOOL);

    @BeforeEach
    public void setUp() {
        loader = new MagicalGoConfigXmlLoader(ConfigElementImplementationRegistryMother.withNoPlugins());
        writer = new MagicalGoConfigXmlWriter(ConfigElementImplementationRegistryMother.withNoPlugins());
    }

    @Test
    public void shouldLoadTrackingTool() throws Exception {
        CruiseConfig cruiseConfig = loader.loadConfigHolder(CONFIG_WITH_TRACKINGTOOL).config;
        PipelineConfig pipelineConfig = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("pipeline1"));
        TrackingTool trackingTool = pipelineConfig.trackingToolOrDefault();

        assertThat(trackingTool.getLink()).isEqualTo("http://mingle05/projects/cce/cards/${ID}");
        assertThat(trackingTool.getRegex()).isEqualTo("(evo-\\d+)");
        assertThat(trackingTool.render("evo-123: fixed bug")).isEqualTo("<a href=\"http://mingle05/projects/cce/cards/evo-123\" target=\"story_tracker\">evo-123</a>: fixed bug");
    }

    @Test
    public void shouldWriteTrackingToolToConfig() throws Exception {
        CruiseConfig cruiseConfig = loader.loadConfigHolder(CONFIG_WITH_TRACKINGTOOL).config;
        PipelineConfig pipelineConfig = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("pipeline1"));

        assertThat(writer.toXmlPartial(pipelineConfig)).isEqualTo(PIPELINE_WITH_TRACKINGTOOL);
    }

    @Test
    public void shouldNotReturnNullWhenTrackingToolIsNotConfigured() throws Exception {
        CruiseConfig cruiseConfig = loader.loadConfigHolder(ONE_PIPELINE).config;
        PipelineConfig pipelineConfig = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("pipeline1"));
        TrackingTool trackingTool = pipelineConfig.trackingToolOrDefault();

        assertThat(trackingTool).isNotNull();
        assertThat(trackingTool.getLink()).isEmpty();
        assertThat(trackingTool.getRegex()).isEmpty();
        assertThat(trackingTool.render("evo-123: fixed bug")).isEqualTo("evo-123: fixed bug");
    }
}
