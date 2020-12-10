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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.exceptions.BadRequestException;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.materials.PackageMaterial;
import com.thoughtworks.go.config.materials.PluggableSCMMaterial;
import com.thoughtworks.go.config.materials.SubprocessExecutionContext;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.config.materials.mercurial.HgMaterial;
import com.thoughtworks.go.config.materials.perforce.P4Material;
import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.config.materials.tfs.TfsMaterial;
import com.thoughtworks.go.domain.MaterialInstance;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.PipelineRunIdInfo;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.materials.*;
import com.thoughtworks.go.domain.materials.git.GitMaterialInstance;
import com.thoughtworks.go.domain.materials.packagematerial.PackageMaterialRevision;
import com.thoughtworks.go.domain.materials.scm.PluggableSCMMaterialRevision;
import com.thoughtworks.go.domain.packagerepository.PackageDefinition;
import com.thoughtworks.go.domain.packagerepository.PackageRepositoryMother;
import com.thoughtworks.go.helper.MaterialsMother;
import com.thoughtworks.go.helper.ModificationsMother;
import com.thoughtworks.go.plugin.access.packagematerial.PackageRepositoryExtension;
import com.thoughtworks.go.plugin.access.scm.SCMExtension;
import com.thoughtworks.go.plugin.access.scm.SCMPropertyConfiguration;
import com.thoughtworks.go.plugin.access.scm.material.MaterialPollResult;
import com.thoughtworks.go.plugin.access.scm.revision.SCMRevision;
import com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration;
import com.thoughtworks.go.plugin.api.material.packagerepository.PackageRevision;
import com.thoughtworks.go.plugin.api.material.packagerepository.RepositoryConfiguration;
import com.thoughtworks.go.server.dao.FeedModifier;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.service.materials.GitPoller;
import com.thoughtworks.go.server.service.materials.MaterialPoller;
import com.thoughtworks.go.server.service.materials.PluggableSCMMaterialPoller;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.server.util.Pagination;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.domain.packagerepository.PackageDefinitionMother.create;
import static com.thoughtworks.go.helper.MaterialConfigsMother.git;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(Theories.class)
public class MaterialServiceTest {
    private static List MODIFICATIONS = new ArrayList<Modification>();

    @Mock
    private MaterialRepository materialRepository;
    @Mock
    private GoConfigService goConfigService;
    @Mock
    private SecurityService securityService;
    @Mock
    private PackageRepositoryExtension packageRepositoryExtension;
    @Mock
    private SCMExtension scmExtension;
    @Mock
    private TransactionTemplate transactionTemplate;
    @Mock
    private SecretParamResolver secretParamResolver;

    private MaterialService materialService;

    @Before
    public void setUp() {
        initMocks(this);
        materialService = new MaterialService(materialRepository, goConfigService, securityService,
                packageRepositoryExtension, scmExtension, transactionTemplate, secretParamResolver);
    }

    @Test
    public void shouldUnderstandIfMaterialHasModifications() {
        assertHasModification(new MaterialRevisions(new MaterialRevision(new HgMaterial("foo.com", null), new Modification(new Date(), "2", "MOCK_LABEL-12", null))), true);
        assertHasModification(new MaterialRevisions(), false);
    }

    @Test
    public void shouldNotBeAuthorizedToViewAPipeline() {
        Username pavan = Username.valueOf("pavan");
        when(securityService.hasViewPermissionForPipeline(pavan, "pipeline")).thenReturn(false);
        LocalizedOperationResult operationResult = mock(LocalizedOperationResult.class);
        materialService.searchRevisions("pipeline", "sha", "search-string", pavan, operationResult);
        verify(operationResult).forbidden(EntityType.Pipeline.forbiddenToView("pipeline", pavan.getUsername()), HealthStateType.general(HealthStateScope.forPipeline("pipeline")));
    }

