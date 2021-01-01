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
package com.thoughtworks.go.api.util;

import com.google.gson.JsonObject;
import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.config.CaseInsensitiveString;
import org.springframework.http.HttpStatus;
import spark.HaltException;

import java.util.function.Consumer;

import static com.thoughtworks.go.api.util.HaltApiMessages.*;
import static java.lang.String.format;
import static spark.Spark.halt;

public abstract class HaltApiResponses {

    public static HaltException haltBecauseQueryParamIsUnknown(String paramName, String value, String... goodValues) {
        return halt(HttpStatus.BAD_REQUEST.value(), MessageJson.create(queryParamIsUnknownMessage(paramName, value, goodValues)));
    }

    public static HaltException haltBecauseForbidden() {
        return halt(HttpStatus.FORBIDDEN.value(), MessageJson.create(forbiddenMessage()));
    }

    public static HaltException haltBecauseForbidden(String message) {
        return halt(HttpStatus.FORBIDDEN.value(), MessageJson.create(message));
    }

    public static HaltException haltBecauseEntityAlreadyExists(Consumer<OutputWriter> jsonInRequestBody, String entityType, Object existingName) {
        return halt(HttpStatus.UNPROCESSABLE_ENTITY.value(), MessageJson.create(entityAlreadyExistsMessage(entityType, existingName), jsonInRequestBody));
    }

    public static HaltException haltBecauseRenameOfEntityIsNotSupported(String entityType) {
        return halt(HttpStatus.UNPROCESSABLE_ENTITY.value(), MessageJson.create(renameOfEntityIsNotSupportedMessage(entityType)));
    }

    public static HaltException haltBecauseEtagDoesNotMatch(String entityType, CaseInsensitiveString name) {
        return halt(HttpStatus.PRECONDITION_FAILED.value(), MessageJson.create(etagDoesNotMatch(entityType, name)));
    }

    public static HaltException haltBecauseEtagDoesNotMatch(String entityType, String name) {
        return halt(HttpStatus.PRECONDITION_FAILED.value(), MessageJson.create(etagDoesNotMatch(entityType, name)));
    }

    public static HaltException haltBecauseEtagDoesNotMatch(String message, Object... tokens) {
        return halt(HttpStatus.PRECONDITION_FAILED.value(), MessageJson.create(format(message, tokens)));
    }

    public static HaltException haltBecauseEtagDoesNotMatch() {
        return halt(HttpStatus.PRECONDITION_FAILED.value(), MessageJson.create("Someone has modified the entity. Please update your copy with the changes and try again."));
    }

    public static HaltException haltBecauseRateLimitExceeded() {
        return halt(HttpStatus.TOO_MANY_REQUESTS.value(), MessageJson.create(rateLimitExceeded()));
    }

    public static HaltException haltBecauseJsonContentTypeExpected() {
        return halt(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(), MessageJson.create(jsonContentTypeExpected()));
    }

    public static HaltException haltBecauseRequiredParamMissing(String paramName) {
        return halt(HttpStatus.BAD_REQUEST.value(), MessageJson.create(missingRequestParameter(paramName)));
    }

    public static HaltException haltBecauseConfirmHeaderMissing() {
        return halt(HttpStatus.BAD_REQUEST.value(), MessageJson.create(confirmHeaderMissing()));
    }

    public static HaltException haltBecauseDeprecatedConfirmHeaderMissing() {
        return halt(HttpStatus.BAD_REQUEST.value(), MessageJson.create(deprecatedConfirmHeaderMissing()));
    }

    public static HaltException haltBecausePropertyIsNotAJsonString(String property, JsonObject jsonObject) {
        return halt(HttpStatus.UNPROCESSABLE_ENTITY.value(), MessageJson.create(propertyIsNotAJsonString(property, jsonObject)));
    }

    public static HaltException haltBecausePropertyIsNotAJsonArray(String property, JsonObject jsonObject) {
        return halt(HttpStatus.UNPROCESSABLE_ENTITY.value(), MessageJson.create(propertyIsNotAJsonArray(property, jsonObject)));
    }

    public static HaltException haltBecausePropertyIsNotAJsonBoolean(String property, JsonObject jsonObject) {
        return halt(HttpStatus.UNPROCESSABLE_ENTITY.value(), MessageJson.create(propertyIsNotAJsonBoolean(property, jsonObject)));
    }

    public static HaltException haltBecausePropertyIsNotAJsonInt(String property, JsonObject jsonObject) {
        return halt(HttpStatus.UNPROCESSABLE_ENTITY.value(), MessageJson.create(propertyIsNotAJsonInt(property, jsonObject)));
    }

    public static HaltException haltBecausePropertyIsNotAJsonObject(String property, JsonObject jsonObject) {
        return halt(HttpStatus.UNPROCESSABLE_ENTITY.value(), MessageJson.create(propertyIsNotAJsonObject(property, jsonObject)));
    }

    public static HaltException haltBecausePropertyIsNotAJsonStringArray(String property, JsonObject jsonObject) {
        return halt(HttpStatus.UNPROCESSABLE_ENTITY.value(), MessageJson.create(propertyIsNotAJsonStringArray(property, jsonObject)));
    }

    public static HaltException haltBecauseMissingJsonProperty(String property, JsonObject jsonObject) {
        return halt(HttpStatus.UNPROCESSABLE_ENTITY.value(), MessageJson.create(missingJsonProperty(property, jsonObject)));
    }

    public static HaltException haltBecauseOfReason(String message, Object... tokens) {
        return halt(HttpStatus.UNPROCESSABLE_ENTITY.value(), MessageJson.create(format(message, tokens)));
    }

    public static HaltException haltBecauseSecurityIsNotEnabled() {
        return halt(HttpStatus.UNPROCESSABLE_ENTITY.value(), MessageJson.create(HaltApiMessages.haltBecauseSecurityIsNotEnabled()));

    }
}
