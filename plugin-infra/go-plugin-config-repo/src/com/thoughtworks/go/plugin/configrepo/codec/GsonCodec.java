package com.thoughtworks.go.plugin.configrepo.codec;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.thoughtworks.go.plugin.configrepo.CRPartialConfig_1;

public class GsonCodec {
    private Gson gson;

    public GsonCodec()
    {
        this(new GsonBuilder());
    }
    public GsonCodec(GsonBuilder builder)
    {
        //TODO register extra configurations, policies, adapters

        gson = builder.create();
    }

    public Gson getGson() {
        return gson;
    }

    public CRPartialConfig_1 partialConfig_1FromJson(String json) {
        return this.getGson().fromJson(json,CRPartialConfig_1.class);
    }
}
