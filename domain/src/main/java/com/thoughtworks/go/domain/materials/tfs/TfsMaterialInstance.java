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
package com.thoughtworks.go.domain.materials.tfs;
import com.thoughtworks.go.config.materials.tfs.TfsMaterial;
import com.thoughtworks.go.domain.MaterialInstance;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.util.command.UrlArgument;

public class TfsMaterialInstance extends MaterialInstance {

    private TfsMaterialInstance() {
        super();
    }

    public TfsMaterialInstance(String url, String userName, String domain, String projectPath, final String flyweightName) {
        super(url, userName, null, null, null, null, null, null, flyweightName, null, projectPath, domain, null);
    }

    @Override public Material toOldMaterial(String name, String folder, String password) {
        TfsMaterial tfsMaterial = new TfsMaterial(new GoCipher(), new UrlArgument(url), username, domain, password, projectPath);
        tfsMaterial.setFolder(folder);
        setName(name,tfsMaterial);
        return tfsMaterial;
    }
}
