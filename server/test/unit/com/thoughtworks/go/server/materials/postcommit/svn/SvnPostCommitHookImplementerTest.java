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

package com.thoughtworks.go.server.materials.postcommit.svn;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.config.materials.mercurial.HgMaterial;
import com.thoughtworks.go.config.materials.perforce.P4Material;
import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.config.materials.tfs.TfsMaterial;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.svn.SvnCommand;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SvnPostCommitHookImplementerTest {

    private SvnPostCommitHookImplementer implementer;

    @Before
    public void setUp() {
        implementer = new SvnPostCommitHookImplementer();
    }

    @Test
    public void shouldPruneListToGiveOutOnlySvnMaterials() {
        final Material svnMaterial1 = mock(SvnMaterial.class);
        final Material svnMaterial2 = mock(SvnMaterial.class);
        final Material svnMaterial3 = mock(SvnMaterial.class);
        final Material hgMaterial = mock(HgMaterial.class);
        final Material gitMaterial = mock(GitMaterial.class);
        final Material p4Material = mock(P4Material.class);
        final Material tfsMaterial = mock(TfsMaterial.class);
        final Material dependencyMaterial = mock(DependencyMaterial.class);
        final HashSet<Material> allMaterials = new HashSet<Material>(Arrays.asList(svnMaterial1, svnMaterial2, svnMaterial3, gitMaterial, hgMaterial, p4Material, tfsMaterial, dependencyMaterial));
        final SvnPostCommitHookImplementer spy = spy(implementer);
        doAnswer(new Answer() {
            @Override public Object answer(InvocationOnMock invocation) throws Throwable {
                return Boolean.TRUE;
            }
        }).when(spy).isQualified(anyString(), any(SvnMaterial.class), any(HashMap.class));
        doAnswer(new Answer() {
            @Override public Object answer(InvocationOnMock invocation) throws Throwable {
                return new HashMap();
            }
        }).when(spy).createUrlToRemoteUUIDMap(allMaterials);
        final HashMap params = new HashMap();
        params.put(SvnPostCommitHookImplementer.UUID, "some uuid");
        final Set<Material> prunedList = spy.prune(allMaterials, params);

        assertThat(prunedList.size(), is(3));
        assertThat(prunedList, hasItems(svnMaterial1, svnMaterial2, svnMaterial3));
    }

    @Test
    public void shouldPruneListToGiveOutOnlySvnMaterialsWhichMatchTheRepositoryUUID() {
        final SvnMaterial svnMaterial1 = mock(SvnMaterial.class);
        final SvnMaterial svnMaterial2 = mock(SvnMaterial.class);
        final SvnMaterial svnMaterial3 = mock(SvnMaterial.class);
        final HashSet<Material> allMaterials = new HashSet<Material>(Arrays.asList(svnMaterial1, svnMaterial2, svnMaterial3));
        final HashMap<Object, Object> params = new HashMap<Object, Object>();
        final String uuid = "12345";
        params.put(SvnPostCommitHookImplementer.UUID, uuid);

        final SvnPostCommitHookImplementer spy = spy(implementer);
        final HashMap<String, String> map = new HashMap<String, String>();
        doAnswer(new Answer() {
            @Override public Object answer(InvocationOnMock invocation) throws Throwable {
                map.put("url1", "12345");
                map.put("url2", "54321");
                return map;
            }
        }).when(spy).createUrlToRemoteUUIDMap(allMaterials);
        doAnswer(new Answer() {
            @Override public Object answer(InvocationOnMock invocation) throws Throwable {
                return Boolean.FALSE;
            }
        }).when(spy).isQualified(uuid, svnMaterial1, map);
        doAnswer(new Answer() {
            @Override public Object answer(InvocationOnMock invocation) throws Throwable {
                return Boolean.TRUE;
            }
        }).when(spy).isQualified(uuid, svnMaterial2, map);
        doAnswer(new Answer() {
            @Override public Object answer(InvocationOnMock invocation) throws Throwable {
                return Boolean.FALSE;
            }
        }).when(spy).isQualified(uuid, svnMaterial3, map);

        final Set<Material> prunedList = spy.prune(allMaterials, params);

        assertThat(prunedList.size(), is(1));
        verify(spy, times(1)).createUrlToRemoteUUIDMap(allMaterials);
    }

    @Test
    public void shouldReturnEmptyListWhenUUIDIsNotPresent() {
        final SvnMaterial svnMaterial1 = mock(SvnMaterial.class);
        final HashSet<Material> allMaterials = new HashSet<Material>();
        allMaterials.add(svnMaterial1);
        final SvnPostCommitHookImplementer spy = spy(implementer);
        final Set<Material> prunedList = spy.prune(allMaterials, new HashMap());
        assertThat(prunedList.size(), is(0));
        verify(spy, never()).isQualified(anyString(), any(SvnMaterial.class), any(HashMap.class));
    }

    @Test
    public void shouldQualifySvnMaterialIfMaterialMatchesUUID() {
        final SvnMaterial svnMaterial1 = mock(SvnMaterial.class);
        when(svnMaterial1.getUrl()).thenReturn("url1");
        final SvnMaterial svnMaterial2 = mock(SvnMaterial.class);
        when(svnMaterial2.getUrl()).thenReturn("url2");
        final SvnMaterial svnMaterial3 = mock(SvnMaterial.class);
        when(svnMaterial3.getUrl()).thenReturn("url not present in map");
        final HashMap<String, String> map = new HashMap<String, String>();
        map.put("url1", "12345");
        map.put("url2", "54321");
        assertThat(implementer.isQualified("12345", svnMaterial1, map), is(true));
        assertThat(implementer.isQualified("12345", svnMaterial2, map), is(false));
        assertThat(implementer.isQualified("12345", svnMaterial3, map), is(false));
    }

    @Test
    public void shouldCreateRemoteUrlToRemoteUUIDMap() {
        final SvnPostCommitHookImplementer spy = spy(implementer);
        final SvnCommand svnCommand = mock(SvnCommand.class);
        final Material svnMaterial1 = mock(SvnMaterial.class);
        final Material hgMaterial1 = mock(HgMaterial.class);
        final HashSet<Material> allMaterials = new HashSet<Material>(Arrays.asList(svnMaterial1, hgMaterial1));
        doAnswer(new Answer() {
            @Override public Object answer(InvocationOnMock invocation) throws Throwable {
                return svnCommand;
            }
        }).when(spy).getEmptySvnCommand();
        spy.createUrlToRemoteUUIDMap(allMaterials);
        verify(svnCommand).createUrlToRemoteUUIDMap(new HashSet<SvnMaterial>(Arrays.asList((SvnMaterial) svnMaterial1)));
    }

    @Test
    public void shouldReturnEmptySvnCommand() {
        final SvnCommand svnCommand = implementer.getEmptySvnCommand();
        assertThat(svnCommand instanceof SvnCommand, is(true));
        assertThat(svnCommand.getUrl().toString(), is("."));
    }
}
