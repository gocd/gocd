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
package com.thoughtworks.go.api.util;

import com.google.gson.JsonObject;
import com.thoughtworks.go.config.CaseInsensitiveString;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import static java.lang.String.format;

public abstract class HaltApiMessages {

    public static String notFoundMessage() {
        return "Either the resource you requested was not found, or you are not authorized to perform this action.";
    }

    public static String forbiddenMessage() {
        return "You are not authorized to perform this action.";
    }

    public static String entityAlreadyExistsMessage(String entityType, Object existingName) {
        return format("Failed to add %s '%s'. Another %s with the same name already exists.", entityType, existingName, entityType);
    }

    public static String renameOfEntityIsNotSupportedMessage(String entityType) {
        return format("Renaming of %s is not supported by this API.", entityType);
    }

    public static String etagDoesNotMatch(String entityType, CaseInsensitiveString name) {
        return etagDoesNotMatch(entityType, name.toString());
    }

    public static String etagDoesNotMatch(String entityType, String name) {
        return format("Someone has modified the configuration for %s '%s'. Please update your copy of the config with the changes and try again.", entityType, name);
    }

    public static String rateLimitExceeded() {
        return "Rate Limit Exceeded.";
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

    public static String propertyIsNotAJsonString(String property, JsonObject jsonObject) {
        return format("Could not read property '%s' as a String in json `%s` ", property, jsonObject);
    }

    public static String propertyIsNotAJsonObject(String property, JsonObject jsonObject) {
        return format("Could not read property '%s' as a JsonObject in json `%s`", property, jsonObject);
    }

    public static String propertyIsNotAJsonStringArray(String property, JsonObject jsonObject) {
        return format("Could not read property '%s' as a JsonArray containing string in `%s`", property, jsonObject);
    }

    public static String propertyIsNotAJsonArray(String property, JsonObject jsonObject) {
        return format("Could not read property '%s' as a JsonArray in json `%s`", property, jsonObject);
    }

    public static String propertyIsNotAJsonBoolean(String property, JsonObject jsonObject) {
        return format("Could not read property '%s' as a Boolean in json `%s` ", property, jsonObject);
    }

    public static String propertyIsNotAJsonInt(String property, JsonObject jsonObject) {
        return format("Could not read property '%s' as an Integer in json `%s` ", property, jsonObject);
    }

    public static String missingJsonProperty(String property, JsonObject jsonObject) {
        return format("Json `%s` does not contain property '%s'", jsonObject, property);
    }

    public static String missingRequestParameter(String paramName) {
        return format("Request is missing parameter `%s`", paramName);
    }

    public static String haltBecauseSecurityIsNotEnabled() {
        return "Security must be enabled for this feature to work!";
    }

    public static String queryParamIsUnknownMessage(String paramName, String value, String... goodValues) {
        String message = "Value `" + value + "` is not allowed for query parameter named `" + paramName + "`.";
        if (!ArrayUtils.isEmpty(goodValues)) {
            message += " Valid values are " + StringUtils.join(goodValues, ", ") + ".";
        }
        return message;
    }
}
