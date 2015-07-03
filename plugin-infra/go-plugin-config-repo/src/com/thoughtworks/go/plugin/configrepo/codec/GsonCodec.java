package com.thoughtworks.go.plugin.configrepo.codec;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

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
}