    @Test
    public void shouldReturnTheRevisionsThatMatchTheGivenSearchString() {
        Username pavan = Username.valueOf("pavan");
        when(securityService.hasViewPermissionForPipeline(pavan, "pipeline")).thenReturn(true);
        LocalizedOperationResult operationResult = mock(LocalizedOperationResult.class);
        MaterialConfig materialConfig = mock(MaterialConfig.class);
        when(goConfigService.materialForPipelineWithFingerprint("pipeline", "sha")).thenReturn(materialConfig);

        List<MatchedRevision> expected = asList(new MatchedRevision("23", "revision", "revision", "user", new DateTime(2009, 10, 10, 12, 0, 0, 0).toDate(), "comment"));
        when(materialRepository.findRevisionsMatching(materialConfig, "23")).thenReturn(expected);
        assertThat(materialService.searchRevisions("pipeline", "sha", "23", pavan, operationResult), is(expected));
    }

    @Test
    public void shouldReturnNotFoundIfTheMaterialDoesNotBelongToTheGivenPipeline() {
        Username pavan = Username.valueOf("pavan");
        when(securityService.hasViewPermissionForPipeline(pavan, "pipeline")).thenReturn(true);
        LocalizedOperationResult operationResult = mock(LocalizedOperationResult.class);

        when(goConfigService.materialForPipelineWithFingerprint("pipeline", "sha")).thenThrow(new RuntimeException("Not found"));

        materialService.searchRevisions("pipeline", "sha", "23", pavan, operationResult);
        verify(operationResult).notFound("Pipeline '" + "pipeline" + "' does not contain material with fingerprint '" + "sha" + "'.", HealthStateType.general(HealthStateScope.forPipeline("pipeline")));
    }

    @DataPoint
    public static RequestDataPoints GIT_LATEST_MODIFICATIONS = new RequestDataPoints(new GitMaterial("url") {
        @Override
        public List<Modification> latestModification(File baseDir, SubprocessExecutionContext execCtx) {
            return (List<Modification>) MODIFICATIONS;
        }

        @Override
        public GitMaterial withShallowClone(boolean value) {
            return this;
        }

        @Override
        public List<Modification> modificationsSince(File baseDir, Revision revision, SubprocessExecutionContext execCtx) {
            return (List<Modification>) MODIFICATIONS;
        }
    }, GitMaterial.class);

    @DataPoint
    public static RequestDataPoints SVN_LATEST_MODIFICATIONS = new RequestDataPoints(new SvnMaterial("url", "username", "password", true) {
        @Override
        public List<Modification> latestModification(File baseDir, SubprocessExecutionContext execCtx) {
            return (List<Modification>) MODIFICATIONS;
        }

        @Override
        public List<Modification> modificationsSince(File baseDir, Revision revision, SubprocessExecutionContext execCtx) {
            return (List<Modification>) MODIFICATIONS;
        }
    }, SvnMaterial.class);

    @DataPoint
    public static RequestDataPoints HG_LATEST_MODIFICATIONS = new RequestDataPoints(new HgMaterial("url", null) {
        @Override
        public List<Modification> latestModification(File baseDir, SubprocessExecutionContext execCtx) {
            return (List<Modification>) MODIFICATIONS;
        }

        @Override
        public List<Modification> modificationsSince(File baseDir, Revision revision, SubprocessExecutionContext execCtx) {
            return (List<Modification>) MODIFICATIONS;
        }
    }, HgMaterial.class);

    @DataPoint
    public static RequestDataPoints TFS_LATEST_MODIFICATIONS = new RequestDataPoints(new TfsMaterial() {
        @Override
        public List<Modification> latestModification(File baseDir, SubprocessExecutionContext execCtx) {
            return (List<Modification>) MODIFICATIONS;
        }

        @Override
        public List<Modification> modificationsSince(File baseDir, Revision revision, SubprocessExecutionContext execCtx) {
            return (List<Modification>) MODIFICATIONS;
        }

    }, TfsMaterial.class);

