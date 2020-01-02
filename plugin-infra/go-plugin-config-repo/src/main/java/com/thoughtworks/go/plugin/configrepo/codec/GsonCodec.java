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
package com.thoughtworks.go.plugin.configrepo.codec;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.thoughtworks.go.plugin.configrepo.contract.CRArtifact;
import com.thoughtworks.go.plugin.configrepo.contract.material.CRMaterial;
import com.thoughtworks.go.plugin.configrepo.contract.tasks.CRTask;

public class GsonCodec {
    private Gson gson;

    public GsonCodec()
    {
        this(new GsonBuilder());
    }

    public GsonCodec(GsonBuilder builder) {
        // here we can register extra configurations, policies, adapters
        builder.registerTypeAdapter(CRMaterial.class, new MaterialTypeAdapter());
        builder.registerTypeAdapter(CRTask.class, new TaskTypeAdapter());
        builder.registerTypeAdapter(CRArtifact.class, new ArtifactTypeAdapter());
        builder.excludeFieldsWithoutExposeAnnotation();

        gson = builder.create();
    }

    public Gson getGson() {
        return gson;
    }
}
