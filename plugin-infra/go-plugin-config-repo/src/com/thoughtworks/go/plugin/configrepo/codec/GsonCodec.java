package com.thoughtworks.go.plugin.configrepo.codec;

import com.google.gson.*;
import com.thoughtworks.go.plugin.configrepo.CRPartialConfig_1;
import com.thoughtworks.go.plugin.configrepo.material.CRMaterial_1;
import com.thoughtworks.go.plugin.configrepo.tasks.CRTask_1;

import java.lang.reflect.Field;

public class GsonCodec {
    private Gson gson;

    public GsonCodec()
    {
        this(new GsonBuilder());
    }
    public GsonCodec(GsonBuilder builder)
    {
        //TODO register extra configurations, policies, adapters
        builder.registerTypeAdapter(CRMaterial_1.class,new MaterialTypeAdapter());
        builder.registerTypeAdapter(CRTask_1.class,new TaskTypeAdapter());

        gson = builder.create();
    }

    public Gson getGson() {
        return gson;
    }

    public CRPartialConfig_1 partialConfig_1FromJson(String json) {
        return this.getGson().fromJson(json,CRPartialConfig_1.class);
    }
}
