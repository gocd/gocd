package com.thoughtworks.go.plugin.access.configrepo.codec;

import com.google.gson.*;
import com.thoughtworks.go.plugin.access.configrepo.contract.material.*;

import java.lang.reflect.Type;

public class MaterialTypeAdapter implements JsonDeserializer<CRMaterial>, JsonSerializer<CRMaterial>  {

    private static final String TYPE = "type";

    @Override
    public CRMaterial deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject =  json.getAsJsonObject();
        JsonPrimitive prim = (JsonPrimitive) jsonObject.get(TYPE);
        String typeName = prim.getAsString();

        Class<?> klass = classForName(typeName);
        return context.deserialize(jsonObject, klass);
    }

    private String getTypeName(CRMaterial src) {
        return src.typeName();
    }

    private Class<?> classForName(String typeName) {
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
