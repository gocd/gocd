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
package com.thoughtworks.go.plugin.access.common.models;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.Map;

import static org.apache.commons.codec.digest.DigestUtils.sha256Hex;

public class ImageDeserializer {

    public com.thoughtworks.go.plugin.domain.common.Image fromJSON(String jsonString) {
        Map<String, String> json = new Gson().fromJson(jsonString, new TypeToken<Map<String, String>>() {
        }.getType());
        String contentType = json.get("content_type");
        String data = json.get("data");
        return new com.thoughtworks.go.plugin.domain.common.Image(contentType, data, hash(contentType, data));
    }

    private String hash(String contentType, String data) {
        return sha256Hex("data:" + contentType + ";base64," + data);
    }
}
