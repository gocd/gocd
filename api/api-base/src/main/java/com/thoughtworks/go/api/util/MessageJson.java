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

package com.thoughtworks.go.api.util;

import java.util.LinkedHashMap;

public class MessageJson {

    private final String message;
    private final Object data;

    private MessageJson(String message, Object data) {
        this.message = message;
        this.data = data;
    }

    public static String create(String message, Object data) {
        return new MessageJson(message, data).toString();
    }

    public static String create(String message) {
        return create(message, null);
    }

    public String toString() {
        LinkedHashMap<String, Object> response = new LinkedHashMap<>();
        response.put("message", message);

        if (data != null) {
            response.put("data", data);
        }

        return GsonTransformer.getInstance().render(response);
    }
}
