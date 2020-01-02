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
package com.thoughtworks.go.domain.materials;

import com.thoughtworks.go.domain.MaterialInstance;

public class TestingMaterialInstance extends MaterialInstance {

    protected TestingMaterialInstance() {
    }

    public TestingMaterialInstance(String url, String flyweightName) {
        super(url, null, null, null, null, false, null, null, flyweightName, false, null, null, null);
    }

    @Override public Material toOldMaterial(String name, String folder, String password) {
        TestingMaterial testing = new TestingMaterial();
        testing.setUrl(url);
        testing.setId(id);
        return testing;
    }
}
