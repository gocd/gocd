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

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.server.domain.PipelineConfigDependencyGraph;
import com.thoughtworks.go.server.materials.MaterialChecker;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * @understands
 */
public class AutoBuildTriangleDependencyTest {

    private GoConfigService goConfigService;
    private PipelineService pipelineService;
    private CruiseConfig cruiseConfig;
    private MaterialChecker materialChecker;
    private ServerHealthService serverHealthService;

    @Before public void setUp() throws Exception {
        goConfigService = mock(GoConfigService.class);
        cruiseConfig = mock(BasicCruiseConfig.class);
        pipelineService = mock(PipelineService.class);
        materialChecker = mock(MaterialChecker.class);
        when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);
        when(materialChecker.hasPipelineEverRunWith(any(String.class), any(MaterialRevisions.class))).thenReturn(false);
        serverHealthService = new ServerHealthService();
    }

    @Test
    public void should_useTriangleDependencyResolution_whenFainInIsOptedOut() throws Exception {
        SystemEnvironment systemEnvironment = mock(SystemEnvironment.class);
        when(systemEnvironment.enforceRevisionCompatibilityWithUpstream()).thenReturn(false);
        String pipelineName = "downstream";
        PipelineConfigDependencyGraph dependencyGraph = mock(PipelineConfigDependencyGraph.class);
        MaterialRevisions originalRevisions = mock(MaterialRevisions.class);
        MaterialRevisions recomputedRevisions = mock(MaterialRevisions.class);
        when(originalRevisions.isEmpty()).thenReturn(false);
        when(originalRevisions.hasDependencyMaterials()).thenReturn(true);
        when(goConfigService.upstreamDependencyGraphOf(pipelineName, cruiseConfig)).thenReturn(dependencyGraph);
        when(pipelineService.getRevisionsBasedOnDependencies(dependencyGraph, originalRevisions)).thenReturn(recomputedRevisions);
        DependencyMaterial dependencyMaterial = new DependencyMaterial(new CaseInsensitiveString("pipeline"), new CaseInsensitiveString("stage"));
        when(dependencyGraph.unsharedMaterialConfigs()).thenReturn(new MaterialConfigs(dependencyMaterial.config()));
        when(originalRevisions.findRevisionFor(dependencyMaterial)).thenReturn(new MaterialRevision(dependencyMaterial, new Modification()));

        AutoBuild autoBuild = new AutoBuild(goConfigService, pipelineService, pipelineName, systemEnvironment, materialChecker);
        AutoBuild spyAutoBuild = spy(autoBuild);
        doAnswer(new Answer() {
            @Override public Object answer(InvocationOnMock invocation) throws Throwable {
                return true;
            }
        }).when(spyAutoBuild).hasAnyUnsharedMaterialChanged(dependencyGraph, originalRevisions);
        BuildCause buildCause = spyAutoBuild.onModifications(originalRevisions, false, null);

        verify(pipelineService).getRevisionsBasedOnDependencies(dependencyGraph, originalRevisions);
        verify(pipelineService, never()).getRevisionsBasedOnDependencies(any(MaterialRevisions.class), any(BasicCruiseConfig.class), any(CaseInsensitiveString.class));
        assertThat(buildCause.getMaterialRevisions(), is(recomputedRevisions));
    }
}
