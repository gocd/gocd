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
package com.thoughtworks.go.server.materials.postcommit.pluggablescm;

import com.thoughtworks.go.config.materials.PluggableSCMMaterial;
import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.helper.MaterialConfigsMother;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class PluggableSCMPostCommitHookImplementerTest {
    private PluggableSCMPostCommitHookImplementer implementer;

    @BeforeEach
    public void setUp() throws Exception {
        implementer = new PluggableSCMPostCommitHookImplementer();
    }

    @Test
    public void shouldReturnListOfMaterialMatchingTheSCMNameWithCaseInsensitivity() throws Exception {
        PluggableSCMMaterial material1 = new PluggableSCMMaterial(MaterialConfigsMother.pluggableSCMMaterialConfig("material-1", null, null));
        PluggableSCMMaterial material2 = new PluggableSCMMaterial(MaterialConfigsMother.pluggableSCMMaterialConfig("material-2", null, null));
        PluggableSCMMaterial material3 = new PluggableSCMMaterial(MaterialConfigsMother.pluggableSCMMaterialConfig("material-3", null, null));
        PluggableSCMMaterial material4 = new PluggableSCMMaterial(MaterialConfigsMother.pluggableSCMMaterialConfig("material-4", null, null));
        Set<Material> materials = new HashSet<>(Arrays.asList(material1, material2, material3, material4));
        Map params = new HashMap();
        params.put(PluggableSCMPostCommitHookImplementer.SCM_NAME, "SCM-MATERIAL-1");

        Set<Material> actual = implementer.prune(materials, params);

        assertThat(actual.size(), is(1));
        assertThat(actual, hasItem(material1));
    }

    @Test
    public void shouldReturnEmptyListIfNoMatchingMaterialFound() throws Exception {
        PluggableSCMMaterial material1 = new PluggableSCMMaterial(MaterialConfigsMother.pluggableSCMMaterialConfig("material-1", null, null));
        PluggableSCMMaterial material2 = new PluggableSCMMaterial(MaterialConfigsMother.pluggableSCMMaterialConfig("material-2", null, null));
        Set<Material> materials = new HashSet<>(Arrays.asList(material1, material2));
        Map params = new HashMap();
        params.put(PluggableSCMPostCommitHookImplementer.SCM_NAME, "unknown-scm-name");

        Set<Material> actual = implementer.prune(materials, params);

        assertThat(actual.size(), is(0));
    }

    @Test
    public void shouldQueryOnlyPluggableSCMMaterialsWhilePruning() throws Exception {
        SvnMaterial material1 = mock(SvnMaterial.class);
        Set<Material> materials = new HashSet<>(Arrays.asList(material1));
        Map params = new HashMap();
        params.put(PluggableSCMPostCommitHookImplementer.SCM_NAME, "scm-material-1");

        Set<Material> actual = implementer.prune(materials, params);

        assertThat(actual.size(), is(0));
        verifyNoMoreInteractions(material1);
    }

    @Test
    public void shouldReturnEmptyListIfParamHasNoValueForSCMName() throws Exception {
        PluggableSCMMaterial material1 = mock(PluggableSCMMaterial.class);
        Set<Material> materials = new HashSet<>(Arrays.asList(material1));
        Map params = new HashMap();
        params.put(PluggableSCMPostCommitHookImplementer.SCM_NAME, "");

        Set<Material> actual = implementer.prune(materials, params);

        assertThat(actual.size(), is(0));
        verifyNoMoreInteractions(material1);
    }

    @Test
    public void shouldReturnEmptyListIfParamIsMissingForSCMName() throws Exception {
        PluggableSCMMaterial material1 = mock(PluggableSCMMaterial.class);
        Set<Material> materials = new HashSet<>(Arrays.asList(material1));

        Set<Material> actual = implementer.prune(materials, new HashMap());

        assertThat(actual.size(), is(0));
        verifyNoMoreInteractions(material1);
    }
}
