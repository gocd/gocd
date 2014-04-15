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

package com.thoughtworks.go.server.controller.beans;

import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig;
import com.thoughtworks.go.config.materials.perforce.P4MaterialConfig;
import com.thoughtworks.go.config.materials.svn.SvnMaterialConfig;
import com.thoughtworks.go.config.materials.tfs.TfsMaterialConfig;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class MaterialFactoryTest {

    private MaterialFactory materialFactory;

    @Before public void setUp() throws Exception {
        materialFactory = new MaterialFactory();
    }

    @Test
    public void shouldCreateP4Material() throws Exception {
        MaterialConfig bean = materialFactory.getMaterial("p4", "url", "username", "password", true, "//...//", null, null, null);
        assertThat(bean, instanceOf(P4MaterialConfig.class));
    }

    @Test(expected = Exception.class)
    public void shouldThrowExceptionWhenViewIsNull() throws Exception {
        materialFactory.getMaterial("p4", "url", "username", "password", true, null, null, null, null);
    }

    @Test(expected = Exception.class)
    public void shouldThrowExceptionWhenViewIsEmpty() throws Exception {
        materialFactory.getMaterial("p4", "url", "username", "password", true, "", null, null, null);
    }

    @Test
    public void shouldCreateGitMaterial() throws Exception {
        MaterialConfig bean = materialFactory.getMaterial("git", "url", "username", "password", true, null, null, null, null);
        assertThat(bean, instanceOf(GitMaterialConfig.class));
    }

    @Test
    public void shouldCreateHgMaterial() throws Exception {
        MaterialConfig bean = materialFactory.getMaterial("hg", "url", "username", "password", true, null, null, null, null);
        assertThat(bean, instanceOf(HgMaterialConfig.class));
    }

    @Test
    public void shouldCreateSvnMaterialIfScmIsNull() throws Exception {
        MaterialConfig bean = materialFactory.getMaterial(null, "url", "username", "password", true, null, null, null, null);
        assertThat(bean, instanceOf(SvnMaterialConfig.class));
    }

    @Test
    public void shouldCreateSvnMaterialIfScmIsEmpty() throws Exception {
        MaterialConfig bean = materialFactory.getMaterial(null, "url", "username", "password", true, null, null, null, null);
        assertThat(bean, instanceOf(SvnMaterialConfig.class));
    }

    @Test(expected = Exception.class)
    public void shouldThrowExceptionWhenSCMIsUnknown() throws Exception {
        materialFactory.getMaterial("starTeam", "url", "username", "password", true, "", null, null, null);
    }

    @Test
    public void shouldNotTrimUrl() throws Exception {
        String paramUrl = " url ";
        GitMaterialConfig git = (GitMaterialConfig) materialFactory.getMaterial("git", paramUrl, "username", "password", true, "", null, null, null);
        assertThat(git.getUrl(), is(paramUrl));
    }

    @Test
    public void shouldNotTrimUsernameAndPassword() throws Exception {
        String paramUsername = " username  ";
        String paramPassword = "  password ";
        SvnMaterialConfig svn = (SvnMaterialConfig) materialFactory.getMaterial("svn", "url", paramUsername, paramPassword, true, "", null, null, null);
        assertThat(svn.getUserName(), is(paramUsername));
        assertThat(svn.getPassword(), is(paramPassword));
    }

    @Test
    public void shouldNotTrimGitBranch() throws Exception {
        String branch = " branch ";
        GitMaterialConfig git = (GitMaterialConfig) materialFactory.getMaterial("git", "url", null, null, false, null, branch, null, null);
        assertThat(git.getBranch(), is(branch));
    }

    @Test
    public void shouldCreateTfsMaterial() {
        TfsMaterialConfig tfs = (TfsMaterialConfig) materialFactory.getMaterial("tfs", "url", "username", "password", false, null, null, "projectPath", "domain");
        assertThat(tfs, instanceOf(TfsMaterialConfig.class));
        assertThat(tfs.getUrl(), is("url"));
        assertThat(tfs.getUsername(), is("username"));
        assertThat(tfs.getPassword(), is("password"));
        assertThat(tfs.getProjectPath(), is("projectPath"));
        assertThat(tfs.getDomain(), is("domain"));
    }

    @Test
    public void shouldConvertDomainValueToEmptyStringIfNull() {
        TfsMaterialConfig tfs = (TfsMaterialConfig) materialFactory.getMaterial("tfs", "url", "username", "password", false, null, null, "projectpath", null);
        assertThat(tfs, instanceOf(TfsMaterialConfig.class));
        assertThat(tfs.getDomain(), is(""));
        assertThat(tfs.getDomain(), is(not(nullValue())));
    }
}
