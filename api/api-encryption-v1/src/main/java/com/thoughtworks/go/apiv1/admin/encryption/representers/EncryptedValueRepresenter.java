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

package com.thoughtworks.go.apiv1.admin.encryption.representers;

import com.thoughtworks.go.api.representers.JsonWriter;
import com.thoughtworks.go.spark.RequestContext;

import java.util.Map;


public interface EncryptedValueRepresenter {
    static Map toJSON(String encryptedValue, RequestContext requestContext) {
        JsonWriter jsonWriter = new JsonWriter(requestContext);
        addLinks(jsonWriter);
        jsonWriter.add("encrypted_value", encryptedValue);
        return jsonWriter.getAsMap();
    }

    static void addLinks(JsonWriter jsonWriter) {
        jsonWriter.addDocLink("https://api.gocd.org/#encryption");
        jsonWriter.addLink("self", "/go/api/admin/encrypt");
    }
}
