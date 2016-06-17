/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.config.materials.git;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.ConfigSaveValidationContext;
import com.thoughtworks.go.config.materials.AbstractMaterialConfig;
import com.thoughtworks.go.config.materials.Filter;
import com.thoughtworks.go.config.materials.IgnoredFiles;
import com.thoughtworks.go.config.materials.ScmMaterialConfig;
import com.thoughtworks.go.util.command.UrlArgument;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

public class GitMaterialConfigTest {
    @Test
    public void shouldSetConfigAttributes() {
        GitMaterialConfig gitMaterialConfig = new GitMaterialConfig("");

        Map<String, String> map = new HashMap<String, String>();
        map.put(GitMaterialConfig.URL, "url");
        map.put(GitMaterialConfig.BRANCH, "some-branch");
        map.put(GitMaterialConfig.SHALLOW_CLONE, "true");
        map.put(ScmMaterialConfig.FOLDER, "folder");
        map.put(ScmMaterialConfig.AUTO_UPDATE, null);
        map.put(ScmMaterialConfig.FILTER, "/root,/**/*.help");
        map.put(AbstractMaterialConfig.MATERIAL_NAME, "material-name");

        gitMaterialConfig.setConfigAttributes(map);

        assertThat(gitMaterialConfig.getUrl(), is("url"));
        assertThat(gitMaterialConfig.getFolder(), is("folder"));
        assertThat(gitMaterialConfig.getBranch(), is("some-branch"));
        assertThat(gitMaterialConfig.getName(), is(new CaseInsensitiveString("material-name")));
        assertThat(gitMaterialConfig.isAutoUpdate(), is(false));
        assertThat(gitMaterialConfig.isShallowClone(), is(true));
        assertThat(gitMaterialConfig.filter(), is(new Filter(new IgnoredFiles("/root"), new IgnoredFiles("/**/*.help"))));
    }


    @Test
    public void byDefaultShallowCloneShouldBeOff() {
        assertThat(new GitMaterialConfig("http://url", "foo").isShallowClone(), is(false));
        assertThat(new GitMaterialConfig("http://url", "foo", false).isShallowClone(), is(false));
        assertThat(new GitMaterialConfig("http://url", "foo", null).isShallowClone(), is(false));
        assertThat(new GitMaterialConfig("http://url", "foo", true).isShallowClone(), is(true));
    }

    @Test
    public void validate_shouldEnsureUrlIsNotBlank() {
        GitMaterialConfig gitMaterialConfig = new GitMaterialConfig("");
        gitMaterialConfig.validate(new ConfigSaveValidationContext(null));
        assertThat(gitMaterialConfig.errors().on(GitMaterialConfig.URL), is("URL cannot be blank"));
    }

    @Test
    public void shouldReturnIfAttributeMapIsNull() {
        GitMaterialConfig gitMaterialConfig = new GitMaterialConfig("");
        gitMaterialConfig.setConfigAttributes(null);
        assertThat(gitMaterialConfig, is(new GitMaterialConfig("")));
    }

    @Test
    public void shouldReturnTheUrl() {
        String url = "git@github.com/my/repo";
        GitMaterialConfig config = new GitMaterialConfig(url);

        assertThat(config.getUrl(), is(url));
    }

    @Test
    public void shouldReturnNullIfUrlForMaterialNotSpecified() {
        GitMaterialConfig config = new GitMaterialConfig();

        assertNull(config.getUrl());
    }

    @Test
    public void shouldSetUrlForAMaterial() {
        String url = "git@github.com/my/repo";
        GitMaterialConfig config = new GitMaterialConfig();

        config.setUrl(url);

        assertThat(config.getUrl(), is(url));
    }

    @Test
    public void shouldHandleNullWhenSettingUrlForAMaterial() {
        GitMaterialConfig config = new GitMaterialConfig();

        config.setUrl(null);

        assertNull(config.getUrl());
    }

    @Test
    public void shouldHandleNullUrlAtTheTimeOfGitMaterialConfigCreation() {
        GitMaterialConfig config = new GitMaterialConfig(null);

        assertNull(config.getUrl());
    }

    @Test
    public void shouldHandleNullBranchAtTheTimeOfMaterialConfigCreation() {
        GitMaterialConfig config1 = new GitMaterialConfig("http://url", null);
        GitMaterialConfig config2 = new GitMaterialConfig(new UrlArgument("http://url"), null, "sub1", true, new Filter(), false, "folder", new CaseInsensitiveString("git"), false);

        assertThat(config1.getBranch(), is("master"));
        assertThat(config2.getBranch(), is("master"));
    }

    @Test
    public void shouldHandleNullBranchWhileSettingConfigAttributes() {
        GitMaterialConfig gitMaterialConfig = new GitMaterialConfig("http://url", "foo");
        gitMaterialConfig.setConfigAttributes(Collections.singletonMap(GitMaterialConfig.BRANCH, null));
        assertThat(gitMaterialConfig.getBranch(), is("master"));
    }

    @Test
    public void shouldHandleEmptyBranchWhileSettingConfigAttributes() {
        GitMaterialConfig gitMaterialConfig = new GitMaterialConfig("http://url", "foo");
        gitMaterialConfig.setConfigAttributes(Collections.singletonMap(GitMaterialConfig.BRANCH, "     "));
        assertThat(gitMaterialConfig.getBranch(), is("master"));
    }
}
