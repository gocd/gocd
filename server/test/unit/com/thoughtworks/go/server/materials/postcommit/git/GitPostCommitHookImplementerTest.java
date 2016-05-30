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

package com.thoughtworks.go.server.materials.postcommit.git;

import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.util.command.UrlArgument;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class GitPostCommitHookImplementerTest {

    private GitPostCommitHookImplementer implementer;

    @Before
    public void setUp() throws Exception {
        implementer = new GitPostCommitHookImplementer();
    }

    @Test
    public void shouldReturnListOfMaterialMatchingThePayloadURL() throws Exception {
        GitMaterial material1 = mock(GitMaterial.class);
        when(material1.getUrlArgument()).thenReturn(new UrlArgument("https://other_repo.local.git"));
        GitMaterial material2 = mock(GitMaterial.class);
        when(material2.getUrlArgument()).thenReturn(new UrlArgument("https://other_repo.local.git"));
        GitMaterial material3 = mock(GitMaterial.class);
        when(material3.getUrlArgument()).thenReturn(new UrlArgument("https://machine.local.git"));
        GitMaterial material4 = mock(GitMaterial.class);
        when(material4.getUrlArgument()).thenReturn(new UrlArgument("https://machine.local.git"));
        Set<Material> materials = new HashSet<Material>(Arrays.asList(material1, material2, material3, material4));
        HashMap params = new HashMap();
        params.put(GitPostCommitHookImplementer.REPO_URL_PARAM_KEY, "https://machine.local.git");

        Set<Material> actual = implementer.prune(materials, params);

        assertThat(actual.size(), is(2));
        assertThat(actual, hasItem(material3));
        assertThat(actual, hasItem(material4));

        verify(material1).getUrlArgument();
        verify(material2).getUrlArgument();
        verify(material3).getUrlArgument();
        verify(material4).getUrlArgument();
    }

    @Test
    public void shouldQueryOnlyGitMaterialsWhilePruning() throws Exception {
        SvnMaterial material1 = mock(SvnMaterial.class);
        Set<Material> materials = new HashSet<Material>(Arrays.asList(material1));
        HashMap params = new HashMap();
        params.put(GitPostCommitHookImplementer.REPO_URL_PARAM_KEY, "https://machine.local.git");

        Set<Material> actual = implementer.prune(materials, params);

        assertThat(actual.size(), is(0));

        verifyNoMoreInteractions(material1);
    }

    @Test
    public void shouldReturnEmptyListIfParamHasNoValueForRepoURL() throws Exception {
        GitMaterial material1 = mock(GitMaterial.class);
        when(material1.getUrlArgument()).thenReturn(new UrlArgument("https://machine.local.git"));
        Set<Material> materials = new HashSet<Material>(Arrays.asList(material1));
        HashMap params = new HashMap();
        params.put(GitPostCommitHookImplementer.REPO_URL_PARAM_KEY, "");

        Set<Material> actual = implementer.prune(materials, params);

        assertThat(actual.size(), is(0));

        verifyNoMoreInteractions(material1);
    }

    @Test
    public void shouldReturnEmptyListIfParamIsMissingForRepoURL() throws Exception {
        GitMaterial material1 = mock(GitMaterial.class);
        when(material1.getUrlArgument()).thenReturn(new UrlArgument("https://machine.local.git"));
        Set<Material> materials = new HashSet<Material>(Arrays.asList(material1));

        Set<Material> actual = implementer.prune(materials, new HashMap());

        assertThat(actual.size(), is(0));

        verifyNoMoreInteractions(material1);
    }

    @Test
    public void shouldReturnTrueWhenURLIsAnExactMatch() throws Exception {
        boolean isEqual = implementer.isUrlEqual("http://repo-url.git", new GitMaterial("http://repo-url.git"));
        assertThat(isEqual, is(true));
    }

    @Test
    public void shouldReturnTrueWhenBasicAuthIsProvidedInURL() throws Exception {
        boolean isEqual = implementer.isUrlEqual("http://repo-url.git", new GitMaterial("http://user:passW)rD@repo-url.git"));
        assertThat(isEqual, is(true));
    }

    @Test
    public void shouldReturnTrueWhenBasicAuthWithoutPasswordIsProvidedInURL() throws Exception {
        boolean isEqual = implementer.isUrlEqual("http://repo-url.git", new GitMaterial("http://user:@repo-url.git"));
        assertThat(isEqual, is(true));
    }

    @Test
    public void shouldReturnTrueWhenBasicAuthWithOnlyUsernameIsProvidedInURL() throws Exception {
        boolean isEqual = implementer.isUrlEqual("http://repo-url.git", new GitMaterial("http://user@repo-url.git"));
        assertThat(isEqual, is(true));
    }

    @Test
    public void shouldReturnFalseWhenProtocolIsDifferent() throws Exception {
        boolean isEqual = implementer.isUrlEqual("http://repo-url.git", new GitMaterial("https://repo-url.git"));
        assertThat(isEqual, is(false));
    }

    @Test
    public void shouldReturnFalseWhenNoValidatorCouldParseUrl() throws Exception {
        boolean isEqual = implementer.isUrlEqual("http://repo-url.git", new GitMaterial("something.completely.random"));
        assertThat(isEqual, is(false));
    }

    @Test
    public void shouldReturnFalseUpWheNoProtocolIsGiven() throws Exception {
        boolean isEqual = implementer.isUrlEqual("http://repo-url.git", new GitMaterial("repo-url.git"));
        assertThat(isEqual, is(false));
    }

    @Test
    public void shouldReturnFalseForEmptyURLField() throws Exception {
        boolean isEqual = implementer.isUrlEqual("http://repo-url.git", new GitMaterial("http://"));
        assertThat(isEqual, is(false));
    }

    @Test
    public void shouldReturnFalseForEmptyURLFieldWithAuth() throws Exception {
        boolean isEqual = implementer.isUrlEqual("http://repo-url.git", new GitMaterial("http://user:password@"));
        assertThat(isEqual, is(false));
    }

    @Test
    public void shouldMatchFileBasedAccessWithoutAuth() throws Exception {
        boolean isEqual = implementer.isUrlEqual("/tmp/foo/repo-git", new GitMaterial("/tmp/foo/repo-git"));
        assertThat(isEqual, is(true));
    }
}
