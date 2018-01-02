/*
 * Copyright 2015 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.web;

import com.google.gson.*;
import com.thoughtworks.go.util.json.JsonAware;
import com.thoughtworks.go.util.json.JsonFakeMap;
import com.thoughtworks.go.util.json.JsonUrl;
import org.springframework.context.MessageSourceResolvable;

import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Type;

public class JsonRenderer {

    public static String render(Object o) {
        return render(o, null);
    }

    public static String render(Object o, GoRequestContext context) {
        StringWriter writer = new StringWriter();
        render(o, context, writer);
        return writer.toString();
    }

    public static void render(Object o, GoRequestContext context, Writer writer) {
        if (o instanceof JsonAware) {
            o = ((JsonAware) o).toJson();
        } else if (o instanceof JsonFakeMap){
            o = ((JsonFakeMap) o).get("json");
        }
        gsonBuilder(context).toJson(o, writer);
    }

    private static Gson gsonBuilder(final GoRequestContext requestContext) {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(JsonUrl.class, new JsonSerializer<JsonUrl>() {
            @Override
            public JsonElement serialize(JsonUrl src, Type typeOfSrc, JsonSerializationContext context) {
                if (requestContext == null) {
                    return new JsonPrimitive(src.getUrl());
                } else {
                    return new JsonPrimitive(requestContext.getFullRequestPath() + src.getUrl());
                }
            }
        });

        builder.registerTypeHierarchyAdapter(MessageSourceResolvable.class, new JsonSerializer<MessageSourceResolvable>() {
            @Override
            public JsonElement serialize(MessageSourceResolvable src, Type typeOfSrc, JsonSerializationContext context) {
                if (requestContext == null) {
                    return new JsonPrimitive(src.getDefaultMessage());
                } else {
                    return new JsonPrimitive(requestContext.getMessage(src));
                }
            }
        });

        builder.serializeNulls();
        return builder.create();
    }
}
