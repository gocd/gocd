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

package com.thoughtworks.go.plugin.domain.authorization;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class AuthorizationServerUrlResponse {

    private final String authorizationServerUrl;
    private final Map<String, String> authSession;

    public AuthorizationServerUrlResponse(String authorizationServerUrl, Map<String, String> authSession) {
        this.authorizationServerUrl = authorizationServerUrl;
        this.authSession = authSession;
    }

    public String getAuthorizationServerUrl() {
        return authorizationServerUrl;
    }

    public Map<String, String> getAuthSession() {
        return authSession;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuthorizationServerUrlResponse that = (AuthorizationServerUrlResponse) o;
        return Objects.equals(authorizationServerUrl, that.authorizationServerUrl) && Objects.equals(authSession, that.authSession);
    }

    @Override
    public int hashCode() {
        return Objects.hash(authorizationServerUrl, authSession);
    }

    @Override
    public String toString() {
        return "AuthorizationServerUrlResponse{" +
            "authorizationServerUrl='" + authorizationServerUrl + '\'' +
            ", authSessionKeys=" + Optional.ofNullable(authSession).map(Map::keySet).orElse(null) +
            '}';
    }
}
