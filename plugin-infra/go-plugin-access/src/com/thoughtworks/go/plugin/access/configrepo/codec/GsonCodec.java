package com.thoughtworks.go.plugin.access.configrepo.codec;

import com.google.gson.*;
import com.thoughtworks.go.plugin.access.configrepo.contract.material.CRMaterial;
import com.thoughtworks.go.plugin.access.configrepo.contract.tasks.CRTask;
import com.thoughtworks.go.plugin.access.configrepo.messages.ParseDirectoryResponseMessage;

public class GsonCodec {
    private Gson gson;

    public GsonCodec()
    {
        this(new GsonBuilder());
    }
    public GsonCodec(GsonBuilder builder)
    {
        // here we can register extra configurations, policies, adapters
        builder.registerTypeAdapter(CRMaterial.class,new MaterialTypeAdapter());
        builder.registerTypeAdapter(CRTask.class,new TaskTypeAdapter());

        gson = builder.create();
    }

    public Gson getGson() {
        return gson;
    }
}
