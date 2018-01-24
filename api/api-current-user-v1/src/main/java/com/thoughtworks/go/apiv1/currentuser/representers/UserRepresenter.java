/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.apiv1.currentuser.representers;

import com.google.common.collect.ImmutableMap;
import com.thoughtworks.go.api.representers.JsonWriter;
import com.thoughtworks.go.domain.User;
import com.thoughtworks.go.spark.RequestContext;

import java.util.Map;


public class UserRepresenter {
    public static Map toJSON(User user, RequestContext requestContext) {
        JsonWriter jsonWriter = new JsonWriter(requestContext);
        addLinks(user, jsonWriter);
        jsonWriter.add("login_name", user.getName());
        jsonWriter.add("display_name", user.getDisplayName());
        jsonWriter.add("enabled", user.isEnabled());
        jsonWriter.add("email", user.getEmail());
        jsonWriter.add("email_me", user.isEmailMe());
        jsonWriter.add("checkin_aliases", user.getMatchers());
        return jsonWriter.getAsMap();
    }

    private static void addLinks(User user, JsonWriter jsonWriter) {
        jsonWriter.addDocLink("https://api.gocd.org/#users");
        jsonWriter.addLink("self", "/api/users/${loginName}", ImmutableMap.of("loginName", user.getName()));
        jsonWriter.addLink("find", "/api/users/${loginName}", ImmutableMap.of("loginName", ":login_name"));
        jsonWriter.addLink("current_user", "/api/current_user");
    }
}
