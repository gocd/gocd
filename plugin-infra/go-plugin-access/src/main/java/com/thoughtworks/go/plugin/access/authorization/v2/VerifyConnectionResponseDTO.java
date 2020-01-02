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
package com.thoughtworks.go.plugin.access.authorization.v2;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.thoughtworks.go.plugin.access.common.handler.JSONResultMessageHandler;
import com.thoughtworks.go.plugin.api.response.validation.ValidationError;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.domain.common.VerifyConnectionResponse;

class VerifyConnectionResponseDTO {
    private static final Gson GSON = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

    @Expose
    @SerializedName("status")
    private final Status status;
    @Expose
    @SerializedName("message")
    private final String message;

    private ValidationResult validationResult;

    public VerifyConnectionResponseDTO(Status status, String message) {
        this.status = status;
        this.message = message;
    }

    public static VerifyConnectionResponseDTO fromJSON(String json) {
        VerifyConnectionResponseDTO response = GSON.fromJson(json, VerifyConnectionResponseDTO.class);
        response.validationResult = validationResult(json);

        return response;
    }

    public VerifyConnectionResponse response() {
        return new VerifyConnectionResponse(status.getStatus(), message, result());
    }

    private com.thoughtworks.go.plugin.domain.common.ValidationResult result() {
        if(validationResult == null) {
            return null;
        }

        com.thoughtworks.go.plugin.domain.common.ValidationResult result = new com.thoughtworks.go.plugin.domain.common.ValidationResult();

        for (ValidationError error : validationResult.getErrors()) {
            result.addError(new com.thoughtworks.go.plugin.domain.common.ValidationError(error.getKey(), error.getMessage()));
        }
        return result;
    }

    private static ValidationResult validationResult(String json) {
        JsonObject jsonObject = GSON.fromJson(json, JsonObject.class);

        JsonElement errors = jsonObject.get("errors");

        return errors != null ? new JSONResultMessageHandler().toValidationResult(errors.toString()) : null;
    }

    private enum Status {
        @SerializedName("success")
        success("success"),
        @SerializedName("failure")
        failure("failure"),
        @SerializedName("validation-failed")
        ValidationFailed("validation-failed");

        private String status;

        Status(String status) {
            this.status = status;
        }

        public String getStatus() {
            return status;
        }
    }
}
