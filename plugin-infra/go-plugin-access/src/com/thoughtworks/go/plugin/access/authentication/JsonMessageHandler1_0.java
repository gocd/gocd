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
import com.thoughtworks.go.plugin.access.authentication.model.AuthenticationPluginConfiguration;
import com.thoughtworks.go.plugin.access.authentication.model.User;
import com.thoughtworks.go.plugin.api.response.Result;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonMessageHandler1_0 implements JsonMessageHandler {
    @Override
    public AuthenticationPluginConfiguration responseMessageForPluginConfiguration(String responseBody) {
        Map<String, Object> map = new Gson().fromJson(responseBody, Map.class);
        return new AuthenticationPluginConfiguration((String) map.get("display-name"), (Boolean) map.get("supports-password-based-authentication"), (Boolean) map.get("supports-user-search"));
    }

    @Override
    public String requestMessageForAuthenticateUser(String username, String password) {
        Map<String, Object> requestMap = new HashMap<String, Object>();
        requestMap.put("username", username);
        requestMap.put("password", password);
        return new Gson().toJson(requestMap);
    }

    @Override
    public Result responseMessageForAuthenticateUser(String responseBody) {
        Map<String, Object> responseMap = new Gson().fromJson(responseBody, Map.class);

        Result result = new Result();
        if (responseMap.get("status").equals("success")) {
            result.withSuccessMessages((List<String>) responseMap.get("messages"));
        } else {
            result.withErrorMessages((List<String>) responseMap.get("messages"));
        }
        return result;
    }

    @Override
    public String requestMessageForGetUserDetails(String username) {
        Map<String, Object> requestMap = new HashMap<String, Object>();
        requestMap.put("username", username);
        return new Gson().toJson(requestMap);
    }

    @Override
    public User responseMessageForGetUserDetails(String responseBody) {
        Map<String, String> userMap = new Gson().fromJson(responseBody, Map.class);
        User user = new User(userMap.get("id"), userMap.get("username"), userMap.get("first-name"), userMap.get("last-name"), userMap.get("email-id"));
        return user;
    }

    @Override
    public String requestMessageForSearchUser(String searchTerm) {
        Map<String, Object> requestMap = new HashMap<String, Object>();
        requestMap.put("search-term", searchTerm);
        return new Gson().toJson(requestMap);
    }

    @Override
    public List<User> responseMessageForSearchUser(String responseBody) {
        List<Map<String, String>> responseList = new Gson().fromJson(responseBody, List.class);
        List<User> searchResults = new ArrayList<User>();
        for (Map<String, String> userMap : responseList) {
            User user = new User(userMap.get("id"), userMap.get("username"), userMap.get("first-name"), userMap.get("last-name"), userMap.get("email-id"));
            searchResults.add(user);
        }
        return searchResults;
    }
}
