/*
 * Copyright 2024 Thoughtworks, Inc.
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
package com.thoughtworks.go.plugin.access.authorization.v2;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.thoughtworks.go.plugin.domain.authorization.AuthorizationServerUrlResponse;

import java.util.Map;

import static java.util.Objects.nonNull;

class AuthorizationServerUrlResponseDTO {

    private static final Gson GSON = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

    @Expose
    @SerializedName("authorization_server_url")
    private final String authorizationServerUrl;

    @Expose
    @SerializedName("auth_session")
    private final Map<String, String> authSession;

    public AuthorizationServerUrlResponseDTO(String authorizationServerUrl, Map<String, String> authSession) {
        this.authorizationServerUrl = authorizationServerUrl;
        this.authSession = authSession;
    }

    public String getAuthorizationServerUrl() {
        return authorizationServerUrl;
    }

    public Map<String, String> getAuthSession() {
        return authSession;
    }

    public static AuthorizationServerUrlResponseDTO fromJSON(String json) {
        return GSON.fromJson(json, AuthorizationServerUrlResponseDTO.class);
    }

    public AuthorizationServerUrlResponse toDomainModel() {
        return new AuthorizationServerUrlResponse(this.authorizationServerUrl, nonNull(this.authSession) ? this.authSession : Map.of());
    }
}
