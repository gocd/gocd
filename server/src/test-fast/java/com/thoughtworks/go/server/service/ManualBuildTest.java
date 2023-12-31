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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.server.domain.Username;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class ManualBuildTest {
    private MaterialRevisions materialRevisions;
    private ManualBuild manualBuild;

    @BeforeEach
    public void setUp() {
        manualBuild = new ManualBuild(new Username(new CaseInsensitiveString("cruise-user")));
        SvnMaterial material = new SvnMaterial("http://foo.bar/baz", "user", "pass", false);
        materialRevisions = new MaterialRevisions(new MaterialRevision(material, new Modification(new Date(), "1234", "MOCK_LABEL-12", null)));
    }

    @Test
    public void shouldPopulateProducedBuildCauseApproverForOnModificationBuildCause() throws Exception {
        BuildCause buildCause = manualBuild.onModifications(materialRevisions, false, null);
        assertThat(buildCause.getApprover(), is("cruise-user"));
    }

    @Test
    public void shouldPopulateProducedBuildCauseApproverForEmptyModificationBuildCause() throws Exception {
        BuildCause buildCause = manualBuild.onEmptyModifications(null, materialRevisions);
        assertThat(buildCause.getApprover(), is("cruise-user"));
    }
}
