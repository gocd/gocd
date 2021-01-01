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
package com.thoughtworks.go.plugin.access.common.handler;

import com.google.gson.GsonBuilder;
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
    public Map configurationToMap(Configuration configuration) {
        Map configuredValuesForRepo = new LinkedHashMap();
        for (Property property : configuration.list()) {
            Map map = new LinkedHashMap();
            map.put("value", property.getValue());
            configuredValuesForRepo.put(property.getKey(), map);
        }
        return configuredValuesForRepo;
    }

    public ValidationResult toValidationResult(String responseBody) {
        try {
            ValidationResult validationResult = new ValidationResult();

            if (isEmpty(responseBody)) return validationResult;

            List errors;
            try {
                errors = (List<Map>) new GsonBuilder().create().fromJson(responseBody, Object.class);
            } catch (Exception e) {
                throw new RuntimeException("Validation errors should be returned as list or errors, with each error represented as a map");
            }

            for (Object errorObj : errors) {
                if (!(errorObj instanceof Map)) {
                    throw new RuntimeException("Each validation error should be represented as a map");
                }
                Map errorMap = (Map) errorObj;

                String key;
                try {
                    key = (String) errorMap.get("key");
                } catch (Exception e) {
                    throw new RuntimeException("Validation error key should be of type string");
                }

                String message;
                try {
                    message = (String) errorMap.get("message");
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

            Map map;
            try {
                map = (Map) new GsonBuilder().create().fromJson(responseBody, Object.class);
            } catch (Exception e) {
                throw new RuntimeException("Check connection result should be returned as map, with key represented as string and messages represented as list");
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
                List messages = null;
                try {
                    messages = (List) map.get("messages");
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
                        result.withSuccessMessages(messages);
                    } else {
                        result.withErrorMessages(messages);
                    }
                }
            }

            return result;
        } catch (Exception e) {
            throw new RuntimeException(format("Unable to de-serialize json response. %s", e.getMessage()));
        }
    }
}