    @DataPoint
    public static RequestDataPoints P4_LATEST_MODIFICATIONS = new RequestDataPoints(new P4Material("url", "view", "user") {
        @Override
        public List<Modification> latestModification(File baseDir, SubprocessExecutionContext execCtx) {
            return (List<Modification>) MODIFICATIONS;
        }

        @Override
        public List<Modification> modificationsSince(File baseDir, Revision revision, SubprocessExecutionContext execCtx) {
            return (List<Modification>) MODIFICATIONS;
        }
    }, P4Material.class);

    @DataPoint
    public static RequestDataPoints DEPENDENCY_LATEST_MODIFICATIONS = new RequestDataPoints(new DependencyMaterial(new CaseInsensitiveString("p1"), new CaseInsensitiveString("s1")) {
        @Override
        public List<Modification> latestModification(File baseDir, SubprocessExecutionContext execCtx) {
            return (List<Modification>) MODIFICATIONS;
        }

        @Override
        public List<Modification> modificationsSince(File baseDir, Revision revision, SubprocessExecutionContext execCtx) {
            return (List<Modification>) MODIFICATIONS;
        }
    }, DependencyMaterial.class);


    @Theory
    public void shouldGetLatestModificationsForGivenMaterial(RequestDataPoints data) {
        MaterialService spy = spy(materialService);
        SubprocessExecutionContext execCtx = mock(SubprocessExecutionContext.class);
        doReturn(data.klass).when(spy).getMaterialClass(data.material);
        List<Modification> actual = spy.latestModification(data.material, null, execCtx);
        assertThat(actual, is(MODIFICATIONS));
    }

    @Theory
    public void shouldGetModificationsSinceARevisionForGivenMaterial(RequestDataPoints data) {
        Revision revision = mock(Revision.class);
        SubprocessExecutionContext execCtx = mock(SubprocessExecutionContext.class);
        MaterialService spy = spy(materialService);
        doReturn(data.klass).when(spy).getMaterialClass(data.material);
        List<Modification> actual = spy.modificationsSince(data.material, null, revision, execCtx);
        assertThat(actual, is(MODIFICATIONS));
    }

    @Theory
    public void shouldCheckoutAGivenRevision(RequestDataPoints data) {
        Revision revision = mock(Revision.class);
        MaterialPoller materialPoller = mock(MaterialPoller.class);
        MaterialService spy = spy(materialService);
        File baseDir = mock(File.class);
        SubprocessExecutionContext execCtx = mock(SubprocessExecutionContext.class);

        doReturn(data.klass).when(spy).getMaterialClass(data.material);
        doReturn(materialPoller).when(spy).getPollerImplementation(data.material);

        spy.checkout(data.material, baseDir, revision, execCtx);

        verify(materialPoller).checkout(data.material, baseDir, revision, execCtx);
    }

    @Test
    public void shouldThrowExceptionWhenPollerForMaterialNotFound() {
        try {
            materialService.latestModification(mock(Material.class), null, null);
            fail("Should have thrown up");
        } catch (RuntimeException e) {
            assertThat(e.getMessage(), is("unknown material type null"));
        }
    }

    @Test
    public void latestModification_shouldResolveSecretsForMaterialConfiguredWithSecretParams() {
        GitMaterial gitMaterial = spy(new GitMaterial("https://example.com"));
        MaterialService spy = spy(materialService);
        GitPoller gitPoller = mock(GitPoller.class);

        doReturn(GitMaterial.class).when(spy).getMaterialClass(gitMaterial);
        doReturn(true).when(gitMaterial).hasSecretParams();
        doReturn(gitPoller).when(spy).getPollerImplementation(gitMaterial);
        when(gitPoller.latestModification(any(), any(), any())).thenReturn(new ArrayList<>());

        spy.latestModification(gitMaterial, null, null);

        verify(secretParamResolver).resolve(gitMaterial);
    }

