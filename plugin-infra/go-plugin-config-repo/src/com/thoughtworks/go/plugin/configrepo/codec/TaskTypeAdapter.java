package com.thoughtworks.go.plugin.configrepo.codec;

import com.google.gson.*;
import com.thoughtworks.go.plugin.configrepo.tasks.*;

import java.lang.reflect.Type;

public class TaskTypeAdapter implements JsonDeserializer<CRTask_1> {
    private static final String TYPE = "type";

    @Override
    public CRTask_1 deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject =  json.getAsJsonObject();
        JsonPrimitive prim = (JsonPrimitive) jsonObject.get(TYPE);
        String typeName = prim.getAsString();

        Class<?> klass = classForName(typeName);
        return context.deserialize(jsonObject, klass);
    }
    private Class<?> classForName(String typeName) {
        if(typeName.equals(CRExecTask_1.TYPE_NAME))
            return CRExecTask_1.class;
        if(typeName.equals(CRBuildTask_1.RAKE_TYPE_NAME))
            return CRBuildTask_1.class;
        if(typeName.equals(CRBuildTask_1.ANT_TYPE_NAME))
            return CRBuildTask_1.class;
        if(typeName.equals(CRBuildTask_1.NANT_TYPE_NAME))
            return CRNantTask_1.class;
        if(typeName.equals(CRPluggableTask_1.TYPE_NAME))
            return CRPluggableTask_1.class;
        else
            throw new JsonParseException(
                    String.format("Invalid or unknown material type '%s'",typeName));
    }
}
