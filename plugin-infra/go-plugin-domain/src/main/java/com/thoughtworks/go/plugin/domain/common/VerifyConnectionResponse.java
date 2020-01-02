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
package com.thoughtworks.go.plugin.domain.common;

public class VerifyConnectionResponse {
    private final String status;
    private final String message;
    private final ValidationResult validationResult;

    public VerifyConnectionResponse(String status, String message, ValidationResult validationResult) {
        this.status = status;
        this.message = message;
        this.validationResult = validationResult;
    }

    public String getMessage() {
        return message;
    }

    public ValidationResult getValidationResult() {
        return validationResult;
    }

    public String getStatus() {

        return status;
    }

    public boolean isSuccessful() {
        return status.equals("success");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        VerifyConnectionResponse that = (VerifyConnectionResponse) o;

        if (status != null ? !status.equals(that.status) : that.status != null) return false;
        if (message != null ? !message.equals(that.message) : that.message != null) return false;
        return validationResult != null ? validationResult.equals(that.validationResult) : that.validationResult == null;

    }

    @Override
    public int hashCode() {
        int result = status != null ? status.hashCode() : 0;
        result = 31 * result + (message != null ? message.hashCode() : 0);
        result = 31 * result + (validationResult != null ? validationResult.hashCode() : 0);
        return result;
    }
}
