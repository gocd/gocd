/*
 * Copyright 2020 ThoughtWorks, Inc.
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
package com.thoughtworks.go.domain.materials.mercurial;

import com.thoughtworks.go.config.materials.mercurial.HgMaterial;
import com.thoughtworks.go.domain.MaterialInstance;
import com.thoughtworks.go.domain.materials.Material;

public class HgMaterialInstance extends MaterialInstance {
    protected HgMaterialInstance() {
    }

    public HgMaterialInstance(String url, String username, String branch, String flyweightName) {
        super(url, username, null, null, null, null, branch, null, flyweightName, null, null, null, null);
    }

    @Override
    public Material toOldMaterial(String name, String folder, String password) {
        HgMaterial hg = new HgMaterial(url, folder);
        setName(name, hg);
        hg.setId(id);
        hg.setUserName(username);
        hg.setPassword(password);
        hg.setBranch(branch);
        return hg;
    }
}