    @Test
    public void modificationsSince_shouldResolveSecretsForMaterialConfiguredWithSecretParams() {
        GitMaterial gitMaterial = spy(new GitMaterial("https://example.com"));
        MaterialService spy = spy(materialService);
        GitPoller gitPoller = mock(GitPoller.class);
        Class<GitMaterial> toBeReturned = GitMaterial.class;

        doReturn(toBeReturned).when(spy).getMaterialClass(gitMaterial);
        doReturn(true).when(gitMaterial).hasSecretParams();
        doReturn(gitPoller).when(spy).getPollerImplementation(gitMaterial);
        when(gitPoller.modificationsSince(any(), any(), any(), any())).thenReturn(new ArrayList<>());

        spy.modificationsSince(gitMaterial, null, null, null);

        verify(secretParamResolver).resolve(gitMaterial);
    }

    @Test
    public void shouldGetLatestModificationForPackageMaterial() {
        PackageMaterial material = new PackageMaterial();
        PackageDefinition packageDefinition = create("id", "package", new Configuration(), PackageRepositoryMother.create("id", "name", "plugin-id", "plugin-version", new Configuration()));
        material.setPackageDefinition(packageDefinition);


        when(packageRepositoryExtension.getLatestRevision(eq("plugin-id"),
                any(PackageConfiguration.class),
                any(RepositoryConfiguration.class))).thenReturn(new PackageRevision("blah-123", new Date(), "user"));


        List<Modification> modifications = materialService.latestModification(material, null, null);
        assertThat(modifications.get(0).getRevision(), is("blah-123"));
    }

    @Test
    public void shouldGetModificationSinceAGivenRevision() {
        PackageMaterial material = new PackageMaterial();
        PackageDefinition packageDefinition = create("id", "package", new Configuration(), PackageRepositoryMother.create("id", "name", "plugin-id", "plugin-version", new Configuration()));
        material.setPackageDefinition(packageDefinition);

        when(packageRepositoryExtension.latestModificationSince(eq("plugin-id"),
                any(PackageConfiguration.class),
                any(RepositoryConfiguration.class),
                any(PackageRevision.class))).thenReturn(new PackageRevision("new-revision-456", new Date(), "user"));
        List<Modification> modifications = materialService.modificationsSince(material, null, new PackageMaterialRevision("revision-124", new Date()), null);
        assertThat(modifications.get(0).getRevision(), is("new-revision-456"));
    }

    @Test
    public void shouldGetLatestModification_PluggableSCMMaterial() {
        PluggableSCMMaterial pluggableSCMMaterial = MaterialsMother.pluggableSCMMaterial();
        MaterialInstance materialInstance = pluggableSCMMaterial.createMaterialInstance();
        when(materialRepository.findMaterialInstance(any(Material.class))).thenReturn(materialInstance);
        MaterialPollResult materialPollResult = new MaterialPollResult(null, new SCMRevision("blah-123", new Date(), "user", "comment", null, null));
        when(scmExtension.getLatestRevision(any(String.class), any(SCMPropertyConfiguration.class), any(Map.class), any(String.class))).thenReturn(materialPollResult);

        List<Modification> modifications = materialService.latestModification(pluggableSCMMaterial, new File("/tmp/flyweight"), null);

        assertThat(modifications.get(0).getRevision(), is("blah-123"));
    }

    @Test
    public void shouldGetModificationSince_PluggableSCMMaterial() {
        PluggableSCMMaterial pluggableSCMMaterial = MaterialsMother.pluggableSCMMaterial();
        MaterialInstance materialInstance = pluggableSCMMaterial.createMaterialInstance();
        when(materialRepository.findMaterialInstance(any(Material.class))).thenReturn(materialInstance);
        MaterialPollResult materialPollResult = new MaterialPollResult(null, asList(new SCMRevision("new-revision-456", new Date(), "user", "comment", null, null)));
        when(scmExtension.latestModificationSince(any(String.class), any(SCMPropertyConfiguration.class), any(Map.class), any(String.class),
                any(SCMRevision.class))).thenReturn(materialPollResult);

        PluggableSCMMaterialRevision previouslyKnownRevision = new PluggableSCMMaterialRevision("revision-124", new Date());
        List<Modification> modifications = materialService.modificationsSince(pluggableSCMMaterial, new File("/tmp/flyweight"), previouslyKnownRevision, null);

        assertThat(modifications.get(0).getRevision(), is("new-revision-456"));
    }

