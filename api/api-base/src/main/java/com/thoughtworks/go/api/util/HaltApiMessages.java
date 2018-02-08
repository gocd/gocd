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

import static java.lang.String.format;

public abstract class HaltApiMessages {

    public static String notFoundMessage() {
        return "Either the resource you requested was not found, or you are not authorized to perform this action.";
    }

    public static String unauthorizedMessage() {
        return "You are not authorized to perform this action.";
    }

    public static String entityAlreadyExistsMessage(String entityType, Object existingName) {
        return String.format("Failed to add %s `%s'. Another %s with the same name already exists.", entityType, existingName, entityType);
    }

    public static String renameOfEntityIsNotSupportedMessage(String entityType) {
        return String.format("Renaming of %s is not supported by this API.", entityType);
    }

    public static String etagDoesNotMatch(String entityType, Object name) {
        return String.format("Someone has modified the configuration for %s `%s'. Please update your copy of the config with the changes and try again.", entityType, name);
    }

    public static String rateLimitExceeded() {
        return "Rate Limit Exceeded";
    }

    public static String errorWhileEncryptingMessage() {
        return "An error occurred while encrypting the value. Please check the logs for more details.";
    }

    public static String jsonContentTypeExpected() {
        return "You must specify a 'Content-Type' of 'application/json'";
    }

    public static String deprecatedConfirmHeaderMissing() {
        return "Missing required header 'Confirm' with value 'true'";
    }

    public static String confirmHeaderMissing() {
        return "Missing required header 'X-GoCD-Confirm' with value 'true'";
    }

    public static String propertyIsNotAJsonString(String property) {
        return String.format("Could not read property '%s' as a String", property);
    }

    public static String propertyIsNotAJsonObject(String property) {
        return String.format("Could not read property '%s' as a JsonObject", property);
    }

    public static String propertyIsNotAJsonStringArray(String property) {
        return String.format("Could not read property '%s' as a JsonArray containing string", property);
    }

    public static String propertyIsNotAJsonArray(String property) {
        return String.format("Could not read property '%s' as a JsonArray", property);
    }

    public static String propertyIsNotAJsonBoolean(String property) {
        return String.format("Could not read property '%s' as a Boolean", property);
    }

    public static String missingJsonProperty(String property) {
        return format("Json does not contain property '%s'", property);
    }
}
