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

package com.thoughtworks.go.config.materials.git;

import java.util.HashMap;
import java.util.Map;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.ValidationContext;
import com.thoughtworks.go.config.materials.AbstractMaterialConfig;
import com.thoughtworks.go.config.materials.Filter;
import com.thoughtworks.go.config.materials.IgnoredFiles;
import com.thoughtworks.go.config.materials.ScmMaterialConfig;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class GitMaterialConfigTest {
    @Test
    public void shouldSetConfigAttributes() {
        GitMaterialConfig gitMaterialConfig = new GitMaterialConfig("");

        Map<String, String> map = new HashMap<String, String>();
        map.put(GitMaterialConfig.URL, "url");
        map.put(GitMaterialConfig.BRANCH, "some-branch");
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
        assertThat(gitMaterialConfig.filter(), is(new Filter(new IgnoredFiles("/root"), new IgnoredFiles("/**/*.help"))));
    }

    @Test
    public void validate_shouldEnsureUrlIsNotBlank() {
        GitMaterialConfig gitMaterialConfig = new GitMaterialConfig("");
        gitMaterialConfig.validate(new ValidationContext(null));
        assertThat(gitMaterialConfig.errors().on(GitMaterialConfig.URL), is("URL cannot be blank"));
    }

    @Test
    public void shouldReturnIfAttributeMapIsNull() {
        GitMaterialConfig gitMaterialConfig = new GitMaterialConfig("");
        gitMaterialConfig.setConfigAttributes(null);
        assertThat(gitMaterialConfig, is(new GitMaterialConfig("")));
    }

    @Test
    public void shouldGetAttributesWithSecureFields() {
        GitMaterialConfig git = new GitMaterialConfig("http://username:password@gitrepo.com", GitMaterialConfig.DEFAULT_BRANCH);
        Map<String, Object> attributes = git.getAttributes(true);

        assertThat((String) attributes.get("type"), is("git"));
        Map<String, Object> configuration = (Map<String, Object>) attributes.get("git-configuration");
        assertThat((String) configuration.get("url"), is("http://username:password@gitrepo.com"));
        assertThat((String) configuration.get("branch"), is(GitMaterialConfig.DEFAULT_BRANCH));
    }

    @Test
    public void shouldGetAttributesWithoutSecureFields() {
        GitMaterialConfig git = new GitMaterialConfig("http://username:password@gitrepo.com", GitMaterialConfig.DEFAULT_BRANCH);
        Map<String, Object> attributes = git.getAttributes(false);

        assertThat((String) attributes.get("type"), is("git"));
        Map<String, Object> configuration = (Map<String, Object>) attributes.get("git-configuration");
        assertThat((String) configuration.get("url"), is("http://username:******@gitrepo.com"));
        assertThat((String) configuration.get("branch"), is(GitMaterialConfig.DEFAULT_BRANCH));
    }
}
