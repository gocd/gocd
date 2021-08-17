/*
 * Copyright 2021 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.Filter;
import com.thoughtworks.go.config.materials.IgnoredFiles;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.config.materials.mercurial.HgMaterial;
import com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig;
import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.helper.*;
import com.thoughtworks.go.server.domain.PipelineConfigDependencyGraph;
import com.thoughtworks.go.server.materials.MaterialChecker;
import com.thoughtworks.go.util.GoConstants;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static com.thoughtworks.go.domain.config.CaseInsensitiveStringMother.str;
import static com.thoughtworks.go.helper.ModificationsMother.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AutoBuildCauseTest {
    @Mock
    private GoConfigService goConfigService;
    @Mock
    private PipelineService pipelineService;
    @Mock
    private MaterialChecker materialChecker;
    @Mock
    private SystemEnvironment systemEnvironment;

    private CruiseConfig cruiseConfig;

    @BeforeEach
    public void setUp() throws Exception {
        cruiseConfig = new BasicCruiseConfig();
        lenient().when(goConfigService.currentCruiseConfig()).thenReturn(cruiseConfig);
    }

    @Test
    public void shouldThrowExceptionIfNoChanges() {
        MaterialRevisions modifications = new MaterialRevisions();
        assertThatThrownBy(() -> new AutoBuild(goConfigService, pipelineService, "foo", new SystemEnvironment(), materialChecker).onModifications(modifications, false, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cannot find modifications");
    }

    @Test
    public void shouldSetApproverToCruiseForTheProducedBuildCause() throws Exception {
        SvnMaterial material = new SvnMaterial("http://foo.bar/baz", "user", "pass", false);
        MaterialRevisions materialRevisions = new MaterialRevisions(new MaterialRevision(material, new Modification(new Date(), "1234", "MOCK_LABEL-12", null)));

        BuildCause buildCause = new AutoBuild(goConfigService, pipelineService, "foo", new SystemEnvironment(), materialChecker).onModifications(materialRevisions, false,
                null);
        assertThat(buildCause.getApprover()).isEqualTo(GoConstants.DEFAULT_APPROVED_BY);
    }

    @Test
    public void shouldReturnNullIfUpstreamMaterialHasNotChanged_WithFaninOff() throws Exception {
        PipelineConfigDependencyGraph dependencyGraph = dependencyGraphOfDepthOne(MaterialConfigsMother.hgMaterialConfig());

        MaterialRevisions revisions = createHgMaterialWithMultipleRevisions(1, oneModifiedFile("2"));
        MaterialRevision dependencyRevision = dependencyMaterialRevision("up1", 1, "label", "first", 1, new Date());
        dependencyRevision.markAsNotChanged();
        revisions.addRevision(dependencyRevision);

        when(goConfigService.upstreamDependencyGraphOf("current", cruiseConfig)).thenReturn(dependencyGraph);

        when(systemEnvironment.enforceRevisionCompatibilityWithUpstream()).thenReturn(false);
        AutoBuild autoBuild = new AutoBuild(goConfigService, pipelineService, "current", systemEnvironment, materialChecker);

        BuildCause current = autoBuild.onModifications(revisions, false, null);
        assertThat(current).isNull();
    }

    @Test
    public void shouldReturnNullIfUpstreamMaterialAndFirstOrderMaterialHaveNotChanged_WithFaninOff() throws Exception {
        PipelineConfigDependencyGraph dependencyGraph = dependencyGraphOfDepthOne(MaterialConfigsMother.hgMaterialConfig(), MaterialConfigsMother.svnMaterialConfig());

        MaterialRevisions revisions = createHgMaterialWithMultipleRevisions(1, oneModifiedFile("2"));
        MaterialRevisions firstOrderRevision = createSvnMaterialWithMultipleRevisions(1, oneModifiedFile("2"));
        MaterialRevision dependencyRevision = dependencyMaterialRevision("up1", 1, "label", "first", 1, new Date());
        revisions.addAll(firstOrderRevision);
        revisions.addRevision(dependencyRevision);

        dependencyRevision.markAsNotChanged();
        firstOrderRevision.getMaterialRevision(0).markAsNotChanged();
        revisions.getMaterialRevision(0).markAsChanged();

        when(goConfigService.upstreamDependencyGraphOf("downstream", cruiseConfig)).thenReturn(dependencyGraph);

        when(systemEnvironment.enforceRevisionCompatibilityWithUpstream()).thenReturn(false);
        AutoBuild build = new AutoBuild(goConfigService, pipelineService, "downstream", systemEnvironment, materialChecker);

        BuildCause cause = build.onModifications(revisions, false, null);
        assertThat(cause).isNull();
    }

    @Test
    public void shouldReturnCorrectRevisionsIfFirstOrderMaterialIsChanged() throws Exception {
        PipelineConfigDependencyGraph dependencyGraph = dependencyGraphOfDepthOne(MaterialConfigsMother.hgMaterialConfig(), MaterialConfigsMother.svnMaterialConfig());
        String targetPipeline = dependencyGraph.getCurrent().name().toLower();

        MaterialRevisions revisions = createHgMaterialWithMultipleRevisions(1, oneModifiedFile("2"));
        MaterialRevisions firstOrderRevision = createSvnMaterialWithMultipleRevisions(1, oneModifiedFile("2"));
        MaterialRevision dependencyRevision = dependencyMaterialRevision("upstream", 1, "label", "first", 1, new Date());
        revisions.addAll(firstOrderRevision);
        revisions.addRevision(dependencyRevision);

        dependencyRevision.markAsNotChanged();
        firstOrderRevision.getMaterialRevision(0).markAsChanged();
        revisions.getMaterialRevision(0).markAsChanged();

        MaterialRevisions expectedRevisions = createHgMaterialWithMultipleRevisions(1, oneModifiedFile("1"));
        expectedRevisions.addRevision(dependencyRevision);

        when(pipelineService.getRevisionsBasedOnDependencies(eq(revisions), eq(cruiseConfig), eq(dependencyGraph.getCurrent().name()))).thenReturn(expectedRevisions);

        assertThat(new AutoBuild(goConfigService, pipelineService, targetPipeline, new SystemEnvironment(), materialChecker)
                .onModifications(revisions, false, null).getMaterialRevisions())
                .isEqualTo(expectedRevisions);
    }

    @Test
    public void shouldReturnCorrectRevisionsIfUpstreamIgnoresAllTheModificationsAndFirstOrderMaterialNotChanged() throws Exception {
        PipelineConfigDependencyGraph dependencyGraph = dependencyGraphOfDepthOne(MaterialConfigsMother.hgMaterialConfig(), MaterialConfigsMother.svnMaterialConfig());
        String targetPipeline = dependencyGraph.getCurrent().name().toLower();
        firstHgMaterial(dependencyGraph).setFilter(new Filter(new IgnoredFiles("**/*.xml")));

        MaterialRevisions revisions = createHgMaterialWithMultipleRevisions(1, oneModifiedFile("2"));
        MaterialRevisions firstOrderRevision = createSvnMaterialWithMultipleRevisions(1, oneModifiedFile("2"));
        MaterialRevision dependencyRevision = dependencyMaterialRevision("up1", 1, "label", "first", 1, new Date());
        revisions.addAll(firstOrderRevision);
        revisions.addRevision(dependencyRevision);

        dependencyRevision.markAsNotChanged();
        firstOrderRevision.getMaterialRevision(0).markAsNotChanged();
        revisions.getMaterialRevision(0).markAsChanged();

        MaterialRevisions expectedRevisions = createHgMaterialWithMultipleRevisions(1, oneModifiedFile("2"));
        expectedRevisions.addRevision(dependencyRevision);

        when(pipelineService.getRevisionsBasedOnDependencies(eq(revisions), eq(cruiseConfig), eq(dependencyGraph.getCurrent().name()))).thenReturn(expectedRevisions);

        assertThat(new AutoBuild(goConfigService, pipelineService, targetPipeline, new SystemEnvironment(), materialChecker)
                .onModifications(revisions, false, null)
                .getMaterialRevisions())
                .isEqualTo(expectedRevisions);
    }

    @Test
    public void shouldReturnNullIfUpstreamMaterialHasChangedButNoFirstOrderMaterialHas_WithFaninOff() throws Exception {
        HgMaterialConfig hg = MaterialConfigsMother.hgMaterialConfig();
        PipelineConfig third = PipelineConfigMother.pipelineConfig("third", MaterialConfigsMother.dependencyMaterialConfig("second", "mingle"), new JobConfigs());
        PipelineConfig second = PipelineConfigMother.pipelineConfig("second", MaterialConfigsMother.dependencyMaterialConfig("first", "mingle"), new JobConfigs());
        PipelineConfig first = PipelineConfigMother.pipelineConfig("first", hg, new JobConfigs());
        PipelineConfigDependencyGraph dependencyGraph = new PipelineConfigDependencyGraph(third,
                new PipelineConfigDependencyGraph(second, new PipelineConfigDependencyGraph(first)),
                new PipelineConfigDependencyGraph(first)
        );

        MaterialRevisions revisions = new MaterialRevisions();
        MaterialRevision firstRev = dependencyMaterialRevision("first", 1, "label", "mingle", 1, new Date());
        firstRev.markAsChanged();
        MaterialRevision secondRev = dependencyMaterialRevision("second", 1, "label", "mingle", 1, new Date());
        secondRev.markAsNotChanged();
        revisions.addRevision(secondRev);
        revisions.addRevision(firstRev);

        when(goConfigService.upstreamDependencyGraphOf("third", cruiseConfig)).thenReturn(dependencyGraph);

        when(systemEnvironment.enforceRevisionCompatibilityWithUpstream()).thenReturn(false);
        AutoBuild build = new AutoBuild(goConfigService, pipelineService, "third", systemEnvironment, materialChecker);
        BuildCause cause = build.onModifications(revisions, false, null);
        assertThat(cause).isNull();
    }

    @Test
    public void shouldReturnPeggedRevisionsForUpstreamMaterialWhenFirstOrderDependencyMaterialIsChanged() throws Exception {
        HgMaterialConfig hg = MaterialConfigsMother.hgMaterialConfig();
        PipelineConfig third = PipelineConfigMother.pipelineConfig("third", MaterialConfigsMother.dependencyMaterialConfig("second", "mingle"), new JobConfigs());
        PipelineConfig second = PipelineConfigMother.pipelineConfig("second", MaterialConfigsMother.dependencyMaterialConfig("first", "mingle"), new JobConfigs());
        PipelineConfig first = PipelineConfigMother.pipelineConfig("first", hg, new JobConfigs());
        PipelineConfigDependencyGraph dependencyGraph = new PipelineConfigDependencyGraph(third,
                new PipelineConfigDependencyGraph(second, new PipelineConfigDependencyGraph(first)),
                new PipelineConfigDependencyGraph(first)
        );

        MaterialRevisions revisions = new MaterialRevisions();
        MaterialRevision firstRev = dependencyMaterialRevision("first", 10, "label", "mingle", 1, new Date());
        firstRev.markAsNotChanged();
        MaterialRevision secondRev = dependencyMaterialRevision("second", 1, "label", "mingle", 1, new Date());
        secondRev.markAsChanged();
        revisions.addRevision(secondRev);
        revisions.addRevision(firstRev);

        MaterialRevisions expectedRevisions = new MaterialRevisions();

        when(pipelineService.getRevisionsBasedOnDependencies(eq(revisions), eq(cruiseConfig), eq(dependencyGraph.getCurrent().name()))).thenReturn(expectedRevisions);

        assertThat(new AutoBuild(goConfigService, pipelineService, "third", new SystemEnvironment(), materialChecker)
                .onModifications(revisions, false, null)
                .getMaterialRevisions())
                .isSameAs(expectedRevisions);
    }

    @Test
    public void shouldUseTheMaterialRevisionsAfterGettingTheRightVersionsBasedOnDependency() throws Exception {
        PipelineConfigDependencyGraph dependencyGraph = dependencyGraphOfDepthOne(MaterialConfigsMother.hgMaterialConfig());
        String targetPipeline = dependencyGraph.getCurrent().name().toLower();

        MaterialRevisions revisions = createHgMaterialWithMultipleRevisions(1, oneModifiedFile("2"));
        MaterialRevision dependencyRevision = dependencyMaterialRevision("up1", 1, "label", "first", 1, new Date());
        dependencyRevision.markAsChanged();
        revisions.addRevision(dependencyRevision);

        MaterialRevisions expectedRevisions = createHgMaterialWithMultipleRevisions(1, oneModifiedFile("1"));
        expectedRevisions.addRevision(dependencyRevision);

        when(pipelineService.getRevisionsBasedOnDependencies(eq(revisions), eq(cruiseConfig), eq(dependencyGraph.getCurrent().name()))).thenReturn(expectedRevisions);

        BuildCause buildCause = new AutoBuild(goConfigService, pipelineService, targetPipeline, new SystemEnvironment(), materialChecker).onModifications(revisions, false, null);

        MaterialRevision expected = expectedRevisions.getMaterialRevision(0);
        assertThat(buildCause.getMaterialRevisions().getMaterialRevision(0)).isEqualTo(expected);
    }

    @Test
    public void shouldUpdateRecomputedMaterialRevisionsChangedStatus() throws Exception {
        PipelineConfigDependencyGraph dependencyGraph = dependencyGraphOfDepthOne(MaterialConfigsMother.hgMaterialConfig(), MaterialConfigsMother.svnMaterialConfig());
        String targetPipeline = dependencyGraph.getCurrent().name().toLower();
        MaterialRevisions revisions = createHgMaterialWithMultipleRevisions(1, oneModifiedFile("2"), oneModifiedFile("1"));
        MaterialRevisions revisionsForMaterial2 = multipleRevisions(MaterialsMother.svnMaterial(), 10, oneModifiedFile("svn2"));
        MaterialRevision dependencyRevision = dependencyMaterialRevision("upstream", 1, "label", "first", 1, new Date());
        revisions.addRevision(dependencyRevision);
        revisions.addAll(revisionsForMaterial2);
        for (MaterialRevision revision : revisions) {
            revision.markAsChanged();
        }

        MaterialRevisions expectedRevisions = createHgMaterialWithMultipleRevisions(1, oneModifiedFile("1"));
        expectedRevisions.getMaterialRevision(0).markAsChanged();
        MaterialRevisions expectedForMaterial2 = multipleRevisions(MaterialsMother.svnMaterial(), 10, oneModifiedFile("svn1"));
        expectedForMaterial2.getMaterialRevision(0).markAsNotChanged();
        expectedRevisions.addRevision(dependencyRevision);
        expectedRevisions.addAll(expectedForMaterial2);


        when(pipelineService.getRevisionsBasedOnDependencies(eq(revisions), eq(cruiseConfig), eq(dependencyGraph.getCurrent().name()))).thenReturn(expectedRevisions);

        BuildCause buildCause = new AutoBuild(goConfigService, pipelineService, targetPipeline, new SystemEnvironment(), materialChecker).onModifications(revisions, false, null);
        MaterialRevisions finalRevisions = buildCause.getMaterialRevisions();

        assertThat(finalRevisions.numberOfRevisions()).isEqualTo(expectedRevisions.numberOfRevisions());

        for (int i = 0; i < expectedRevisions.numberOfRevisions(); i++) {
            MaterialRevision finalRev = finalRevisions.getMaterialRevision(i);
            MaterialRevision expectedRev = expectedRevisions.getMaterialRevision(i);
            assertThat(finalRev).isEqualTo(expectedRev);
            assertThat(finalRev.isChanged()).isEqualTo(expectedRev.isChanged());
        }
    }

    @Test
    public void shouldNotFallbackToFanInOffTriangleDependencyBehaviourOnNoCompatibleUpstreamRevisionsException() throws Exception {
        PipelineConfigDependencyGraph dependencyGraph = dependencyGraphOfDepthOne(MaterialConfigsMother.hgMaterialConfig());
        String targetPipeline = dependencyGraph.getCurrent().name().toLower();
        MaterialRevisions revisions = createHgMaterialWithMultipleRevisions(1, oneModifiedFile("2"));
        MaterialRevision dependencyRevision = dependencyMaterialRevision("up1", 1, "label", "first", 1, new Date());
        dependencyRevision.markAsChanged();
        revisions.addRevision(dependencyRevision);
        NoCompatibleUpstreamRevisionsException expectedException = NoCompatibleUpstreamRevisionsException.failedToFindCompatibleRevision(new CaseInsensitiveString("downstream"), null);

        when(pipelineService.getRevisionsBasedOnDependencies(eq(revisions), eq(cruiseConfig), eq(dependencyGraph.getCurrent().name()))).thenThrow(expectedException);

        when(systemEnvironment.enforceRevisionCompatibilityWithUpstream()).thenReturn(true);

        try {
            new AutoBuild(goConfigService, pipelineService, targetPipeline, systemEnvironment, materialChecker).onModifications(revisions, false, null);
            fail("should have thrown exception");
        } catch (NoCompatibleUpstreamRevisionsException e) {
            assertThat(e).isEqualTo(expectedException);
        }
    }

    @Test
    public void shouldTurnOffFanInFallbackBehaviourWhenSystemEnvironmentVariableIsOff() throws Exception {
        PipelineConfigDependencyGraph dependencyGraph = dependencyGraphOfDepthOne(MaterialConfigsMother.hgMaterialConfig());
        String targetPipeline = dependencyGraph.getCurrent().name().toLower();
        MaterialRevisions revisions = createHgMaterialWithMultipleRevisions(1, oneModifiedFile("2"));
        MaterialRevision dependencyRevision = dependencyMaterialRevision("up1", 1, "label", "first", 1, new Date());
        dependencyRevision.markAsChanged();
        revisions.addRevision(dependencyRevision);
        RuntimeException expectedException = new RuntimeException("failed");

        when(pipelineService.getRevisionsBasedOnDependencies(eq(revisions), eq(cruiseConfig), eq(dependencyGraph.getCurrent().name()))).thenThrow(expectedException);
        when(systemEnvironment.enforceRevisionCompatibilityWithUpstream()).thenReturn(true);

        try {
            new AutoBuild(goConfigService, pipelineService, targetPipeline, systemEnvironment, materialChecker).onModifications(revisions, false, null);
            fail("should have thrown exception");
        } catch (RuntimeException e) {
            assertThat(e).isEqualTo(expectedException);
        }
    }

    @Test
    public void isValidBuildCause_shouldReturnFalseIfDependencyMaterialIsSetToIgnoreForScheduling() {
        DependencyMaterialConfig dependencyMaterialConfig = new DependencyMaterialConfig(str("upstream-pipeline"), str("stage1"));
        dependencyMaterialConfig.ignoreForScheduling(true);
        PipelineConfig pipelineToBeScheduled = GoConfigMother.createPipelineConfigWithMaterialConfig("my-pipeline",
                dependencyMaterialConfig);

        MaterialRevision materialRevision = ModificationsMother.dependencyMaterialRevision(2, "2", 1,
                new DependencyMaterial(dependencyMaterialConfig), new Date());
        materialRevision.markAsChanged();
        MaterialRevisions materialRevisions = new MaterialRevisions(materialRevision);

        AutoBuild autoBuild = new AutoBuild(goConfigService, pipelineService, "my-pipeline", systemEnvironment, materialChecker);
        boolean isValidBuildCause = autoBuild.isValidBuildCause(pipelineToBeScheduled, BuildCause.createWithModifications(materialRevisions, "changes"));

        assertThat(isValidBuildCause).isFalse();
    }

    @Test
    public void isValidBuildCause_shouldReturnTrueIfDependencyMaterialIsNotSetToIgnoreForScheduling() {
        DependencyMaterialConfig dependencyMaterialConfig = new DependencyMaterialConfig(str("upstream-pipeline"), str("stage1"));
        PipelineConfig pipelineToBeScheduled = GoConfigMother.createPipelineConfigWithMaterialConfig("my-pipeline",
                dependencyMaterialConfig);

        MaterialRevision materialRevision = ModificationsMother.dependencyMaterialRevision(2, "2", 1,
                new DependencyMaterial(dependencyMaterialConfig), new Date());
        materialRevision.markAsChanged();
        MaterialRevisions materialRevisions = new MaterialRevisions(materialRevision);

        AutoBuild autoBuild = new AutoBuild(goConfigService, pipelineService, "my-pipeline", systemEnvironment, materialChecker);
        boolean isValidBuildCause = autoBuild.isValidBuildCause(pipelineToBeScheduled, BuildCause.createWithModifications(materialRevisions, "changes"));

        assertThat(isValidBuildCause).isTrue();
    }

    private PipelineConfigDependencyGraph dependencyGraphOfDepthOne(MaterialConfig sharedMaterial, MaterialConfig firstOrderMaterial) {
        PipelineConfig current;
        if (firstOrderMaterial != null) {
            current = GoConfigMother.createPipelineConfigWithMaterialConfig("current", sharedMaterial, firstOrderMaterial, new DependencyMaterialConfig(new CaseInsensitiveString("up1"),
                    new CaseInsensitiveString("first")));
        } else {
            current = GoConfigMother.createPipelineConfigWithMaterialConfig("current", sharedMaterial,
                    new DependencyMaterialConfig(new CaseInsensitiveString("up1"), new CaseInsensitiveString("first")));
        }
        PipelineConfig upStream = GoConfigMother.createPipelineConfigWithMaterialConfig("up1", sharedMaterial);
        return new PipelineConfigDependencyGraph(current, new PipelineConfigDependencyGraph(upStream));
    }

    private PipelineConfigDependencyGraph dependencyGraphOfDepthOne(MaterialConfig sharedMaterial) {
        return dependencyGraphOfDepthOne(sharedMaterial, null);
    }

    private HgMaterial firstHgMaterial(PipelineConfigDependencyGraph dependencyGraph) {
        return ((HgMaterial) new MaterialConfigConverter().toMaterial(daddy(dependencyGraph).materialConfigs().first()));
    }

    private PipelineConfig daddy(PipelineConfigDependencyGraph dependencyGraph) {
        return dependencyGraph.getUpstreamDependencies().get(0).getCurrent();
    }
}
