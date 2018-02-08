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

import org.springframework.http.HttpStatus;
import spark.HaltException;

import static com.thoughtworks.go.api.util.HaltApiMessages.*;
import static spark.Spark.halt;

public abstract class HaltApiResponses {

    public static HaltException haltBecauseUnauthorized() {
        return halt(HttpStatus.UNAUTHORIZED.value(), MessageJson.create(unauthorizedMessage()));
    }

    public static HaltException haltBecauseEntityAlreadyExists(Object jsonInRequestBody, String entityType, Object existingName) {
        return halt(HttpStatus.UNPROCESSABLE_ENTITY.value(), MessageJson.create(entityAlreadyExistsMessage(entityType, existingName), jsonInRequestBody));
    }

    public static HaltException haltBecauseRenameOfEntityIsNotSupported(String entityType) {
        return halt(HttpStatus.UNPROCESSABLE_ENTITY.value(), MessageJson.create(renameOfEntityIsNotSupportedMessage(entityType)));
    }

    public static HaltException haltBecauseEtagDoesNotMatch(String entityType, Object name) {
        return halt(HttpStatus.PRECONDITION_FAILED.value(), MessageJson.create(etagDoesNotMatch(entityType, name)));
    }

    public static HaltException haltBecauseRateLimitExceeded() {
        return halt(HttpStatus.TOO_MANY_REQUESTS.value(), MessageJson.create(rateLimitExceeded()));
    }

    public static HaltException haltBecauseJsonContentTypeExpected() {
        return halt(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(), MessageJson.create(jsonContentTypeExpected()));
    }

    public static HaltException haltBecauseConfirmHeaderMissing() {
        return halt(HttpStatus.BAD_REQUEST.value(), MessageJson.create(confirmHeaderMissing()));
    }

    public static HaltException haltBecauseDeprecatedConfirmHeaderMissing() {
        return halt(HttpStatus.BAD_REQUEST.value(), MessageJson.create(deprecatedConfirmHeaderMissing()));
    }

    public static HaltException haltBecausePropertyIsNotAJsonString(String property) {
        return halt(HttpStatus.UNPROCESSABLE_ENTITY.value(), MessageJson.create(propertyIsNotAJsonString(property)));
    }

    public static HaltException haltBecausePropertyIsNotAJsonArray(String property) {
        return halt(HttpStatus.UNPROCESSABLE_ENTITY.value(), MessageJson.create(propertyIsNotAJsonArray(property)));
    }

    public static HaltException haltBecausePropertyIsNotAJsonBoolean(String property) {
        return halt(HttpStatus.UNPROCESSABLE_ENTITY.value(), MessageJson.create(propertyIsNotAJsonBoolean(property)));
    }

    public static HaltException haltBecausePropertyIsNotAJsonObject(String property) {
        return halt(HttpStatus.UNPROCESSABLE_ENTITY.value(), MessageJson.create(propertyIsNotAJsonObject(property)));
    }

    public static HaltException haltBecausePropertyIsNotAJsonStringArray(String property) {
        return halt(HttpStatus.UNPROCESSABLE_ENTITY.value(), MessageJson.create(propertyIsNotAJsonStringArray(property)));
    }

    public static HaltException haltBecauseMissingJsonProperty(String property) {
        return halt(HttpStatus.UNPROCESSABLE_ENTITY.value(), MessageJson.create(missingJsonProperty(property)));
    }
}
