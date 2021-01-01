/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.plugin.access.secrets.v1;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.thoughtworks.go.plugin.domain.secrets.Secret;

import java.util.List;

class SecretDTO {
    private static final Gson GSON = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

    @Expose
    @SerializedName("key")
    private final String key;
    @Expose
    @SerializedName("value")
    private final String value;

    public SecretDTO(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public static SecretDTO fromJSON(String json) {
        return GSON.fromJson(json, SecretDTO.class);
    }

    public static List<SecretDTO> fromJSONList(String json) {
        return GSON.fromJson(json, new TypeToken<List<SecretDTO>>() {
        }.getType());
    }

    public Secret toDomainModel() {
        return new Secret(this.key, this.value);
    }
}
