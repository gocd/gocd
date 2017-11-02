/*
 * Copyright 2017 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.websocket.browser.subscription;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.thoughtworks.go.server.websocket.browser.BrowserWebSocket;
import com.thoughtworks.go.server.websocket.browser.subscription.jobstatuschange.JobStatusChange;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;

public abstract class SubscriptionMessage {
    private static final List<Class<? extends SubscriptionMessage>> CLASSES = Arrays.asList(
            ServerHealthMessageCount.class,
            JobStatusChange.class
    );

    public static final Type TYPE = new TypeToken<List<SubscriptionMessage>>() {
    }.getType();
    private static Gson GSON;

    public static List<SubscriptionMessage> fromJSON(String json) {
        return gson().fromJson(json, TYPE);
    }

    private static synchronized Gson gson() {
        if (GSON == null) {
            RuntimeTypeAdapterFactory<SubscriptionMessage> typeAdapterFactory = RuntimeTypeAdapterFactory.of(SubscriptionMessage.class);
            for (Class<? extends SubscriptionMessage> aClass : CLASSES) {
                typeAdapterFactory.registerSubtype(aClass);
            }
            GSON = new GsonBuilder()
                    .registerTypeAdapterFactory(typeAdapterFactory)
                    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                    .excludeFieldsWithoutExposeAnnotation()
                    .create();
        }
        return GSON;
    }


    public abstract void subscribe(WebSocketSubscriptionManager webSocketSubscriptionManager, BrowserWebSocket webSocket) throws Exception;

    public abstract boolean isAuthorized(WebSocketSubscriptionManager webSocketSubscriptionManager, BrowserWebSocket webSocket);
}
