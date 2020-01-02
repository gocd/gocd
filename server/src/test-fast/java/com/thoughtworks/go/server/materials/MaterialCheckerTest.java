/*
 * Copyright 2020 ThoughtWorks, Inc.
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
package com.thoughtworks.go.server.materials;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.materials.Materials;
import com.thoughtworks.go.config.materials.PackageMaterial;
import com.thoughtworks.go.config.materials.ScmMaterial;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.Modifications;
import com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother;
import com.thoughtworks.go.helper.MaterialsMother;
import com.thoughtworks.go.helper.StageMother;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Date;

import static com.thoughtworks.go.domain.materials.Modification.modifications;
import static java.lang.String.format;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

public class MaterialCheckerTest {
    private MaterialRepository materialRepository;
    private ScmMaterial mockMaterial;
    private MaterialChecker materialChecker;

    @Before public void setUp() throws Exception {
        materialRepository = Mockito.mock(MaterialRepository.class);
        mockMaterial = Mockito.mock(ScmMaterial.class);
        materialChecker = new MaterialChecker(materialRepository);
    }

    @Test
    public void shouldUseFlyweightWorkingFolderForLatestModificationCheck() throws Exception {
        Modification modification = new Modification();
        Mockito.when(materialRepository.findLatestModification(mockMaterial)).thenReturn(revisions(mockMaterial, modification));

        materialChecker.findLatestRevisions(new MaterialRevisions(), new Materials(mockMaterial));

        Mockito.verify(materialRepository).findLatestModification(mockMaterial);
    }

    private MaterialRevisions revisions(Material material, Modification modification) {
        return new MaterialRevisions(new MaterialRevision(material, modifications(modification)));
    }

    @Test
    public void shouldUseLatestPipelineInstanceForDependentPipelineGivenThePreviousRevision() throws Exception {
        DependencyMaterial dependencyMaterial = new DependencyMaterial(new CaseInsensitiveString("pipeline-name"), new CaseInsensitiveString("stage-name"));

        Stage passedStage = StageMother.passedStageInstance("stage-name", "job-name", "pipeline-name");
        MaterialRevisions materialRevisions = new MaterialRevisions();
        Modification previous = new Modification("Unknown", "Unknown", null, passedStage.completedDate(), "pipeline-name/1/stage-name/0");
        MaterialRevision previousRevision = revisions(dependencyMaterial, previous).getMaterialRevision(0);
        when(materialRepository.findModificationsSince(dependencyMaterial, previousRevision)).thenReturn(Arrays.asList(new Modification(new Date(), "pipeline-name/2/stage-name/0", "MOCK_LABEL-12", null)));

        MaterialRevisions revisionsSince = materialChecker.findRevisionsSince(materialRevisions, new Materials(dependencyMaterial), new MaterialRevisions(previousRevision), new MaterialRevisions()/*will not be used, as no new material has been introduced*/);

        assertThat(revisionsSince.getMaterialRevision(0).getRevision().getRevision(), is("pipeline-name/2/stage-name/0"));
    }

    @Test
    public void shouldUseLatestPipelineInstanceForDependentPipeline() throws Exception {
        DependencyMaterial dependencyMaterial = new DependencyMaterial(new CaseInsensitiveString("pipeline-name"), new CaseInsensitiveString("stage-name"));
        Stage passedStage = StageMother.passedStageInstance("stage-name", "job-name", "pipeline-name");
        Modification modification = new Modification("Unknown", "Unknown", null, passedStage.completedDate(), "pipeline-name/1[LABEL-1]/stage-name/0");

        Mockito.when(materialRepository.findLatestModification(dependencyMaterial)).thenReturn(revisions(dependencyMaterial,modification));

        materialChecker.findLatestRevisions(new MaterialRevisions(), new Materials(dependencyMaterial));

        Mockito.verify(materialRepository).findLatestModification(dependencyMaterial);
    }

    @Test public void shouldSkipLatestRevisionsForMaterialsThatWereAlreadyChecked() throws Exception {
        DependencyMaterial dependencyMaterial = new DependencyMaterial(new CaseInsensitiveString("pipeline-name"), new CaseInsensitiveString("stage-name"));
        SvnMaterial svnMaterial = new SvnMaterial("svnUrl", null, null, false);
        Stage passedStage = StageMother.passedStageInstance("stage-name", "job-name", "pipeline-name");

        Modification dependencyModification = new Modification("Unknown", "Unknown", null, passedStage.completedDate(), "pipeline-name/1[LABEL-1]/stage-name/0");
        Modification svnModification = new Modification("user", "commend", "em@il", new Date(), "1");

        Mockito.when(materialRepository.findLatestModification(svnMaterial)).thenReturn(revisions(dependencyMaterial, svnModification));
        materialChecker.findLatestRevisions(new MaterialRevisions(new MaterialRevision(dependencyMaterial, dependencyModification)),
                new Materials(dependencyMaterial, svnMaterial));

        Mockito.verify(materialRepository, never()).findLatestModification(dependencyMaterial);
        Mockito.verify(materialRepository).findLatestModification(svnMaterial);
    }

    @Test
    public void shouldFindSpecificRevisionForDependentPipeline() throws Exception {
        DependencyMaterial dependencyMaterial = new DependencyMaterial(new CaseInsensitiveString("pipeline-name"), new CaseInsensitiveString("stage-name"));
        Stage passedStage = StageMother.passedStageInstance("stage-name", "job-name", "pipeline-name");
        Modification modification = new Modification("Unknown", "Unknown", null, passedStage.completedDate(), "pipeline-name/1/stage-name/0");

        Mockito.when(materialRepository.findModificationWithRevision(dependencyMaterial,"pipeline-name/1/stage-name/0")).thenReturn(modification);

        MaterialRevision actualRevision = materialChecker.findSpecificRevision(dependencyMaterial, "pipeline-name/1/stage-name/0");
        assertThat(actualRevision.getModifications().size(), is(1));
        assertThat(actualRevision.getModification(0).getModifiedTime(), is(passedStage.completedDate()));
        assertThat(actualRevision.getModification(0).getRevision(), is("pipeline-name/1/stage-name/0"));
    }

    @Test public void shouldThrowExceptionIfSpecifiedRevisionDoesNotExist() throws Exception {
        DependencyMaterial dependencyMaterial = new DependencyMaterial(new CaseInsensitiveString("pipeline-name"), new CaseInsensitiveString("stage-name"));
        Mockito.when(materialRepository.findModificationWithRevision(dependencyMaterial,"pipeline-name/500/stage-name/0")).thenReturn(null);

        try {
            materialChecker.findSpecificRevision(dependencyMaterial, "pipeline-name/500/stage-name/0");
            fail("Should not be able to find revision");
        } catch (Exception expected) {
            assertThat(expected.getMessage(), is(format("Unable to find revision [pipeline-name/500/stage-name/0] for material [%s]", dependencyMaterial)));
        }
    }

    @Test public void shouldThrowExceptionIfRevisionIsNotSpecified() throws Exception {
        DependencyMaterial dependencyMaterial = new DependencyMaterial(new CaseInsensitiveString("pipeline-name"), new CaseInsensitiveString("stage-name"));

        try {
            materialChecker.findSpecificRevision(dependencyMaterial, "");
            fail("Should not be able to empty revision");
        } catch (Exception expected) {
            assertThat(expected.getMessage(), is(format("Revision was not specified for material [%s]", dependencyMaterial)));
        }
    }

    @Test
    public void shouldSkipFindingRevisionsSinceForMaterialsThatWereAlreadyChecked() throws Exception {
        DependencyMaterial dependencyMaterial = new DependencyMaterial(new CaseInsensitiveString("pipeline-name"), new CaseInsensitiveString("stage-name"));
        SvnMaterial svnMaterial = new SvnMaterial("svnUrl", null, null, false);
        Stage passedStage = StageMother.passedStageInstance("stage-name", "job-name", "pipeline-name");

        MaterialRevision previousDependantRevision = new MaterialRevision(dependencyMaterial, new Modification("Unknown", "Unknown", null, passedStage.completedDate(), "pipeline-name/1[LABEL-1]/stage-name/0"));
        Modification dependencyModification = new Modification("Unknown", "Unknown", null, passedStage.completedDate(), "pipeline-name/2[LABEL-2]/stage-name/0");
        MaterialRevision previousSvnRevision = new MaterialRevision(svnMaterial, mod(1L));
        Modification svnModification = new Modification("user", "commend", "em@il", new Date(), "2");

        Mockito.when(materialRepository.findModificationsSince(svnMaterial, previousSvnRevision)).thenReturn(modifications(svnModification));
        MaterialRevisions alreadyFoundRevisions = new MaterialRevisions(new MaterialRevision(dependencyMaterial, dependencyModification));
        MaterialRevisions latestRevisions = new MaterialRevisions(); //will not be used, as no new materials have appeared
        MaterialRevisions revisionsSince = materialChecker.findRevisionsSince(alreadyFoundRevisions, new Materials(dependencyMaterial, svnMaterial), new MaterialRevisions(previousDependantRevision, previousSvnRevision), latestRevisions);
        assertThat(revisionsSince, is(new MaterialRevisions(new MaterialRevision(dependencyMaterial, dependencyModification), new MaterialRevision(svnMaterial, svnModification))));

        Mockito.verify(materialRepository, never()).findLatestModification(dependencyMaterial);
        Mockito.verify(materialRepository).findModificationsSince(svnMaterial, previousSvnRevision);
    }

    @Test
    public void shouldUseLatestMaterialDuringCreationOfNewRevisionsSince_bug7486() throws Exception {
        DependencyMaterial dependencyMaterial = new DependencyMaterial(new CaseInsensitiveString("pipeline-name"), new CaseInsensitiveString("stage-name"));
        PackageMaterial oldPkgMaterial = MaterialsMother.packageMaterial("repo-id", "repo-old-name", "pkg-id", "pkg-old-name", ConfigurationPropertyMother.create("key", false, "value"));
        Stage passedStage = StageMother.passedStageInstance("stage-name", "job-name", "pipeline-name");

        MaterialRevision previousDependantRevision = new MaterialRevision(dependencyMaterial, new Modification("Unknown", "Unknown", null, passedStage.completedDate(), "pipeline-name/1[LABEL-1]/stage-name/0"));
        Modification dependencyModification = new Modification("Unknown", "Unknown", null, passedStage.completedDate(), "pipeline-name/2[LABEL-2]/stage-name/0");
        Modification oldPkgMod = mod(1L);
        MaterialRevision previousPkgRevision = new MaterialRevision(oldPkgMaterial, oldPkgMod);

        PackageMaterial newPkgMaterial = MaterialsMother.packageMaterial("repo-id", "repo-new-name", "pkg-id", "pkg-new-name", ConfigurationPropertyMother.create("key", false, "value"));
        Modification newPkgMod = mod(2L);

        Mockito.when(materialRepository.findModificationsSince(oldPkgMaterial, previousPkgRevision)).thenReturn(modifications(newPkgMod));
        MaterialRevisions alreadyFoundRevisions = new MaterialRevisions(new MaterialRevision(dependencyMaterial, dependencyModification));
        MaterialRevisions latestRevisions = new MaterialRevisions(); //will not be used, as no new materials have appeared
        MaterialRevisions revisionsSince = materialChecker.findRevisionsSince(alreadyFoundRevisions, new Materials(dependencyMaterial, newPkgMaterial), new MaterialRevisions(previousDependantRevision, previousPkgRevision), latestRevisions);

        assertThat(revisionsSince, is(new MaterialRevisions(new MaterialRevision(dependencyMaterial, dependencyModification), new MaterialRevision(oldPkgMaterial, newPkgMod))));
        // since name is not part of equals
        assertThat(((PackageMaterial)revisionsSince.getMaterialRevision(1).getMaterial()).getPackageDefinition().getName(), is("pkg-new-name"));
        assertThat(((PackageMaterial)revisionsSince.getMaterialRevision(1).getMaterial()).getPackageDefinition().getRepository().getName(), is("repo-new-name"));

        Mockito.verify(materialRepository, never()).findLatestModification(dependencyMaterial);
        Mockito.verify(materialRepository).findModificationsSince(oldPkgMaterial, previousPkgRevision);
    }

    @Test
    public void shouldNOTSkipFindingRevisionsSinceForMaterialsThatAreNewlyAdded() throws Exception {
        SvnMaterial svnMaterial = new SvnMaterial("svnUrl", null, null, false);
        SvnMaterial svnExternalMaterial = new SvnMaterial("svnExternalUrl", null, null, false);

        Modification svnExternalModification = new Modification("user", "external commit", "em@il", new Date(), "3");

        MaterialRevision previousSvnRevision = new MaterialRevision(svnMaterial, mod(1L));
        Modification svnModification = new Modification("user", "commend", "em@il", new Date(), "2");

        MaterialRevisions latestRevisions = new MaterialRevisions(new MaterialRevision(svnMaterial, svnModification), new MaterialRevision(svnExternalMaterial, svnExternalModification));

        Mockito.when(materialRepository.findModificationsSince(svnMaterial, previousSvnRevision)).thenReturn(modifications(svnModification));
        MaterialRevisions revisionsSince = materialChecker.findRevisionsSince(new MaterialRevisions(), new Materials(svnMaterial, svnExternalMaterial), new MaterialRevisions(previousSvnRevision), latestRevisions);
        assertThat(revisionsSince, is(new MaterialRevisions(new MaterialRevision(svnMaterial, svnModification), new MaterialRevision(svnExternalMaterial, svnExternalModification))));

        Mockito.verify(materialRepository).findModificationsSince(svnMaterial, previousSvnRevision);
    }

    @Test
    public void updateChangedRevisionsShouldFilterRevisionsThatHaveBuiltBefore() {
        CaseInsensitiveString pipelineName = new CaseInsensitiveString("pipelineName");
        GitMaterial gitMaterial = new GitMaterial("git://foo");
        BuildCause buildCause = BuildCause.createWithModifications(new MaterialRevisions(new MaterialRevision(gitMaterial, mod(10L), mod(9L), mod(8L))), "user");
        when(materialRepository.latestModificationRunByPipeline(pipelineName, gitMaterial)).thenReturn(9L);
        materialChecker.updateChangedRevisions(pipelineName, buildCause);

        MaterialRevisions actualRevisions = buildCause.getMaterialRevisions();
        assertThat(actualRevisions.getModifications(gitMaterial), is(new Modifications(mod(10L))));
        assertThat(actualRevisions.findRevisionFor(gitMaterial).isChanged(), is(true));
    }

    @Test
    public void updateChangedRevisionsShouldRetainLatestRevisionIfAllHaveBuiltBefore() {
        CaseInsensitiveString pipelineName = new CaseInsensitiveString("pipelineName");
        GitMaterial gitMaterial = new GitMaterial("git://foo");
        BuildCause buildCause = BuildCause.createWithModifications(new MaterialRevisions(new MaterialRevision(gitMaterial, mod(10L), mod(9L), mod(8L))), "user");
        when(materialRepository.latestModificationRunByPipeline(pipelineName, gitMaterial)).thenReturn(10L);
        materialChecker.updateChangedRevisions(pipelineName, buildCause);

        MaterialRevisions actualRevisions = buildCause.getMaterialRevisions();
        assertThat(actualRevisions.getModifications(gitMaterial), is(new Modifications(mod(10L), mod(9L), mod(8L))));
        assertThat(actualRevisions.findRevisionFor(gitMaterial).isChanged(), is(false));
    }

    private Modification mod(final Long revision) {
        Modification modification = new Modification("user", "comment", "em@il", new Date(12121), revision.toString());
        modification.setId(revision);
        return modification;
    }
}
