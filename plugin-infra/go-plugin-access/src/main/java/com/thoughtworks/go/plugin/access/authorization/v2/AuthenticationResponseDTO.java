/*
 * Copyright 2020 ThoughtWorks, Inc.
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
import com.thoughtworks.go.plugin.domain.authorization.AuthenticationResponse;
import org.apache.commons.collections4.ListUtils;

import java.util.Collections;
import java.util.List;

class AuthenticationResponseDTO {
    private static final Gson GSON = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

    @Expose
    @SerializedName("user")
    private final UserDTO user;
    @Expose
    @SerializedName("roles")
    private final List<String> roles;

    public AuthenticationResponseDTO(UserDTO user, List<String> roles) {
        this.user = user;
        this.roles = roles;
    }

    public UserDTO getUser() {
        return user;
    }

    public List<String> getRoles() {
        return roles;
    }

    public static AuthenticationResponseDTO fromJSON(String json) {
        return GSON.fromJson(json, AuthenticationResponseDTO.class);
    }

    public AuthenticationResponse toDomainModel() {
        return new AuthenticationResponse(this.user != null ? this.user.toDomainModel() : null, ListUtils.defaultIfNull(this.roles, Collections.emptyList()));
    }
}
