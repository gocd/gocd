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
package com.thoughtworks.go.domain.materials.perforce;

import com.thoughtworks.go.config.materials.perforce.P4Material;
import com.thoughtworks.go.domain.MaterialInstance;
import com.thoughtworks.go.domain.materials.Material;
import org.apache.commons.lang3.StringUtils;

public class P4MaterialInstance extends MaterialInstance {
    protected P4MaterialInstance() {
    }

    public P4MaterialInstance(String serverAndPort, String userName, String view, Boolean useTickets, String flyweightName) {
        super(serverAndPort, userName, null, null, view, useTickets, null, null, flyweightName, null, null, null, null);
    }

    @Override public Material toOldMaterial(String name, String folder, String password) {
        P4Material p4 = new P4Material(url, view, username, folder);
        setName(name, p4);
        p4.setFolder(StringUtils.isBlank(folder) ? null : folder);
        p4.setPassword(password);
        p4.setUseTickets(getUseTickets());
        p4.setId(id);
        return p4;
    }
}
