/*
 * Copyright Thoughtworks, Inc.
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
        return determineJsonElementForDistinguishingImplementers(json, context, TYPE);
    }

    @Override
    protected Class<?> classForName(String typeName, String origin) {
        return switch (typeName) {
            case CRDependencyMaterial.TYPE_NAME -> CRDependencyMaterial.class;
            case CRPackageMaterial.TYPE_NAME -> CRPackageMaterial.class;
            case CRPluggableScmMaterial.TYPE_NAME -> CRPluggableScmMaterial.class;
            case CRGitMaterial.TYPE_NAME -> CRGitMaterial.class;
            case CRHgMaterial.TYPE_NAME -> CRHgMaterial.class;
            case CRSvnMaterial.TYPE_NAME -> CRSvnMaterial.class;
            case CRP4Material.TYPE_NAME -> CRP4Material.class;
            case CRTfsMaterial.TYPE_NAME -> CRTfsMaterial.class;
            case CRConfigMaterial.TYPE_NAME -> CRConfigMaterial.class;
            default -> throw new JsonParseException(
                String.format("Invalid or unknown material type '%s'", typeName));
        };
    }

    @Override
    public JsonElement serialize(CRMaterial material, Type type, JsonSerializationContext context) {
        return context.serialize(material);
    }
}
