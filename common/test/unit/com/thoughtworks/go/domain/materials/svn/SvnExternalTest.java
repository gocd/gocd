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

package com.thoughtworks.go.domain.materials.svn;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.thoughtworks.go.config.materials.Materials;
import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.TestSubprocessExecutionContext;
import com.thoughtworks.go.helper.SvnTestRepoWithExternal;
import com.thoughtworks.go.helper.TestRepo;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.thoughtworks.go.helper.MaterialsMother.svnMaterial;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class SvnExternalTest {
    private static SvnTestRepoWithExternal svnRepo;
    public static File workingDir;

    @BeforeClass
    public static void copyRepository() throws IOException {
        svnRepo = new SvnTestRepoWithExternal();
        workingDir = svnRepo.projectRepositoryUrlAsFile();
    }

    @AfterClass
    public static void deleteRepository() throws IOException {
        TestRepo.internalTearDown();
    }

    @Test
    public void shouldGetAllExternalURLSByPropgetOnMainURL() throws Exception {
        String url = svnRepo.projectRepositoryUrl();
        SvnCommand svn = new SvnCommand(null, url, "user", "pass", false);
        List<SvnExternal> urls = svn.getAllExternalURLs();
        assertThat(urls.size(), is(1));
    }




    @Test
    public void shouldGetLatestRevisionFromExpandedSvnExternalRepository() {
        MaterialRevisions materialRevisions = new MaterialRevisions();
        Material svnExt = svnMaterial(svnRepo.externalRepositoryUrl(), "end2end");
        List<Modification> modifications = ((SvnMaterial) svnExt).latestModification(svnRepo.workingFolder(), new TestSubprocessExecutionContext());
        materialRevisions.addRevision(svnExt, modifications);

        assertThat(materialRevisions.numberOfRevisions(), is(1));
        MaterialRevision materialRevision = materialRevisions.getRevisions().get(0);
        assertThat(materialRevision.getMaterial(), is(svnExt));
        assertThat(materialRevision.getRevision().getRevision(), is("4"));
    }

    @Test
    public void shouldGetLatestRevision() {
        SvnMaterial svn = svnMaterial(svnRepo.projectRepositoryUrl(), null);
        SvnMaterial svnExt = svnMaterial(svnRepo.externalRepositoryUrl(), "end2end");
        final Materials materials = new Materials(svn, svnExt);

        final MaterialRevisions materialRevisions = materials.latestModification(svnRepo.workingFolder(), new TestSubprocessExecutionContext());
        assertThat(materialRevisions.numberOfRevisions(), is(2));

        MaterialRevision main = materialRevisions.getRevisions().get(0);
        assertThat((SvnMaterial) main.getMaterial(), is(svn));
        assertThat(main.getModifications().size(), is(1));
        assertThat(main.getRevision().getRevision(), is("5"));

        MaterialRevision external = materialRevisions.getRevisions().get(1);
        assertThat((SvnMaterial) external.getMaterial(), is(svnExt));
        assertThat(external.getRevision().getRevision(), is("4"));
        assertThat(external.getModifications().size(), is(1));
    }
}
