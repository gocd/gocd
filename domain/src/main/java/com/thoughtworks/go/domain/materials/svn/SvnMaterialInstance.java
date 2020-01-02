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
package com.thoughtworks.go.domain.materials.svn;

import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.domain.MaterialInstance;
import com.thoughtworks.go.domain.materials.Material;

public class SvnMaterialInstance extends MaterialInstance {

    protected SvnMaterialInstance() {
        super();
    }

    public SvnMaterialInstance(String url, String userName, String flyweightName, final Boolean checkExternals) {
        super(url, userName, null, null, null, null, null, null, flyweightName, checkExternals, null, null, null);
    }

    @Override public Material toOldMaterial(String name, String folder, String password) {
        SvnMaterial svn = new SvnMaterial(url, username, password, checkExternals, folder);
        setName(name, svn);
        svn.setId(id);
        return svn;
    }
}
