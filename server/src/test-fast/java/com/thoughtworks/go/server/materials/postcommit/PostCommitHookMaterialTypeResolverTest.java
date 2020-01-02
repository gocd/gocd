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
package com.thoughtworks.go.server.materials.postcommit;

import com.thoughtworks.go.server.materials.postcommit.git.GitPostCommitHookImplementer;
import com.thoughtworks.go.server.materials.postcommit.mercurial.MercurialPostCommitHookImplementer;
import com.thoughtworks.go.server.materials.postcommit.pluggablescm.PluggableSCMPostCommitHookImplementer;
import com.thoughtworks.go.server.materials.postcommit.svn.SvnPostCommitHookImplementer;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class PostCommitHookMaterialTypeResolverTest {

    private PostCommitHookMaterialTypeResolver resolver;

    @Before
    public void setUp() {
        resolver = new PostCommitHookMaterialTypeResolver();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void shouldReturnUnknownPostCommitHookMaterialType() {
        final PostCommitHookMaterialType materialType = resolver.toType("some_invalid_type");
        assertThat(materialType instanceof PostCommitHookMaterialTypeResolver.UnknownPostCommitHookMaterialType, is(true));
        assertThat(materialType.isKnown(), is(false));
        assertThat(materialType.isValid("some_invalid_type"), is(false));
        materialType.getImplementer();
    }

    @Test
    public void shouldReturnSvnPostCommitHookMaterialType() {
        final PostCommitHookMaterialType materialType = resolver.toType("SVN");
        assertThat(materialType instanceof PostCommitHookMaterialTypeResolver.SvnPostCommitHookMaterialType, is(true));
        assertThat(materialType.isKnown(), is(true));
        assertThat(materialType.isValid("SVN"), is(true));
        assertThat(materialType.getImplementer() instanceof SvnPostCommitHookImplementer, is(true));
    }

    @Test
    public void shouldReturnGitPostCommitHookMaterialType() {
        final PostCommitHookMaterialType materialType = resolver.toType("GIT");
        assertThat(materialType instanceof PostCommitHookMaterialTypeResolver.GitPostCommitHookMaterialType, is(true));
        assertThat(materialType.isKnown(), is(true));
        assertThat(materialType.isValid("GIT"), is(true));
        assertThat(materialType.getImplementer() instanceof GitPostCommitHookImplementer, is(true));
    }

    @Test
    public void shouldReturnMercurialPostCommitHookMaterialType() {
        final PostCommitHookMaterialType materialType = resolver.toType("HG");
        assertThat(materialType instanceof PostCommitHookMaterialTypeResolver.MercurialPostCommitHookMaterialType, is(true));
        assertThat(materialType.isKnown(), is(true));
        assertThat(materialType.isValid("hg"), is(true));
        assertThat(materialType.getImplementer() instanceof MercurialPostCommitHookImplementer, is(true));
    }

    @Test
    public void shouldReturnPluggableSCMPostCommitHookMaterialTypeWithCaseInsensitivity() {
        final PostCommitHookMaterialType materialType = resolver.toType("SCM");
        assertThat(materialType instanceof PostCommitHookMaterialTypeResolver.PluggableSCMPostCommitHookMaterialType, is(true));
        assertThat(materialType.isKnown(), is(true));
        assertThat(materialType.isValid("scm"), is(true));
        assertThat(materialType.getImplementer() instanceof PluggableSCMPostCommitHookImplementer, is(true));
    }
}
