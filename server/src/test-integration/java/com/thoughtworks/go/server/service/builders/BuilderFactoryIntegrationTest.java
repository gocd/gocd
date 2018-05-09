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

package com.thoughtworks.go.server.service.builders;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.builder.Builder;
import com.thoughtworks.go.domain.builder.FetchArtifactBuilder;
import com.thoughtworks.go.domain.builder.FetchPluggableArtifactBuilder;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.PipelineService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:testPropertyConfigurer.xml"
})
public class BuilderFactoryIntegrationTest {
    @Autowired
    private BuilderFactory builderFactory;
    @Autowired
    private PipelineService upstreamResolver;
    @Autowired
    private GoConfigService goConfigService;

    @Test
    public void shouldCreateBuilderForFetchTask() {
        final FetchTask fetchTask = new FetchTask(new CaseInsensitiveString("up42"),
                new CaseInsensitiveString("up42_stage"),
                new CaseInsensitiveString("up42_job"), "installers.zip", "dist");

        final Pipeline pipeline = new Pipeline("up42", BuildCause.createExternal());

        final Builder builder = builderFactory.builderFor(fetchTask, pipeline, upstreamResolver);

        assertThat(builder, instanceOf(FetchArtifactBuilder.class));
    }

    @Test
    public void shouldCreateBuilderForFetchPluggableArtifactTask() {
        final PipelineConfig pipelineConfig = goConfigService.pipelineConfigNamed(new CaseInsensitiveString("up42"));
        goConfigService.artifactStores().add(new ArtifactStore("storeId", "PluginId"));
        pipelineConfig.getStage("up42_stage").jobConfigByConfigName("up42_job").artifactConfigs()
                .add(new PluggableArtifactConfig("artifactId", "storeId"));

        final FetchPluggableArtifactTask pluggableArtifactTask = new FetchPluggableArtifactTask(
                new CaseInsensitiveString("up42"),
                new CaseInsensitiveString("up42_stage"),
                new CaseInsensitiveString("up42_job"), "artifactId");

        final Pipeline pipeline = new Pipeline("up42", BuildCause.createExternal());

        final Builder builder = builderFactory.builderFor(pluggableArtifactTask, pipeline, upstreamResolver);

        assertThat(builder, instanceOf(FetchPluggableArtifactBuilder.class));
    }
}