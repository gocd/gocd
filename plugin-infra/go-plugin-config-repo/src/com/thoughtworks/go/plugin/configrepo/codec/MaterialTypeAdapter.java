package com.thoughtworks.go.plugin.configrepo.codec;

import com.google.gson.*;
import com.thoughtworks.go.plugin.configrepo.material.CRDependencyMaterial_1;
import com.thoughtworks.go.plugin.configrepo.material.CRMaterial_1;
import com.thoughtworks.go.plugin.configrepo.material.CRPackageMaterial_1;

import java.lang.reflect.Type;

public class MaterialTypeAdapter implements JsonDeserializer<CRMaterial_1> {

    private static final String TYPE = "type";

    @Override
    public CRMaterial_1 deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject =  json.getAsJsonObject();
        JsonPrimitive prim = (JsonPrimitive) jsonObject.get(TYPE);
        String typeName = prim.getAsString();

        Class<?> klass = classForName(typeName);
        return context.deserialize(jsonObject, klass);
    }

    private String getTypeName(CRMaterial_1 src) {
        return src.typeName();
    }

    private Class<?> classForName(String typeName) {
        if(typeName.equals(CRDependencyMaterial_1.TYPE_NAME))
            return CRDependencyMaterial_1.class;
        if(typeName.equals(CRPackageMaterial_1.TYPE_NAME))
            return CRPackageMaterial_1.class;
        else
            throw new JsonParseException(
                    String.format("Invalid or unknown material type '%s'",typeName));
    }
}
