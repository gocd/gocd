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

import com.google.gson.*;
import com.thoughtworks.go.plugin.configrepo.contract.material.*;

import java.lang.reflect.Type;

public class MaterialTypeAdapter extends TypeAdapter implements JsonDeserializer<CRMaterial>, JsonSerializer<CRMaterial>  {

    private static final String TYPE = "type";

    @Override
    public CRMaterial deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
        return determineJsonElementForDistinguishingImplementers(json, context, TYPE, ARTIFACT_ORIGIN);
    }

    @Override
    protected Class<?> classForName(String typeName, String origin) {
        if(typeName.equals(CRDependencyMaterial.TYPE_NAME))
            return CRDependencyMaterial.class;
        if(typeName.equals(CRPackageMaterial.TYPE_NAME))
            return CRPackageMaterial.class;
        if(typeName.equals(CRPluggableScmMaterial.TYPE_NAME))
            return CRPluggableScmMaterial.class;
        if(typeName.equals(CRGitMaterial.TYPE_NAME))
            return CRGitMaterial.class;
        if(typeName.equals(CRHgMaterial.TYPE_NAME))
            return CRHgMaterial.class;
        if(typeName.equals(CRSvnMaterial.TYPE_NAME))
            return CRSvnMaterial.class;
        if(typeName.equals(CRP4Material.TYPE_NAME))
            return CRP4Material.class;
        if(typeName.equals(CRTfsMaterial.TYPE_NAME))
            return CRTfsMaterial.class;
        if(typeName.equals(CRConfigMaterial.TYPE_NAME))
            return CRConfigMaterial.class;
        else
            throw new JsonParseException(
                    String.format("Invalid or unknown material type '%s'",typeName));
    }

    @Override
    public JsonElement serialize(CRMaterial material, Type type, JsonSerializationContext context) {
        return context.serialize(material);
    }
}
