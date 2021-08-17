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
package com.thoughtworks.go.domain.valuestreammap;


import com.thoughtworks.go.config.materials.ScmMaterial;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.helper.MaterialsMother;
import com.thoughtworks.go.helper.ModificationsMother;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class SCMDependencyNodeTest {
    @Test
    public void shouldBeAbleToAddMaterialRevisions() {
        ScmMaterial gitMaterial = MaterialsMother.gitMaterial("/url/for/repo");
        List<Modification> modifications = ModificationsMother.multipleModificationList();

        SCMDependencyNode node = new SCMDependencyNode("nodeID", "nodeName", "GIT");

        node.addMaterialRevision(new MaterialRevision(null, modifications));
        node.addMaterialRevision(new MaterialRevision(gitMaterial, modifications));

        assertThat(node.getMaterialRevisions().size(), is(2));
    }

    @Test
    public void addMaterialRevisionShouldNotAllowDuplicates() {
        ScmMaterial gitMaterial = MaterialsMother.gitMaterial("/url/for/repo");
        List<Modification> modifications = ModificationsMother.multipleModificationList();

        SCMDependencyNode node = new SCMDependencyNode("nodeID", "nodeName", "GIT");

        node.addMaterialRevision(new MaterialRevision(gitMaterial, modifications));
        node.addMaterialRevision(new MaterialRevision(gitMaterial, modifications));

        assertThat(node.getMaterialRevisions().size(), is(1));
    }

    @Test
    public void addMaterialRevisionShouldNotAllowNull() {
        assertThatThrownBy(() -> new SCMDependencyNode("nodeID", "nodeName", "GIT").addMaterialRevision(null))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    public void addRevisionShouldBeDisallowed() {
        assertThatThrownBy(() -> new SCMDependencyNode("nodeID", "nodeName", "GIT").
                addRevision(new SCMRevision(ModificationsMother.oneModifiedFile("some_revision"))))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    public void addRevisionsShouldBeDisallowed() {
        ArrayList<Revision> revisions = new ArrayList<>();
        revisions.add(new SCMRevision(ModificationsMother.oneModifiedFile("some_revision")));

        assertThatThrownBy(() -> new SCMDependencyNode("nodeID", "nodeName", "GIT").addRevisions(revisions))
        .isInstanceOf(RuntimeException.class);
    }

    @Test
    public void revisionsShouldFetchRevisionsFromMaterialRevisionSorted() {
        ScmMaterial gitMaterial = MaterialsMother.gitMaterial("/url/for/repo");
        Modification twoDaysAgo = ModificationsMother.oneModifiedFile("rev1", new DateTime().minusDays(2).toDate());
        Modification yesterday = ModificationsMother.oneModifiedFile("rev2", new DateTime().minusDays(1).toDate());
        Modification today = ModificationsMother.oneModifiedFile("rev3", new Date());

        SCMDependencyNode node = new SCMDependencyNode("nodeID", "nodeName", "GIT");
        node.addMaterialRevision(new MaterialRevision(gitMaterial, false, twoDaysAgo));
        node.addMaterialRevision(new MaterialRevision(gitMaterial, false, yesterday));
        node.addMaterialRevision(new MaterialRevision(gitMaterial, false, today));

        assertThat(node.revisions().size(), is(3));
        assertThat(node.revisions().get(0).getRevisionString(), is(today.getRevision()));
        assertThat(node.revisions().get(1).getRevisionString(), is(yesterday.getRevision()));
        assertThat(node.revisions().get(2).getRevisionString(), is(twoDaysAgo.getRevision()));
    }
}
