package com.thoughtworks.go.plugin.access.configrepo.codec;

import com.google.gson.*;
import com.thoughtworks.go.plugin.access.configrepo.contract.tasks.*;

import java.lang.reflect.Type;

public class TaskTypeAdapter implements JsonDeserializer<CRTask>, JsonSerializer<CRTask> {
    private static final String TYPE = "type";

    @Override
    public CRTask deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject =  json.getAsJsonObject();
        JsonPrimitive prim = (JsonPrimitive) jsonObject.get(TYPE);
        String typeName = prim.getAsString();

        Class<?> klass = classForName(typeName);
        return context.deserialize(jsonObject, klass);
    }
    private Class<?> classForName(String typeName) {
        if(typeName.equals(CRExecTask.TYPE_NAME))
            return CRExecTask.class;
        if(typeName.equals(CRBuildTask.RAKE_TYPE_NAME))
            return CRBuildTask.class;
        if(typeName.equals(CRBuildTask.ANT_TYPE_NAME))
            return CRBuildTask.class;
        if(typeName.equals(CRBuildTask.NANT_TYPE_NAME))
            return CRNantTask.class;
        if(typeName.equals(CRPluggableTask.TYPE_NAME))
            return CRPluggableTask.class;
        if(typeName.equals(CRFetchArtifactTask.TYPE_NAME))
            return CRFetchArtifactTask.class;
        else
            throw new JsonParseException(
                    String.format("Invalid or unknown task type '%s'",typeName));
    }

    @Override
    public JsonElement serialize(CRTask crTask, Type type, JsonSerializationContext context) {
        JsonObject retValue = context.serialize(crTask).getAsJsonObject();
        CRTask onCancel = crTask.getOnCancel();
        if(onCancel != null) {
            retValue.remove("onCancel");
            retValue.add("onCancel", context.serialize(onCancel));
        }

        return retValue;
    }
}
