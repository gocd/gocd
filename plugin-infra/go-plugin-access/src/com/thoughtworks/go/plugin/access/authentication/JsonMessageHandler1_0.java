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
import com.thoughtworks.go.util.StringUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonMessageHandler1_0 implements JsonMessageHandler {
    @Override
    public AuthenticationPluginConfiguration responseMessageForPluginConfiguration(String responseBody) {
        Map map;
        try {
            map = parseResponseToMap(responseBody);
        } catch (Exception e) {
            throw new RuntimeException("Plugin configuration should be returned as a map");
        }

        return toPluginConfiguration(map);
    }

    @Override
    public String requestMessageForAuthenticateUser(String username, String password) {
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("username", username);
        requestMap.put("password", password);
        return toJsonString(requestMap);
    }

    @Override
    public User responseMessageForAuthenticateUser(String responseBody) {
        Map map;
        try {
            map = parseResponseToMap(responseBody);
        } catch (Exception e) {
            throw new RuntimeException("User should be returned as a map");
        }

        if (map == null || map.isEmpty()) {
            return null;
        }

        Map userMap;
        try {
            userMap = (Map) map.get("user");
        } catch (Exception e) {
            throw new RuntimeException("User should be returned as a map");
        }

        User user = toUser(userMap);
        return user;
    }

    @Override
    public String requestMessageForSearchUser(String searchTerm) {
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("search-term", searchTerm);
        return toJsonString(requestMap);
    }

    @Override
    public List<User> responseMessageForSearchUser(String responseBody) {
        List<Map> list;
        try {
            list = parseResponseToList(responseBody);
        } catch (Exception e) {
            throw new RuntimeException("Search results should be returned as a list");
        }

        List<User> searchResults = new ArrayList<>();

        if (list == null || list.isEmpty()) {
            return searchResults;
        }

        for (Map userMap : list) {
            User user = toUser(userMap);
            searchResults.add(user);
        }
        return searchResults;
    }

    private static String toJsonString(Object object) {
        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
        return gson.toJson(object);
    }

    private List<Map> parseResponseToList(String responseBody) {
        return (List<Map>) new GsonBuilder().create().fromJson(responseBody, Object.class);
    }

    private Map parseResponseToMap(String responseBody) {
        return (Map) new GsonBuilder().create().fromJson(responseBody, Object.class);
    }

    AuthenticationPluginConfiguration toPluginConfiguration(Map map) {
        String displayName;
        try {
            displayName = (String) map.get("display-name");
        } catch (Exception e) {
            throw new RuntimeException("Configuration 'display-name' should be of type string");
        }

        String displayImageURL;
        try {
            displayImageURL = (String) map.get("display-image-url");
        } catch (Exception e) {
            throw new RuntimeException("Configuration 'display-image-url' should be of type string");
        }

        Boolean supportsWebBasedAuthentication = false;
        try {
            if (map.get("supports-web-based-authentication") != null) {
                supportsWebBasedAuthentication = (Boolean) map.get("supports-web-based-authentication");
            }
        } catch (Exception e) {
            throw new RuntimeException("Configuration 'supports-web-based-authentication' should be of type boolean");
        }

        Boolean supportsPasswordBasedAuthentication = false;
        try {
            if (map.get("supports-password-based-authentication") != null) {
                supportsPasswordBasedAuthentication = (Boolean) map.get("supports-password-based-authentication");
            }
        } catch (Exception e) {
            throw new RuntimeException("Configuration 'supports-password-based-authentication' should be of type boolean");
        }

        return new AuthenticationPluginConfiguration(displayName, displayImageURL, supportsWebBasedAuthentication, supportsPasswordBasedAuthentication);
    }

    User toUser(Map map) {
        String username;
        try {
            username = (String) map.get("username");
        } catch (Exception e) {
            throw new RuntimeException("User 'username' should be of type string");
        }

        if (StringUtil.isBlank(username)) {
            throw new RuntimeException("User 'username' cannot be empty");
        }

        String displayName;
        try {
            displayName = (String) map.get("display-name");
        } catch (Exception e) {
            throw new RuntimeException("User 'display-name' should be of type string");
        }

        String emailId;
        try {
            emailId = (String) map.get("email-id");
        } catch (Exception e) {
            throw new RuntimeException("User 'email-id' should be of type string");
        }

        return new User(username, displayName, emailId);
    }
}
