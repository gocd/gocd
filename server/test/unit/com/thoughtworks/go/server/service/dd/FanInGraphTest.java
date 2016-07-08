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

package com.thoughtworks.go.server.service.dd;

import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.BasicPipelineConfigs;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.ScmMaterialConfig;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig;
import com.thoughtworks.go.helper.PipelineConfigMother;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class FanInGraphTest {
    @Test
    public void shouldConstructAFaninGraph() throws Exception {
        GitMaterialConfig git = new GitMaterialConfig("giturl", "dest");
        HgMaterialConfig hg = new HgMaterialConfig("hgurl", "dest");
        PipelineConfig p1 = PipelineConfigMother.pipelineConfig("p1", new MaterialConfigs(git));
        DependencyMaterialConfig p1Dep = new DependencyMaterialConfig(p1.name(), p1.get(0).name());
        PipelineConfig p2 = PipelineConfigMother.pipelineConfig("p2", new MaterialConfigs(p1Dep));
        PipelineConfig p3 = PipelineConfigMother.pipelineConfig("p3", new MaterialConfigs(p1Dep, hg));
        DependencyMaterialConfig p2Dep = new DependencyMaterialConfig(p2.name(), p2.get(0).name());
        DependencyMaterialConfig p3Dep = new DependencyMaterialConfig(p3.name(), p3.get(0).name());
        PipelineConfig p4 = PipelineConfigMother.pipelineConfig("p4", new MaterialConfigs(p2Dep, p3Dep));

        CruiseConfig cruiseConfig = new BasicCruiseConfig(new BasicPipelineConfigs(p1, p2, p3, p4));
        FanInGraph faninGraph = new FanInGraph(cruiseConfig, p4.name(), null, null, null, null);
        List<ScmMaterialConfig> scmMaterialNodes = faninGraph.getScmMaterials();
        List<String> scmMaterialUrls = new ArrayList<String>();
        for (ScmMaterialConfig scmMaterialNode : scmMaterialNodes) {
            scmMaterialUrls.add(scmMaterialNode.getUrl().toString());
        }
        assertThat(scmMaterialUrls.contains("giturl"), is(true));
        assertThat(scmMaterialUrls.contains("hgurl"), is(true));
    }
}