    @Test
    public void shouldDelegateToMaterialRepository_getTotalModificationsFor() {
        GitMaterialConfig materialConfig = git("http://test.com");
        GitMaterialInstance gitMaterialInstance = new GitMaterialInstance("http://test.com", null, null, null, "flyweight");

        when(materialRepository.findMaterialInstance(materialConfig)).thenReturn(gitMaterialInstance);

        when(materialRepository.getTotalModificationsFor(gitMaterialInstance)).thenReturn(1L);

        Long totalCount = materialService.getTotalModificationsFor(materialConfig);

        assertThat(totalCount, is(1L));
    }

    @Test
    public void shouldDelegateToMaterialRepository_getModificationsFor() {
        GitMaterialConfig materialConfig = git("http://test.com");
        GitMaterialInstance gitMaterialInstance = new GitMaterialInstance("http://test.com", null, null, null, "flyweight");
        Pagination pagination = Pagination.pageStartingAt(0, 10, 10);
        Modifications modifications = new Modifications();
        modifications.add(new Modification("user", "comment", "email", new Date(), "revision"));

        when(materialRepository.findMaterialInstance(materialConfig)).thenReturn(gitMaterialInstance);

        when(materialRepository.getModificationsFor(gitMaterialInstance, pagination)).thenReturn(modifications);

        Modifications gotModifications = materialService.getModificationsFor(materialConfig, pagination);

        assertThat(gotModifications, is(modifications));
    }

    @Test
    public void shouldGetLatestModificationWithMaterial() {
        MaterialInstance instance = MaterialsMother.gitMaterial("http://example.com/gocd.git").createMaterialInstance();
        Modification modification = ModificationsMother.withModifiedFileWhoseNameLengthIsOneK();
        modification.setMaterialInstance(instance);
        ArrayList<Modification> mods = new ArrayList<>();
        mods.add(modification);

        when(materialRepository.getLatestModificationForEachMaterial()).thenReturn(mods);

        Map<String, Modification> modificationsMap = materialService.getLatestModificationForEachMaterial();

        assertEquals(modificationsMap.size(), 1);
        assertThat(modificationsMap.keySet(), containsInAnyOrder(instance.getFingerprint()));
        assertEquals(modificationsMap.get(instance.getFingerprint()), modification);
    }

    @Test
    public void shouldReturnEmptyMapIfNoMaterialAndModificationFound() {
        when(materialRepository.getLatestModificationForEachMaterial()).thenReturn(emptyList());

        Map<String, Modification> modificationsMap = materialService.getLatestModificationForEachMaterial();

        assertEquals(modificationsMap.size(), 0);
    }

    @Test
    public void history_shouldCallDaoToFetchLatestModificationData() {
        GitMaterialConfig materialConfig = git("http://test.com");
        GitMaterialInstance gitMaterialInstance = new GitMaterialInstance("http://test.com", null, null, null, "flyweight");
        Modifications modifications = new Modifications();
        modifications.add(new Modification("user", "comment 1", "email", new DateTime().minusHours(1).toDate(), "revision"));
        modifications.add(new Modification("user", "comment 2", "email", new DateTime().minusHours(2).toDate(), "revision"));
        modifications.add(new Modification("user", "comment 3", "email", new DateTime().minusHours(3).toDate(), "revision"));

        when(materialRepository.findMaterialInstance(materialConfig)).thenReturn(gitMaterialInstance);
        when(materialRepository.loadHistory(anyLong(), any(), anyLong(), anyInt())).thenReturn(modifications);

        List<Modification> gotModifications = materialService.getModificationsFor(materialConfig, "", 0, 0, 3);

        verify(materialRepository).loadHistory(anyLong(), eq(FeedModifier.Latest), eq(0L), eq(3));
        assertThat(gotModifications, is(modifications));
    }

