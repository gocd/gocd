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

import com.thoughtworks.go.plugin.access.configrepo.ConfigRepoExtension;
import com.thoughtworks.go.plugin.configrepo.contract.CREnvironment;
import com.thoughtworks.go.plugin.configrepo.contract.CRParseResult;
import com.thoughtworks.go.plugin.configrepo.contract.CRPipeline;
import com.thoughtworks.go.plugin.configrepo.contract.ErrorCollection;
import com.thoughtworks.go.server.service.AgentService;
import com.thoughtworks.go.util.ConfigElementImplementationRegistryMother;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GoConfigPluginServiceTest {

    private GoConfigPluginService service;
    private final Collection<CREnvironment> environments = new ArrayList<>();
    private final Collection<CRPipeline> pipelines = new ArrayList<>();
    private final ErrorCollection errors = new ErrorCollection();

    @BeforeEach
    public void setUp() {
        ConfigRepoExtension extension = mock(ConfigRepoExtension.class);
        service = new GoConfigPluginService(extension, ConfigElementImplementationRegistryMother.withNoPlugins(),
            mock(CachedGoConfig.class), mock(AgentService.class));
        CRParseResult parseResult = new CRParseResult(environments, pipelines, errors);

        when(extension.parseDirectory(any(String.class), any(String.class), any())).thenReturn(parseResult);
    }

    @Test
    public void shouldAskExtensionForPluginImplementationWhenPluginIdSpecified() {
        PartialConfigProvider plugin = service.partialConfigProviderFor("plugin-id");
        assertThat(plugin instanceof ConfigRepoPlugin).isTrue();
        CRParseResult loaded = ((ConfigRepoPlugin) plugin).parseDirectory(new File("dir"), mock());
        assertSame(environments, loaded.getEnvironments());
        assertSame(pipelines, loaded.getPipelines());
        assertSame(errors, loaded.getErrors());
    }
}
