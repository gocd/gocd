/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.websocket;

import com.google.gson.*;
import com.thoughtworks.go.domain.FetchHandler;
import com.thoughtworks.go.domain.JobPlan;
import com.thoughtworks.go.domain.MaterialInstance;
import com.thoughtworks.go.domain.builder.Builder;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.Modifications;

import java.lang.reflect.Modifier;
import java.lang.reflect.Type;

public class JsonMessage {
    private static Gson gson;

    static {
        GsonBuilder builder = new GsonBuilder()
                .registerTypeAdapter(Builder.class, new ObjectConverter())
                .registerTypeAdapter(JobPlan.class, new ObjectConverter())
                .registerTypeAdapter(Material.class, new ObjectConverter())
                .registerTypeAdapter(Message.class, new MessageConverter())
                .registerTypeAdapter(FetchHandler.class, new ObjectConverter())
                .registerTypeAdapter(MaterialInstance.class, new ObjectConverter())
                .registerTypeAdapter(Modifications.class, new ModificationsConverter())
                .excludeFieldsWithModifiers(Modifier.TRANSIENT, Modifier.STATIC);
        gson = builder.create();
    }

    private static class MessageConverter implements JsonSerializer<Message>, JsonDeserializer<Message> {
        @Override
        public Message deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();

            Action action = Action.valueOf(obj.get("action").getAsString());
            Object data = JsonMessage.deserialize(obj.get("data"));
            return new Message(action, data);
        }

        @Override
        public JsonElement serialize(Message src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject obj = new JsonObject();
            obj.addProperty("action", src.getAction().toString());
            obj.add("data", JsonMessage.serialize(src.getData()));
            return obj;
        }
    }

    private static class ModificationsConverter implements JsonSerializer<Modifications>, JsonDeserializer<Modifications> {
        @Override
        public Modifications deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            Modifications obj = new Modifications();
            obj.add((Modification) JsonMessage.deserialize(json));
            return obj;
        }

        @Override
        public JsonElement serialize(Modifications src, Type typeOfSrc, JsonSerializationContext context) {
            return JsonMessage.serialize(src.get(0));
        }
    }

    private static class ObjectConverter implements JsonSerializer<Object>, JsonDeserializer<Object> {
        @Override
        public Object deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return JsonMessage.deserialize(json);
        }

        @Override
        public JsonElement serialize(Object src, Type typeOfSrc, JsonSerializationContext context) {
            return JsonMessage.serialize(src);
        }
    }

    public static String encode(Message msg) {
        return getGson().toJsonTree(msg).toString();
    }

    public static Message decode(String message) {
        return getGson().fromJson(message, Message.class);
    }

    private static JsonElement serialize(Object src) {
        if (src == null) {
            return null;
        }
        JsonObject obj = new JsonObject();
        obj.addProperty("type", src.getClass().getCanonicalName());
        obj.add("data", getGson().toJsonTree(src));
        return obj;
    }

    private static Object deserialize(JsonElement json) {
        if (json == null) {
            return null;
        }
        JsonObject object = json.getAsJsonObject();
        String type = object.get("type").getAsString();
        try {
            Class k = Class.forName(type);
            JsonElement data = object.get("data");
            return getGson().fromJson(data.isJsonPrimitive() ? data.getAsJsonPrimitive() : data.getAsJsonObject(), k);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static Gson getGson() {
        return gson;
    }
}
