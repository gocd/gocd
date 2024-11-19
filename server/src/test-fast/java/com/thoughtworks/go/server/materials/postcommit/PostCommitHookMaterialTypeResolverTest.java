/*
 * Copyright 2024 Thoughtworks, Inc.
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PostCommitHookMaterialTypeResolverTest {

    private PostCommitHookMaterialTypeResolver resolver;

    @BeforeEach
    public void setUp() {
        resolver = new PostCommitHookMaterialTypeResolver();
    }

    @Test
    public void shouldReturnUnknownPostCommitHookMaterialType() {
        final PostCommitHookMaterialType materialType = resolver.toType("some_invalid_type");
        assertThat(materialType instanceof PostCommitHookMaterialTypeResolver.UnknownPostCommitHookMaterialType).isTrue();
        assertThat(materialType.isKnown()).isFalse();
        assertThat(materialType.isValid("some_invalid_type")).isFalse();
        assertThatThrownBy(materialType::getImplementer)
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    public void shouldReturnSvnPostCommitHookMaterialType() {
        final PostCommitHookMaterialType materialType = resolver.toType("SVN");
        assertThat(materialType instanceof PostCommitHookMaterialTypeResolver.SvnPostCommitHookMaterialType).isTrue();
        assertThat(materialType.isKnown()).isTrue();
        assertThat(materialType.isValid("SVN")).isTrue();
        assertThat(materialType.getImplementer() instanceof SvnPostCommitHookImplementer).isTrue();
    }

    @Test
    public void shouldReturnGitPostCommitHookMaterialType() {
        final PostCommitHookMaterialType materialType = resolver.toType("GIT");
        assertThat(materialType instanceof PostCommitHookMaterialTypeResolver.GitPostCommitHookMaterialType).isTrue();
        assertThat(materialType.isKnown()).isTrue();
        assertThat(materialType.isValid("GIT")).isTrue();
        assertThat(materialType.getImplementer() instanceof GitPostCommitHookImplementer).isTrue();
    }

    @Test
    public void shouldReturnMercurialPostCommitHookMaterialType() {
        final PostCommitHookMaterialType materialType = resolver.toType("HG");
        assertThat(materialType instanceof PostCommitHookMaterialTypeResolver.MercurialPostCommitHookMaterialType).isTrue();
        assertThat(materialType.isKnown()).isTrue();
        assertThat(materialType.isValid("hg")).isTrue();
        assertThat(materialType.getImplementer() instanceof MercurialPostCommitHookImplementer).isTrue();
    }

    @Test
    public void shouldReturnPluggableSCMPostCommitHookMaterialTypeWithCaseInsensitivity() {
        final PostCommitHookMaterialType materialType = resolver.toType("SCM");
        assertThat(materialType instanceof PostCommitHookMaterialTypeResolver.PluggableSCMPostCommitHookMaterialType).isTrue();
        assertThat(materialType.isKnown()).isTrue();
        assertThat(materialType.isValid("scm")).isTrue();
        assertThat(materialType.getImplementer() instanceof PluggableSCMPostCommitHookImplementer).isTrue();
    }
}