    @Test
    public void history_shouldCallDaoToFetchModificationDataAfterTheGivenCursor() {
        GitMaterialConfig materialConfig = git("http://test.com");
        GitMaterialInstance gitMaterialInstance = new GitMaterialInstance("http://test.com", null, null, null, "flyweight");
        Modifications modifications = new Modifications();
        modifications.add(new Modification("user", "comment 1", "email", new DateTime().minusHours(1).toDate(), "revision"));

        when(materialRepository.findMaterialInstance(materialConfig)).thenReturn(gitMaterialInstance);
        when(materialRepository.loadHistory(anyLong(), any(), anyLong(), anyInt())).thenReturn(modifications);

        List<Modification> gotModifications = materialService.getModificationsFor(materialConfig, "", 2, 0, 3);

        verify(materialRepository).loadHistory(anyLong(), eq(FeedModifier.After), eq(2L), eq(3));
    }

    @Test
    public void history_shouldCallDaoToFetchModificationDataBeforeTheGivenCursor() {
        GitMaterialConfig materialConfig = git("http://test.com");
        GitMaterialInstance gitMaterialInstance = new GitMaterialInstance("http://test.com", null, null, null, "flyweight");
        Modifications modifications = new Modifications();
        modifications.add(new Modification("user", "comment 1", "email", new DateTime().minusHours(1).toDate(), "revision"));

        when(materialRepository.findMaterialInstance(materialConfig)).thenReturn(gitMaterialInstance);
        when(materialRepository.loadHistory(anyLong(), any(), anyLong(), anyInt())).thenReturn(modifications);

        List<Modification> gotModifications = materialService.getModificationsFor(materialConfig, "", 0, 2, 3);

        verify(materialRepository).loadHistory(anyLong(), eq(FeedModifier.Before), eq(2L), eq(3));
    }

    @Test
    public void history_shouldThrowIfTheAfterCursorIsInvalid() {
        GitMaterialConfig materialConfig = git("http://test.com");
        GitMaterialInstance gitMaterialInstance = new GitMaterialInstance("http://test.com", null, null, null, "flyweight");

        when(materialRepository.findMaterialInstance(materialConfig)).thenReturn(gitMaterialInstance);

        assertThatCode(() -> materialService.getModificationsFor(materialConfig, "", -10, 0, 3))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("The query parameter 'after', if specified, must be a positive integer.");

        verify(materialRepository).findMaterialInstance(materialConfig);
        verifyNoMoreInteractions(materialRepository);
    }

    @Test
    public void history_shouldThrowIfTheBeforeCursorIsInvalid() {
        GitMaterialConfig materialConfig = git("http://test.com");
        GitMaterialInstance gitMaterialInstance = new GitMaterialInstance("http://test.com", null, null, null, "flyweight");

        when(materialRepository.findMaterialInstance(materialConfig)).thenReturn(gitMaterialInstance);

        assertThatCode(() -> materialService.getModificationsFor(materialConfig, "", 0, -10, 3))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("The query parameter 'before', if specified, must be a positive integer.");

        verify(materialRepository).findMaterialInstance(materialConfig);
        verifyNoMoreInteractions(materialRepository);
    }

    @Test
    public void shouldCallDaoToFetchLatestAndOlderModification() {
        GitMaterialConfig materialConfig = git("http://test.com");
        GitMaterialInstance gitMaterialInstance = new GitMaterialInstance("http://test.com", null, null, null, "flyweight");
        PipelineRunIdInfo value = new PipelineRunIdInfo(1, 2);

        when(materialRepository.findMaterialInstance(materialConfig)).thenReturn(gitMaterialInstance);
        when(materialRepository.getOldestAndLatestModificationId(anyLong(), anyString())).thenReturn(value);

        PipelineRunIdInfo info = materialService.getLatestAndOldestModification(materialConfig, "");

        verify(materialRepository).getOldestAndLatestModificationId(anyLong(), eq(""));
        assertThat(info, is(value));
    }

