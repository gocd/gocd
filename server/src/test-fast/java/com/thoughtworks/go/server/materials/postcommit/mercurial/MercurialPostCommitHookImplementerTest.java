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
package com.thoughtworks.go.server.materials.postcommit.mercurial;

import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.config.materials.mercurial.HgMaterial;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.util.command.UrlArgument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

public class MercurialPostCommitHookImplementerTest {

    private MercurialPostCommitHookImplementer implementer;

    @BeforeEach
    public void setUp() throws Exception {
        implementer = new MercurialPostCommitHookImplementer();
    }

    @Test
    public void shouldPruneMercurialMaterialsWhichMatchIncomingURL() throws Exception {
        HgMaterial material1 = mock(HgMaterial.class);
        when(material1.getUrlArgument()).thenReturn(new UrlArgument("http://repo1.something.local"));
        HgMaterial material2 = mock(HgMaterial.class);
        when(material2.getUrlArgument()).thenReturn(new UrlArgument("http://repo1.something.local"));
        HgMaterial material3 = mock(HgMaterial.class);
        when(material3.getUrlArgument()).thenReturn(new UrlArgument("ssh://repo1.something.local"));
        GitMaterial material4 = mock(GitMaterial.class);
        when(material4.getUrlArgument()).thenReturn(new UrlArgument("http://repo1.something.local"));
        Set<Material> materials = new HashSet<>(Arrays.asList(material1, material2, material3, material4));
        Map params = new HashMap();
        params.put(MercurialPostCommitHookImplementer.REPO_URL_PARAM_KEY, "http://repo1.something.local");

        Set<Material> actual = implementer.prune(materials, params);

        assertThat(actual.size(), is(2));
        assertThat(actual, hasItem(material1));
        assertThat(actual, hasItem(material2));
        verify(material1).getUrlArgument();
        verify(material2).getUrlArgument();
        verify(material3).getUrlArgument();
        verify(material4, times(0)).getUrlArgument();
    }

    @Test
    public void shouldReturnEmptyListWhenURLIsSpecified() throws Exception {
        HgMaterial material = mock(HgMaterial.class);
        HashSet<Material> materials = new HashSet<>(Arrays.asList(material));

        Set<Material> actual = implementer.prune(materials, new HashMap());

        assertThat(actual.size(), is(0));
        verify(material, times(0)).getUrlArgument();
    }

    @Test
    public void shouldReturnTrueWhenURLIsAnExactMatch() throws Exception {
        boolean isEqual = implementer.isUrlEqual("http://repo-url", new HgMaterial("http://repo-url", "dest"));
        assertThat(isEqual, is(true));
    }

    @Test
    public void shouldReturnTrueWhenBasicAuthIsProvidedInURL() throws Exception {
        boolean isEqual = implementer.isUrlEqual("http://repo-url", new HgMaterial("http://user:passW)rD@repo-url", "dest"));
        assertThat(isEqual, is(true));
    }

    @Test
    public void shouldReturnTrueWhenBasicAuthWithoutPasswordIsProvidedInURL() throws Exception {
        boolean isEqual = implementer.isUrlEqual("http://repo-url", new HgMaterial("http://user:@repo-url", "dest"));
        assertThat(isEqual, is(true));
    }

    @Test
    public void shouldReturnTrueWhenBasicAuthWithOnlyUsernameIsProvidedInURL() throws Exception {
        boolean isEqual = implementer.isUrlEqual("http://repo-url", new HgMaterial("http://user@repo-url", "dest"));
        assertThat(isEqual, is(true));
    }

    @Test
    public void shouldReturnFalseWhenProtocolIsDifferent() throws Exception {
        boolean isEqual = implementer.isUrlEqual("http://repo-url", new HgMaterial("https://repo-url", "dest"));
        assertThat(isEqual, is(false));
    }

    @Test
    public void shouldReturnFalseWhenNoValidatorCouldParseUrl() throws Exception {
        boolean isEqual = implementer.isUrlEqual("http://repo-url", new HgMaterial("something.completely.random", "dest"));
        assertThat(isEqual, is(false));
    }

    @Test
    public void shouldReturnFalseWhenNoProtocolIsGiven() throws Exception {
        boolean isEqual = implementer.isUrlEqual("http://repo-url#foo", new HgMaterial("repo-url#foo", "dest"));
        assertThat(isEqual, is(false));
    }

    @Test
    public void shouldReturnFalseForEmptyURLField() throws Exception {
        boolean isEqual = implementer.isUrlEqual("http://repo-url", new HgMaterial("http://", "dest"));
        assertThat(isEqual, is(false));
    }

    @Test
    public void shouldReturnFalseForEmptyURLFieldWithAuth() throws Exception {
        boolean isEqual = implementer.isUrlEqual("http://repo-url", new HgMaterial("http://user:password@", "dest"));
        assertThat(isEqual, is(false));
    }

    @Test
    public void shouldMatchFileBasedAccessWithoutAuth() throws Exception {
        boolean isEqual = implementer.isUrlEqual("/tmp/foo/repo-git", new HgMaterial("/tmp/foo/repo-git", "dest"));
        assertThat(isEqual, is(true));
    }

    @Test
    public void shouldReturnTrueWhenIncomingUrlDoesNotHaveAuthDetails() throws Exception {
        boolean isEqual = implementer.isUrlEqual("http://foo.bar/#foo", new HgMaterial("http://user:password@foo.bar/#foo", "dest"));
        assertThat(isEqual, is(true));
    }
}
