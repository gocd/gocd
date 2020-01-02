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
package com.thoughtworks.go.server.materials.postcommit.svn;

import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.config.materials.mercurial.HgMaterial;
import com.thoughtworks.go.config.materials.perforce.P4Material;
import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.config.materials.tfs.TfsMaterial;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.svn.SvnCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SvnPostCommitHookImplementerTest {

    private SvnPostCommitHookImplementer implementer;

    @BeforeEach
    void setUp() {
        implementer = new SvnPostCommitHookImplementer();
    }

    @Test
    void shouldPruneListToGiveOutOnlySvnMaterials() {
        final Material svnMaterial1 = mock(SvnMaterial.class);
        final Material svnMaterial2 = mock(SvnMaterial.class);
        final Material svnMaterial3 = mock(SvnMaterial.class);
        final Material hgMaterial = mock(HgMaterial.class);
        final Material gitMaterial = mock(GitMaterial.class);
        final Material p4Material = mock(P4Material.class);
        final Material tfsMaterial = mock(TfsMaterial.class);
        final Material dependencyMaterial = mock(DependencyMaterial.class);
        final HashSet<Material> allMaterials = new HashSet<>(Arrays.asList(svnMaterial1, svnMaterial2, svnMaterial3, gitMaterial, hgMaterial, p4Material, tfsMaterial, dependencyMaterial));
        final SvnPostCommitHookImplementer spy = spy(implementer);
        doAnswer(invocation -> Boolean.TRUE).when(spy).isQualified(anyString(), any(SvnMaterial.class), any(HashMap.class));
        doAnswer(invocation -> new HashMap()).when(spy).createUrlToRemoteUUIDMap(allMaterials);
        final HashMap params = new HashMap();
        params.put(SvnPostCommitHookImplementer.UUID, "some uuid");
        final Set<Material> prunedList = spy.prune(allMaterials, params);

        assertThat(prunedList.size()).isEqualTo(3);
        assertThat(prunedList).contains(svnMaterial1, svnMaterial2, svnMaterial3);
    }

    @Test
    void shouldPruneListToGiveOutOnlySvnMaterialsWhichMatchTheRepositoryUUID() {
        final SvnMaterial svnMaterial1 = mock(SvnMaterial.class);
        final SvnMaterial svnMaterial2 = mock(SvnMaterial.class);
        final SvnMaterial svnMaterial3 = mock(SvnMaterial.class);
        final HashSet<Material> allMaterials = new HashSet<>(Arrays.asList(svnMaterial1, svnMaterial2, svnMaterial3));
        final HashMap<Object, Object> params = new HashMap<>();
        final String uuid = "12345";
        params.put(SvnPostCommitHookImplementer.UUID, uuid);

        final SvnPostCommitHookImplementer spy = spy(implementer);
        final HashMap<String, String> map = new HashMap<>();
        doAnswer(invocation -> {
            map.put("url1", "12345");
            map.put("url2", "54321");
            return map;
        }).when(spy).createUrlToRemoteUUIDMap(allMaterials);
        doAnswer(invocation -> Boolean.FALSE).when(spy).isQualified(uuid, svnMaterial1, map);
        doAnswer(invocation -> Boolean.TRUE).when(spy).isQualified(uuid, svnMaterial2, map);
        doAnswer(invocation -> Boolean.FALSE).when(spy).isQualified(uuid, svnMaterial3, map);

        final Set<Material> prunedList = spy.prune(allMaterials, params);

        assertThat(prunedList.size()).isEqualTo(1);
        verify(spy, times(1)).createUrlToRemoteUUIDMap(allMaterials);
    }

    @Test
    void shouldPruneListToGiveOutOnlySvnMaterialsWhichMatchTheRepositoryUrl() {
        SvnMaterial svnMaterial1 = new SvnMaterial("http://user:pass@example.com/svn1", "user", "pass", true);
        SvnMaterial svnMaterial2 = new SvnMaterial("http://admin@example.com/svn2", "admin", "discreetbluewhale", true);
        GitMaterial gitMaterial = new GitMaterial("http://admin@example.com/svn2");
        final HashSet<Material> allMaterials = new HashSet<>(Arrays.asList(svnMaterial1, svnMaterial2, gitMaterial));
        final HashMap<Object, Object> params = new HashMap<>();
        final SvnPostCommitHookImplementer svnPostCommitHookImplementer = new SvnPostCommitHookImplementer();
        params.put(SvnPostCommitHookImplementer.REPO_URL_PARAM_KEY, "http://example.com/svn1");
        Set<Material> prunedList = svnPostCommitHookImplementer.prune(allMaterials, params);

        assertThat(prunedList).contains(svnMaterial1);
        assertThat(prunedList).hasSize(1);

        params.put(SvnPostCommitHookImplementer.REPO_URL_PARAM_KEY, "http://admin@example.com/svn2");
        prunedList = svnPostCommitHookImplementer.prune(allMaterials, params);

        assertThat(prunedList).contains(svnMaterial2);
        assertThat(prunedList).hasSize(1);

        params.put(SvnPostCommitHookImplementer.REPO_URL_PARAM_KEY, "http://example.com/svn42");
        prunedList = svnPostCommitHookImplementer.prune(allMaterials, params);
        assertThat(prunedList).isEmpty();
    }

    @Test
    void shouldReturnEmptyListWhenUUIDIsNotPresent() {
        final SvnMaterial svnMaterial1 = mock(SvnMaterial.class);
        final HashSet<Material> allMaterials = new HashSet<>();
        allMaterials.add(svnMaterial1);
        final SvnPostCommitHookImplementer spy = spy(implementer);
        final Set<Material> prunedList = spy.prune(allMaterials, new HashMap());
        assertThat(prunedList.size()).isEqualTo(0);
        verify(spy, never()).isQualified(anyString(), any(SvnMaterial.class), any(HashMap.class));
    }

    @Test
    void shouldQualifySvnMaterialIfMaterialMatchesUUID() {
        final SvnMaterial svnMaterial1 = mock(SvnMaterial.class);
        when(svnMaterial1.urlForCommandLine()).thenReturn("url1");
        final SvnMaterial svnMaterial2 = mock(SvnMaterial.class);
        when(svnMaterial2.urlForCommandLine()).thenReturn("url2");
        final SvnMaterial svnMaterial3 = mock(SvnMaterial.class);
        when(svnMaterial3.urlForCommandLine()).thenReturn("url not present in map");
        final HashMap<String, String> map = new HashMap<>();
        map.put("url1", "12345");
        map.put("url2", "54321");
        assertThat(implementer.isQualified("12345", svnMaterial1, map)).isTrue();
        assertThat(implementer.isQualified("12345", svnMaterial2, map)).isFalse();
        assertThat(implementer.isQualified("12345", svnMaterial3, map)).isFalse();
    }

    @Test
    void shouldCreateRemoteUrlToRemoteUUIDMap() {
        final SvnPostCommitHookImplementer spy = spy(implementer);
        final SvnCommand svnCommand = mock(SvnCommand.class);
        final Material svnMaterial1 = mock(SvnMaterial.class);
        final Material hgMaterial1 = mock(HgMaterial.class);
        final HashSet<Material> allMaterials = new HashSet<>(Arrays.asList(svnMaterial1, hgMaterial1));
        doAnswer(invocation -> svnCommand).when(spy).getEmptySvnCommand();
        spy.createUrlToRemoteUUIDMap(allMaterials);
        verify(svnCommand).createUrlToRemoteUUIDMap(new HashSet<>(Arrays.asList((SvnMaterial) svnMaterial1)));
    }

    @Test
    void shouldReturnEmptySvnCommand() {
        final SvnCommand svnCommand = implementer.getEmptySvnCommand();
        assertThat(svnCommand instanceof SvnCommand).isTrue();
        assertThat(svnCommand.getUrl().toString()).isEqualTo(".");
    }
}
