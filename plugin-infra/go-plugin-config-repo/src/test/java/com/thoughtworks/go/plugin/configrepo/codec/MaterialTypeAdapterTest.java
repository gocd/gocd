/*
 * Copyright 2021 ThoughtWorks, Inc.
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

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.thoughtworks.go.plugin.configrepo.contract.material.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Type;

public class MaterialTypeAdapterTest {

    private MaterialTypeAdapter materialTypeAdapter;

    @Mock
    private JsonDeserializationContext jsonDeserializationContext;

    @Mock
    private Type type;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        materialTypeAdapter = new MaterialTypeAdapter();
    }

    @Test
    public void shouldDeserializeGitMaterialType() throws Exception {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("type", "git");
        materialTypeAdapter.deserialize(jsonObject, type, jsonDeserializationContext);

        Mockito.verify(jsonDeserializationContext).deserialize(jsonObject, CRGitMaterial.class);
    }

    @Test
    public void shouldDeserializeHgMaterialType() throws Exception {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("type", "hg");
        materialTypeAdapter.deserialize(jsonObject, type, jsonDeserializationContext);

        Mockito.verify(jsonDeserializationContext).deserialize(jsonObject, CRHgMaterial.class);
    }

    @Test
    public void shouldDeserializeP4tMaterialType() throws Exception {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("type", "p4");
        materialTypeAdapter.deserialize(jsonObject, type, jsonDeserializationContext);

        Mockito.verify(jsonDeserializationContext).deserialize(jsonObject, CRP4Material.class);
    }

    @Test
    public void shouldDeserializeTfsMaterialType() throws Exception {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("type", "tfs");
        materialTypeAdapter.deserialize(jsonObject, type, jsonDeserializationContext);

        Mockito.verify(jsonDeserializationContext).deserialize(jsonObject, CRTfsMaterial.class);
    }

    @Test
    public void shouldDeserializeSvnMaterialType() throws Exception {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("type", "svn");
        materialTypeAdapter.deserialize(jsonObject, type, jsonDeserializationContext);

        Mockito.verify(jsonDeserializationContext).deserialize(jsonObject, CRSvnMaterial.class);
    }

    @Test
    public void shouldDeserializePackageMaterialType() throws Exception {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("type", "package");
        materialTypeAdapter.deserialize(jsonObject, type, jsonDeserializationContext);

        Mockito.verify(jsonDeserializationContext).deserialize(jsonObject, CRPackageMaterial.class);
    }

    @Test
    public void shouldDeserializePluggableScmMaterialType() throws Exception {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("type", "plugin");
        materialTypeAdapter.deserialize(jsonObject, type, jsonDeserializationContext);

        Mockito.verify(jsonDeserializationContext).deserialize(jsonObject, CRPluggableScmMaterial.class);
    }
}