    @Test
    public void shouldReturnNullIfNoInstanceIsPresent() {
        GitMaterialConfig materialConfig = git("http://test.com");

        when(materialRepository.findMaterialInstance(materialConfig)).thenReturn(null);

        PipelineRunIdInfo info = materialService.getLatestAndOldestModification(materialConfig, "");

        verify(materialRepository, never()).getOldestAndLatestModificationId(anyLong(), anyString());
        assertThat(info, is(nullValue()));
    }

    @Test
    public void findMatchingMods_shouldCallDaoToFetchLatestMatchingMods() {
        GitMaterialConfig config = git("http://test.com");
        GitMaterialInstance instance = new GitMaterialInstance("http://test.com", null, null, null, "flyweight");
        Modifications modifications = new Modifications();
        modifications.add(new Modification("user", "comment 1", "email", new DateTime().minusHours(1).toDate(), "revision"));
        modifications.add(new Modification("user", "comment 2", "email", new DateTime().minusHours(2).toDate(), "revision"));
        modifications.add(new Modification("user", "comment 3", "email", new DateTime().minusHours(3).toDate(), "revision"));

        when(materialRepository.findMaterialInstance(config)).thenReturn(instance);
        when(materialRepository.findMatchingModifications(anyLong(), anyString(), any(FeedModifier.class), anyLong(), anyInt())).thenReturn(modifications);

        List<Modification> result = materialService.getModificationsFor(config, "comment", 0, 0, 10);

        verify(materialRepository).findMatchingModifications(eq(instance.getId()), eq("comment"), eq(FeedModifier.Latest), eq(0L), eq(10));
        assertThat(result, is(modifications));
    }

    @Test
    public void findMatchingMods_shouldCallDaoToFetchMatchingModsAfterCursor() {
        GitMaterialConfig config = git("http://test.com");
        GitMaterialInstance instance = new GitMaterialInstance("http://test.com", null, null, null, "flyweight");
        Modifications modifications = new Modifications();
        modifications.add(new Modification("user", "comment 1", "email", new DateTime().minusHours(1).toDate(), "revision"));
        modifications.add(new Modification("user", "comment 2", "email", new DateTime().minusHours(2).toDate(), "revision"));
        modifications.add(new Modification("user", "comment 3", "email", new DateTime().minusHours(3).toDate(), "revision"));

        when(materialRepository.findMaterialInstance(config)).thenReturn(instance);
        when(materialRepository.findMatchingModifications(anyLong(), anyString(), any(FeedModifier.class), anyLong(), anyInt())).thenReturn(modifications);

        List<Modification> result = materialService.getModificationsFor(config, "comment", 3, 0, 10);

        verify(materialRepository).findMatchingModifications(eq(instance.getId()), eq("comment"), eq(FeedModifier.After), eq(3L), eq(10));
        assertThat(result, is(modifications));
    }

    @Test
    public void findMatchingMods_shouldCallDaoToFetchMatchingModsBeforeCursor() {
        GitMaterialConfig config = git("http://test.com");
        GitMaterialInstance instance = new GitMaterialInstance("http://test.com", null, null, null, "flyweight");
        Modifications modifications = new Modifications();
        modifications.add(new Modification("user", "comment 1", "email", new DateTime().minusHours(1).toDate(), "revision"));
        modifications.add(new Modification("user", "comment 2", "email", new DateTime().minusHours(2).toDate(), "revision"));
        modifications.add(new Modification("user", "comment 3", "email", new DateTime().minusHours(3).toDate(), "revision"));

        when(materialRepository.findMaterialInstance(config)).thenReturn(instance);
        when(materialRepository.findMatchingModifications(anyLong(), anyString(), any(FeedModifier.class), anyLong(), anyInt())).thenReturn(modifications);

        List<Modification> result = materialService.getModificationsFor(config, "comment", 0, 3, 10);

        verify(materialRepository).findMatchingModifications(eq(instance.getId()), eq("comment"), eq(FeedModifier.Before), eq(3L), eq(10));
        assertThat(result, is(modifications));
    }

