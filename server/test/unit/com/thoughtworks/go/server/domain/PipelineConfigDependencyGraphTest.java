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

package com.thoughtworks.go.server.domain;

import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.materials.Filter;
import com.thoughtworks.go.config.materials.IgnoredFiles;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig;
import com.thoughtworks.go.config.materials.perforce.P4MaterialConfig;
import com.thoughtworks.go.config.materials.svn.SvnMaterialConfig;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.ModifiedAction;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.MaterialConfigsMother;
import com.thoughtworks.go.helper.ModificationsMother;
import org.junit.Test;

import static com.thoughtworks.go.helper.MaterialConfigsMother.filteredHgMaterialConfig;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class PipelineConfigDependencyGraphTest {

    @Test
    public void shouldFindPipelineConfigQueueEntryWithCorrespondingPath() throws Exception {
        HgMaterialConfig hgConfig = MaterialConfigsMother.hgMaterialConfig();
        PipelineConfig current = GoConfigMother.createPipelineConfigWithMaterialConfig("current", hgConfig, new DependencyMaterialConfig(new CaseInsensitiveString("up1"), new CaseInsensitiveString("first")),
                new DependencyMaterialConfig(new CaseInsensitiveString("up2"), new CaseInsensitiveString("first")));
        PipelineConfig up1 = GoConfigMother.createPipelineConfigWithMaterialConfig("up1", hgConfig,
                new DependencyMaterialConfig(new CaseInsensitiveString("uppest"), new CaseInsensitiveString("first")));
        PipelineConfig up2 = GoConfigMother.createPipelineConfigWithMaterialConfig("up2", hgConfig,
                new DependencyMaterialConfig(new CaseInsensitiveString("uppest"), new CaseInsensitiveString("first")));
        PipelineConfig uppest = GoConfigMother.createPipelineConfigWithMaterialConfig("uppest", hgConfig);

        PipelineConfigDependencyGraph dependencyGraph = new PipelineConfigDependencyGraph(current,
                                                            new PipelineConfigDependencyGraph(up1, new PipelineConfigDependencyGraph(uppest)),
                                                            new PipelineConfigDependencyGraph(up2, new PipelineConfigDependencyGraph(uppest))
                                                        );

        Queue<PipelineConfigDependencyGraph.PipelineConfigQueueEntry> queue = new LinkedList<PipelineConfigDependencyGraph.PipelineConfigQueueEntry>();
        queue.add(new PipelineConfigDependencyGraph.PipelineConfigQueueEntry(up1, Arrays.asList(current, up1)));
        queue.add(new PipelineConfigDependencyGraph.PipelineConfigQueueEntry(up2, Arrays.asList(current, up2)));
        queue.add(new PipelineConfigDependencyGraph.PipelineConfigQueueEntry(uppest, Arrays.asList(current, up1, uppest)));
        queue.add(new PipelineConfigDependencyGraph.PipelineConfigQueueEntry(uppest, Arrays.asList(current, up2, uppest)));
        assertThat(dependencyGraph.buildQueue(), is(queue));
    }

    @Test
    public void shouldFindPipelineConfigQueueEntryWithCorrespondingPathForHigherDepth() throws Exception {
        PipelineConfig current = GoConfigMother.createPipelineConfigWithMaterialConfig("current");
        PipelineConfig up1 = GoConfigMother.createPipelineConfigWithMaterialConfig("up1");
        PipelineConfig upper = GoConfigMother.createPipelineConfigWithMaterialConfig("upper");
        PipelineConfig up2 = GoConfigMother.createPipelineConfigWithMaterialConfig("up2");
        PipelineConfig uppest = GoConfigMother.createPipelineConfigWithMaterialConfig("uppest");

        PipelineConfigDependencyGraph dependencyGraph = new PipelineConfigDependencyGraph(current,
                                                            new PipelineConfigDependencyGraph(up1, new PipelineConfigDependencyGraph(upper, new PipelineConfigDependencyGraph(uppest)), new PipelineConfigDependencyGraph(uppest)),
                                                            new PipelineConfigDependencyGraph(up2, new PipelineConfigDependencyGraph(upper, new PipelineConfigDependencyGraph(uppest)), new PipelineConfigDependencyGraph(uppest))
                                                        );

        Queue<PipelineConfigDependencyGraph.PipelineConfigQueueEntry> queue = new LinkedList<PipelineConfigDependencyGraph.PipelineConfigQueueEntry>();
        queue.add(new PipelineConfigDependencyGraph.PipelineConfigQueueEntry(up1, Arrays.asList(current, up1)));
        queue.add(new PipelineConfigDependencyGraph.PipelineConfigQueueEntry(up2, Arrays.asList(current, up2)));
        queue.add(new PipelineConfigDependencyGraph.PipelineConfigQueueEntry(upper, Arrays.asList(current, up1, upper)));
        queue.add(new PipelineConfigDependencyGraph.PipelineConfigQueueEntry(uppest, Arrays.asList(current, up1, uppest)));
        queue.add(new PipelineConfigDependencyGraph.PipelineConfigQueueEntry(upper, Arrays.asList(current, up2, upper)));
        queue.add(new PipelineConfigDependencyGraph.PipelineConfigQueueEntry(uppest, Arrays.asList(current, up2, uppest)));
        queue.add(new PipelineConfigDependencyGraph.PipelineConfigQueueEntry(uppest, Arrays.asList(current, up1, upper, uppest)));
        queue.add(new PipelineConfigDependencyGraph.PipelineConfigQueueEntry(uppest, Arrays.asList(current, up2, upper, uppest)));
        Queue<PipelineConfigDependencyGraph.PipelineConfigQueueEntry> configQueueEntryQueue = dependencyGraph.buildQueue();
        assertThat(configQueueEntryQueue, is(queue));
    }

    @Test
    public void shouldReturnTheListOfFirstOrderMaterialsIgnoringDestFoldersForScmMaterials() throws Exception {
        HgMaterialConfig common1 = MaterialConfigsMother.hgMaterialConfig("hg-url", "one-folder");
        HgMaterialConfig common2 = MaterialConfigsMother.hgMaterialConfig("hg-url", "another-folder");
        SvnMaterialConfig firstOrderSVNMaterial = MaterialConfigsMother.svnMaterialConfig();
        GitMaterialConfig firstOrderGitMaterial = MaterialConfigsMother.gitMaterialConfig("url", "submodule", "branch", false);
        P4MaterialConfig firstOrderP4Material = MaterialConfigsMother.p4MaterialConfig();

        DependencyMaterialConfig up1DependencyMaterial = new DependencyMaterialConfig(new CaseInsensitiveString("up1"), new CaseInsensitiveString("first"));
        DependencyMaterialConfig up2DependencyMaterial = new DependencyMaterialConfig(new CaseInsensitiveString("up2"), new CaseInsensitiveString("first"));
        DependencyMaterialConfig uppestDependencyMaterial = new DependencyMaterialConfig(new CaseInsensitiveString("uppest"), new CaseInsensitiveString("first"));

        PipelineConfig current = GoConfigMother.createPipelineConfigWithMaterialConfig("current", common1, up1DependencyMaterial, up2DependencyMaterial);
        PipelineConfig up1 = GoConfigMother.createPipelineConfigWithMaterialConfig("up1", common2, firstOrderGitMaterial, uppestDependencyMaterial);
        PipelineConfig up2 = GoConfigMother.createPipelineConfigWithMaterialConfig("up2", firstOrderSVNMaterial, common2, uppestDependencyMaterial);
        PipelineConfig uppest = GoConfigMother.createPipelineConfigWithMaterialConfig("uppest", common1, firstOrderP4Material);

        PipelineConfigDependencyGraph uppestGraph = new PipelineConfigDependencyGraph(uppest);
        PipelineConfigDependencyGraph up1Graph = new PipelineConfigDependencyGraph(up1, uppestGraph);
        PipelineConfigDependencyGraph up2Graph = new PipelineConfigDependencyGraph(up2, uppestGraph);
        PipelineConfigDependencyGraph dependencyGraph = new PipelineConfigDependencyGraph(current, up1Graph, up2Graph);

        assertThat(dependencyGraph.unsharedMaterialConfigs().size(), is(2));
        assertThat(dependencyGraph.unsharedMaterialConfigs().get(0), is((MaterialConfig) up1DependencyMaterial));
        assertThat(dependencyGraph.unsharedMaterialConfigs().get(1), is((MaterialConfig) up2DependencyMaterial));

        assertThat(up1Graph.unsharedMaterialConfigs().size(), is(2));
        assertThat(up1Graph.unsharedMaterialConfigs().get(0), is((MaterialConfig) firstOrderGitMaterial));
        assertThat(up1Graph.unsharedMaterialConfigs().get(1), is((MaterialConfig) uppestDependencyMaterial));

        assertThat(up2Graph.unsharedMaterialConfigs().size(), is(2));
        assertThat(up2Graph.unsharedMaterialConfigs().get(0), is((MaterialConfig) firstOrderSVNMaterial));
        assertThat(up2Graph.unsharedMaterialConfigs().get(1), is((MaterialConfig) uppestDependencyMaterial));

        assertThat(uppestGraph.unsharedMaterialConfigs().size(), is(2));
        assertThat(uppestGraph.unsharedMaterialConfigs().get(0), is((MaterialConfig) common1));
        assertThat(uppestGraph.unsharedMaterialConfigs().get(1), is((MaterialConfig) firstOrderP4Material));
    }

    @Test
    public void shouldReturnTheSetOfFingerprintsOfAllMaterials() throws Exception {
        HgMaterialConfig common = MaterialConfigsMother.hgMaterialConfig();
        SvnMaterialConfig firstOrderSVNMaterial = MaterialConfigsMother.svnMaterialConfig();
        GitMaterialConfig firstOrderGitMaterial = MaterialConfigsMother.gitMaterialConfig("url", "submodule", "branch", false);
        P4MaterialConfig firstOrderP4Material = MaterialConfigsMother.p4MaterialConfig();

        DependencyMaterialConfig up1DependencyMaterial = new DependencyMaterialConfig(new CaseInsensitiveString("up1"), new CaseInsensitiveString("first"));
        DependencyMaterialConfig up2DependencyMaterial = new DependencyMaterialConfig(new CaseInsensitiveString("up2"), new CaseInsensitiveString("first"));
        DependencyMaterialConfig uppestDependencyMaterial = new DependencyMaterialConfig(new CaseInsensitiveString("uppest"), new CaseInsensitiveString("first"));

        PipelineConfig current = GoConfigMother.createPipelineConfigWithMaterialConfig("current", common, up1DependencyMaterial, up2DependencyMaterial);
        PipelineConfig up1 = GoConfigMother.createPipelineConfigWithMaterialConfig("up1", common, firstOrderGitMaterial, uppestDependencyMaterial);
        PipelineConfig up2 = GoConfigMother.createPipelineConfigWithMaterialConfig("up2", firstOrderSVNMaterial, common, uppestDependencyMaterial);
        PipelineConfig uppest = GoConfigMother.createPipelineConfigWithMaterialConfig("uppest", common, firstOrderP4Material);

        PipelineConfigDependencyGraph uppestGraph = new PipelineConfigDependencyGraph(uppest);
        PipelineConfigDependencyGraph up1Graph = new PipelineConfigDependencyGraph(up1, uppestGraph);
        PipelineConfigDependencyGraph up2Graph = new PipelineConfigDependencyGraph(up2, uppestGraph);
        PipelineConfigDependencyGraph dependencyGraph = new PipelineConfigDependencyGraph(current, up1Graph, up2Graph);

        assertThat(dependencyGraph.allMaterialFingerprints().size(), is(7));
        assertThat(dependencyGraph.allMaterialFingerprints(), hasItems(common.getFingerprint(), firstOrderSVNMaterial.getFingerprint(), firstOrderGitMaterial.getFingerprint(), firstOrderP4Material.getFingerprint(),
                                                                        up1DependencyMaterial.getFingerprint(), up2DependencyMaterial.getFingerprint(), uppestDependencyMaterial.getFingerprint()));
    }

    @Test
    public void shouldReturnIfSharedRevisionsAreIgnoredByAllDaddys() throws Exception {
        SvnMaterialConfig firstOrderSVNMaterial = MaterialConfigsMother.svnMaterialConfig();
        GitMaterialConfig firstOrderGitMaterial = MaterialConfigsMother.gitMaterialConfig("url");
        P4MaterialConfig firstOrderP4Material = MaterialConfigsMother.p4MaterialConfig();
        firstOrderP4Material.setFilter(new Filter(new IgnoredFiles("foo")));

        PipelineConfig current = GoConfigMother.createPipelineConfigWithMaterialConfig("current", MaterialConfigsMother.hgMaterialConfig(), new DependencyMaterialConfig(new CaseInsensitiveString("up1"),
                new CaseInsensitiveString("first")), new DependencyMaterialConfig(
                new CaseInsensitiveString("up2"), new CaseInsensitiveString("first")));
        PipelineConfig up1 = GoConfigMother.createPipelineConfigWithMaterialConfig("up1", filteredHgMaterialConfig("phigar"), firstOrderGitMaterial, new DependencyMaterialConfig(new CaseInsensitiveString("uppest"),
                new CaseInsensitiveString("first")));
        PipelineConfig up2 = GoConfigMother.createPipelineConfigWithMaterialConfig("up2", firstOrderSVNMaterial, firstOrderP4Material, filteredHgMaterialConfig("phigar"), new DependencyMaterialConfig(
                new CaseInsensitiveString("uppest"), new CaseInsensitiveString("first")));
        PipelineConfig uppest = GoConfigMother.createPipelineConfigWithMaterialConfig("uppest", filteredHgMaterialConfig("phigar"), firstOrderP4Material);

        PipelineConfigDependencyGraph uppestGraph = new PipelineConfigDependencyGraph(uppest);
        PipelineConfigDependencyGraph up1Graph = new PipelineConfigDependencyGraph(up1, uppestGraph);
        PipelineConfigDependencyGraph up2Graph = new PipelineConfigDependencyGraph(up2, uppestGraph);
        PipelineConfigDependencyGraph dependencyGraph = new PipelineConfigDependencyGraph(current, up1Graph, up2Graph);

        Modification modification = new Modification("user", "comment", "i@u.com", new Date(), "foo");
        modification.createModifiedFile("phigar", "", ModifiedAction.added);

        boolean ignored = dependencyGraph.isRevisionsOfSharedMaterialsIgnored(ModificationsMother.createHgMaterialWithMultipleRevisions(1, modification));
        assertThat(ignored, is(true));

        ignored = dependencyGraph.isRevisionsOfSharedMaterialsIgnored(ModificationsMother.createHgMaterialWithMultipleRevisions(1, ModificationsMother.oneModifiedFile("Silly")));
        assertThat(ignored, is(false));

        MaterialRevisions materialRevisions = ModificationsMother.createSvnMaterialRevisions(modification);
        materialRevisions.addAll(ModificationsMother.createP4MaterialRevisions(modification));
        materialRevisions.addAll(ModificationsMother.createHgMaterialWithMultipleRevisions(1, modification));
        ignored = up2Graph.isRevisionsOfSharedMaterialsIgnored(materialRevisions);
        assertThat(ignored, is(true));

        ignored = up2Graph.isRevisionsOfSharedMaterialsIgnored(ModificationsMother.createHgMaterialWithMultipleRevisions(1, ModificationsMother.oneModifiedFile("Silly")));
        assertThat(ignored, is(false));
    }

}
