/*
 * Copyright 2019 ThoughtWorks, Inc.
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

package com.thoughtworks.go.domain.materials.git;

import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.domain.MaterialInstance;
import com.thoughtworks.go.domain.materials.Material;

public class GitMaterialInstance extends MaterialInstance {
    protected GitMaterialInstance() {
    }

    public GitMaterialInstance(String url, String userName, String branch, String submoduleFolder, String flyweightName) {
        super(url, userName, null, null, null, null, branch, submoduleFolder, flyweightName, null, null, null, null);
    }

    @Override
    public Material toOldMaterial(String name, String folder, String password) {
        GitMaterial git = new GitMaterial(url, branch, folder);
        setName(name, git);
        git.setUserName(username);
        git.setSubmoduleFolder(submoduleFolder);
        git.setId(id);
        return git;
    }
}
