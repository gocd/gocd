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

package com.thoughtworks.go.server.websocket.browser.subscription.request;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.thoughtworks.go.server.websocket.browser.subscription.JobStatusChange;
import com.thoughtworks.go.server.websocket.browser.subscription.RuntimeTypeAdapterFactory;
import com.thoughtworks.go.server.websocket.browser.subscription.ServerHealthMessageCount;
import com.thoughtworks.go.server.websocket.browser.subscription.SubscriptionMessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class SubscriptionRequest {
    private static final List<Class<? extends SubscriptionMessage>> CLASSES = Arrays.asList(
            ServerHealthMessageCount.class,
            JobStatusChange.class
    );

    private static Gson GSON;
    @Expose
    private final SubscriptionRequestAction action;

    @Expose
    private final ArrayList<SubscriptionMessage> events;

    public SubscriptionRequest(SubscriptionRequestAction action, ArrayList<SubscriptionMessage> events) {
        this.action = action;
        this.events = events;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SubscriptionRequest)) return false;
        SubscriptionRequest that = (SubscriptionRequest) o;
        return Objects.equals(action, that.action) &&
                Objects.equals(events, that.events);
    }

    @Override
    public int hashCode() {
        return Objects.hash(action, events);
    }

    public SubscriptionRequestAction getAction() {
        return action;
    }

    public ArrayList<SubscriptionMessage> getEvents() {
        return events;
    }

    public static SubscriptionRequest fromJSON(String input) {
        return gson().fromJson(input, SubscriptionRequest.class);
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
}
