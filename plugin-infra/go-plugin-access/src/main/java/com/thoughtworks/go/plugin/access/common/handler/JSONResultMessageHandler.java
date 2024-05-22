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
package com.thoughtworks.go.plugin.access.common.handler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.thoughtworks.go.plugin.api.config.Configuration;
import com.thoughtworks.go.plugin.api.config.Property;
import com.thoughtworks.go.plugin.api.response.Result;
import com.thoughtworks.go.plugin.api.response.validation.ValidationError;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isEmpty;


public class JSONResultMessageHandler {

    private static final Gson GSON = new GsonBuilder().create();

    public Map<String, Object> configurationToMap(Configuration configuration) {
        Map<String, Object> configuredValuesForRepo = new LinkedHashMap<>();
        for (Property property : configuration.list()) {
            Map<String, String> map = new LinkedHashMap<>();
            map.put("value", property.getValue());
            configuredValuesForRepo.put(property.getKey(), map);
        }
        return configuredValuesForRepo;
    }

    public ValidationResult toValidationResult(String responseBody) {
        try {
            ValidationResult validationResult = new ValidationResult();

            if (isEmpty(responseBody)) return validationResult;

            List<Map<String, Object>> errors;
            try {
                errors = GSON.fromJson(responseBody, new TypeToken<List<Map<String, Object>>>() {}.getType());
            } catch (Exception e) {
                throw new RuntimeException("Validation errors should be returned as list of errors, with each error represented as a map");
            }

            for (Map<String, Object> error : errors) {
                String key;
                try {
                    key = (String) error.get("key");
                } catch (Exception e) {
                    throw new RuntimeException("Validation error key should be of type string");
                }

                String message;
                try {
                    message = (String) error.get("message");
                } catch (Exception e) {
                    throw new RuntimeException("Validation message should be of type string");
                }

                if (isEmpty(key)) {
                    validationResult.addError(new ValidationError(message));
                } else {
                    validationResult.addError(new ValidationError(key, message));
                }
            }

            return validationResult;
        } catch (Exception e) {
            throw new RuntimeException(format("Unable to de-serialize json response. %s", e.getMessage()));
        }
    }

    public Result toResult(String responseBody) {
        try {
            Result result = new Result();

            Map<String, Object> map;
            try {
                map = GSON.fromJson(responseBody, new TypeToken<Map<String, Object>>() {}.getType());
            } catch (Exception e) {
                throw new RuntimeException("Check connection result should be returned as map, with status represented as string and messages represented as list");
            }
            if (map == null || map.isEmpty()) {
                throw new RuntimeException("Empty response body");
            }

            String status;
            try {
                status = (String) map.get("status");
            } catch (Exception e) {
                throw new RuntimeException("Check connection 'status' should be of type string");
            }

            if (isEmpty(status)) {
                throw new RuntimeException("Check connection 'status' is a required field");
            }

            if ("success".equalsIgnoreCase(status)) {
                result.withSuccessMessages(new ArrayList<>());
            } else {
                result.withErrorMessages(new ArrayList<>());
            }

            if (map.containsKey("messages") && map.get("messages") != null) {
                List<?> messages;
                try {
                    messages = (List<?>) map.get("messages");
                } catch (Exception e) {
                    throw new RuntimeException("Check connection 'messages' should be of type list of string");
                }

                if (!messages.isEmpty()) {
                    for (Object message : messages) {
                        if (!(message instanceof String)) {
                            throw new RuntimeException("Check connection 'message' should be of type string");
                        }
                    }

                    if (result.isSuccessful()) {
                        //noinspection unchecked
                        result.withSuccessMessages((List<String>) messages);
                    } else {
                        //noinspection unchecked
                        result.withErrorMessages((List<String>) messages);
                    }
                }
            }

            return result;
        } catch (Exception e) {
            throw new RuntimeException(format("Unable to de-serialize json response. %s", e.getMessage()));
        }
    }
}