    @Test
    public void findMatchingMods_shouldReturnNullIfMaterialIsNotPresent() {
        GitMaterialConfig material = git("http://test.com");

        when(materialRepository.findMaterialInstance(material)).thenReturn(null);

        List<Modification> result = materialService.getModificationsFor(material, "comment", 0, 0, 10);

        assertThat(result, is(nullValue()));
        verify(materialRepository).findMaterialInstance(material);
        verifyNoMoreInteractions(materialRepository);
    }

    private void assertHasModification(MaterialRevisions materialRevisions, boolean b) {
        HgMaterial hgMaterial = new HgMaterial("foo.com", null);
        when(materialRepository.findLatestModification(hgMaterial)).thenReturn(materialRevisions);
        assertThat(materialService.hasModificationFor(hgMaterial), is(b));
    }

    private static class RequestDataPoints<T extends Material> {
        final T material;
        final Class klass;

        public RequestDataPoints(T material, Class klass) {
            this.material = material;
            this.klass = klass;
        }
    }

    @Test
    public void latestModification_shouldResolveSecretsForPluggableScmMaterial() {
        PluggableSCMMaterial pluggableSCMMaterial = spy(new PluggableSCMMaterial());
        MaterialService serviceSpy = spy(materialService);
        PluggableSCMMaterialPoller poller = mock(PluggableSCMMaterialPoller.class);

        doReturn(PluggableSCMMaterial.class).when(serviceSpy).getMaterialClass(pluggableSCMMaterial);
        doReturn(true).when(pluggableSCMMaterial).hasSecretParams();
        doReturn(poller).when(serviceSpy).getPollerImplementation(pluggableSCMMaterial);
        when(poller.latestModification(any(), any(), any())).thenReturn(new ArrayList<>());

        serviceSpy.latestModification(pluggableSCMMaterial, null, null);

        verify(secretParamResolver).resolve(pluggableSCMMaterial);
    }

    @Test
    public void modificationsSince_shouldResolveSecretsForPluggableScmMaterial() {
        PluggableSCMMaterial pluggableSCMMaterial = spy(new PluggableSCMMaterial());
        MaterialService serviceSpy = spy(materialService);
        PluggableSCMMaterialPoller poller = mock(PluggableSCMMaterialPoller.class);

        doReturn(PluggableSCMMaterial.class).when(serviceSpy).getMaterialClass(pluggableSCMMaterial);
        doReturn(true).when(pluggableSCMMaterial).hasSecretParams();
        doReturn(poller).when(serviceSpy).getPollerImplementation(pluggableSCMMaterial);
        when(poller.latestModification(any(), any(), any())).thenReturn(new ArrayList<>());

        serviceSpy.modificationsSince(pluggableSCMMaterial, null, null, null);

        verify(secretParamResolver).resolve(pluggableSCMMaterial);
    }

    @Test
    public void checkout_shouldResolveSecretsForPluggableScmMaterial() {
        PluggableSCMMaterial pluggableSCMMaterial = spy(new PluggableSCMMaterial());
        MaterialService serviceSpy = spy(materialService);
        PluggableSCMMaterialPoller poller = mock(PluggableSCMMaterialPoller.class);

        doReturn(PluggableSCMMaterial.class).when(serviceSpy).getMaterialClass(pluggableSCMMaterial);
        doReturn(true).when(pluggableSCMMaterial).hasSecretParams();
        doReturn(poller).when(serviceSpy).getPollerImplementation(pluggableSCMMaterial);
        when(poller.latestModification(any(), any(), any())).thenReturn(new ArrayList<>());

        serviceSpy.checkout(pluggableSCMMaterial, null, null, null);

        verify(secretParamResolver).resolve(pluggableSCMMaterial);
    }
}
