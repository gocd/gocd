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

package com.thoughtworks.go.server.materials;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.Revision;
import com.thoughtworks.go.server.service.MaterialService;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LegacyMaterialCheckerTest {

    private LegacyMaterialChecker checker;
    private MaterialService materialService;
    private File file;

    @Before
    public void setUp() {
        materialService = mock(MaterialService.class);
        checker = new LegacyMaterialChecker(materialService, null);
        file = new File(".");
    }

    @Test
    public void shouldThrowAnExceptionWhenAMaterialReturnsAnEmptyListForLatestModifications() {
        Material material = mock(Material.class);
        when(materialService.latestModification(material, file, null)).thenReturn(new ArrayList<Modification>());
        when(material.toString()).thenReturn("material");
        try {
            checker.findLatestModification(file, material, null);
            fail("Should have failed since the latest modification check failed");
        } catch (RuntimeException e) {
            assertThat(e.getMessage(),
                    is("Latest modifications check for the material 'material' returned an empty modification list. This might be because the material might be wrongly configured."));
        }
    }

    @Test
    public void shouldGetModificationsSinceRevision() {
        Material material = mock(Material.class);
        MaterialRevision materialRevision = mock(MaterialRevision.class);
        Revision revision = mock(Revision.class);
        List<Modification> modifications = new ArrayList<Modification>();
        when(materialRevision.getRevision()).thenReturn(revision);
        when(materialService.modificationsSince(material, file, revision, null)).thenReturn(modifications);
        List<Modification> actual = checker.findModificationsSince(file, material, materialRevision);
        assertThat(actual, is(modifications));
    }
}
