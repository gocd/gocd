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

package com.thoughtworks.go.server.materials.postcommit.git;

import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.server.materials.postcommit.UrlMatchers;
import com.thoughtworks.go.server.materials.postcommit.PostCommitHookImplementer;
import org.apache.commons.lang.StringUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class GitPostCommitHookImplementer implements PostCommitHookImplementer {

    static final String REPO_URL_PARAM_KEY = "repository_url";
    private final UrlMatchers validators = new UrlMatchers();

    @Override
    public Set<Material> prune(Set<Material> materials, Map params) {
        HashSet<Material> prunedCollection = new HashSet<>();
        if (params.containsKey(REPO_URL_PARAM_KEY)) {
            String paramRepoUrl = (String) params.get(REPO_URL_PARAM_KEY);
            if (StringUtils.isNotBlank(paramRepoUrl)) {
                for (Material material : materials) {
                    if (material instanceof GitMaterial && isUrlEqual(paramRepoUrl, (GitMaterial) material)) {
                        prunedCollection.add(material);
                    }
                }
            }
            return prunedCollection;
        } else {
            return prunedCollection;
        }
    }

    boolean isUrlEqual(String paramRepoUrl, GitMaterial material) {
        String materialUrl = material.getUrlArgument().forCommandline();
        return validators.perform(paramRepoUrl, materialUrl);
    }
}
