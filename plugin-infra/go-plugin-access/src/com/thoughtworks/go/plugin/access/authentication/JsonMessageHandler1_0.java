/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.plugin.access.authentication;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.thoughtworks.go.plugin.access.authentication.model.AuthenticationPluginConfiguration;
import com.thoughtworks.go.plugin.access.authentication.model.User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonMessageHandler1_0 implements JsonMessageHandler {
    @Override
    public AuthenticationPluginConfiguration responseMessageForPluginConfiguration(String responseBody) {
        Map<String, Object> map = new Gson().fromJson(responseBody, Map.class);
        return new AuthenticationPluginConfiguration((String) map.get("display-name"), (Boolean) map.get("supports-user-search"));
    }

    @Override
    public String requestMessageForSearchUser(String searchTerm) {
        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
        Map<String, Object> requestMap = new HashMap<String, Object>();
        requestMap.put("search-term", searchTerm);
        return gson.toJson(requestMap);
    }

    @Override
    public List<User> responseMessageForSearchUser(String responseBody) {
        return null;
    }
}
