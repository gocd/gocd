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
package com.thoughtworks.go.config;

import com.thoughtworks.go.config.remote.FileConfigOrigin;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.helper.PipelineConfigMother;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class BasicPipelineConfigsTest extends PipelineConfigsTestBase {

    @Override
    protected PipelineConfigs createWithPipeline(PipelineConfig pipelineConfig) {
        return new BasicPipelineConfigs(pipelineConfig);
    }

    @Override
    protected PipelineConfigs createEmpty() {
        return new BasicPipelineConfigs();
    }

    @Override
    protected PipelineConfigs createWithPipelines(PipelineConfig first, PipelineConfig second) {
        return new BasicPipelineConfigs(first, second);
    }

    @Test
    public void shouldReturnSelfForGetLocalWhenOriginIsNull() {
        PipelineConfigs pipelineConfigs = createEmpty();
        assertThat(pipelineConfigs.getLocal().size()).isEqualTo(0);
        assertSame(pipelineConfigs, pipelineConfigs.getLocal());
    }

    @Test
    public void shouldReturnSelfForGetLocalPartsWhenOriginIsFile() {
        PipelineConfigs pipelineConfigs = createEmpty();
        pipelineConfigs.setOrigins(new FileConfigOrigin());
        assertThat(pipelineConfigs.getLocal().size()).isEqualTo(0);
        assertSame(pipelineConfigs, pipelineConfigs.getLocal());
    }

    @Test
    public void shouldReturnNullGetLocalPartsWhenOriginIsRepo() {
        PipelineConfigs pipelineConfigs = createEmpty();
        pipelineConfigs.setOrigins(new RepoConfigOrigin());
        assertNull(pipelineConfigs.getLocal());
    }


    @Test
    public void shouldSetOriginInPipelines() {
        PipelineConfig pipe = PipelineConfigMother.pipelineConfig("pipeline1");
        PipelineConfigs group = new BasicPipelineConfigs(pipe);
        group.setOrigins(new FileConfigOrigin());
        assertThat(pipe.getOrigin()).isEqualTo(new FileConfigOrigin());
    }

    @Test
    public void shouldSetOriginInAuthorization() {
        PipelineConfig pipe = PipelineConfigMother.pipelineConfig("pipeline1");
        PipelineConfigs group = new BasicPipelineConfigs(pipe);
        group.setOrigins(new FileConfigOrigin());
        assertThat(group.getAuthorization().getOrigin()).isEqualTo(new FileConfigOrigin());
    }

    @Test
    public void shouldAnswerWhetherTheGroupNameIsSame() {
        BasicPipelineConfigs group = new BasicPipelineConfigs("first", new Authorization());

        assertFalse(group.isNamed("second"));

        assertTrue(group.isNamed("First"));
        assertTrue(group.isNamed("FiRsT"));
        assertTrue(group.isNamed("FIRST"));
        assertTrue(group.isNamed("first"));
    }

    @Test
    public void shouldUpdateName() {
        PipelineConfigs group = createWithPipeline(PipelineConfigMother.pipelineConfig("pipeline1"));
        group.setConfigAttributes(Map.of(BasicPipelineConfigs.GROUP, "my-new-group"));
        assertThat(group.getGroup()).isEqualTo("my-new-group");

        group.setConfigAttributes(Map.of());
        assertThat(group.getGroup()).isEqualTo("my-new-group");

        group.setConfigAttributes(null);
        assertThat(group.getGroup()).isEqualTo("my-new-group");

        group.setConfigAttributes(mapOfNull(BasicPipelineConfigs.GROUP));
        assertThat(group.getGroup()).isNull();
    }

    private static Map<String, Object> mapOfNull(String key) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(key, null);
        return attributes;
    }
}
