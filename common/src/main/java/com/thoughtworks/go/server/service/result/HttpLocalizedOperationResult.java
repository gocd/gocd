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
package com.thoughtworks.go.server.service.result;

import com.thoughtworks.go.serverhealth.HealthStateType;
import org.apache.http.HttpStatus;

import static org.apache.http.HttpStatus.SC_FORBIDDEN;

/**
 * @understands localized operation result for http
 * We suck at this
 */
public class HttpLocalizedOperationResult implements LocalizedOperationResult {
    private String message;
    private HealthStateType healthStateType;
    private int httpCode = 200;

    @Override
    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public boolean hasMessage() {
        return message != null;
    }

    @Override
    public void notImplemented(String message) {
        this.message = message;
        httpCode = HttpStatus.SC_NOT_IMPLEMENTED;
    }

    @Override
    public void unprocessableEntity(String message) {
        this.message = message;
        this.httpCode = HttpStatus.SC_UNPROCESSABLE_ENTITY;
    }

    @Override
    public void forbidden(String message, HealthStateType healthStateType) {
        this.message = message;
        this.healthStateType = healthStateType;
        httpCode = SC_FORBIDDEN;
    }

    @Override
    public void stale(String message) {
        this.message = message;
        httpCode = HttpStatus.SC_PRECONDITION_FAILED;
    }

    @Override
    public void notFound(String message, HealthStateType healthStateType) {
        this.message = message;
        this.healthStateType = healthStateType;
        httpCode = HttpStatus.SC_NOT_FOUND;
    }

    @Override
    public void conflict(String message) {
        this.message = message;
        httpCode = HttpStatus.SC_CONFLICT;
    }

    @Override
    public void internalServerError(String message) {
        this.message = message;
        httpCode = HttpStatus.SC_INTERNAL_SERVER_ERROR;
    }

    @Override
    public void badRequest(String message) {
        this.message = message;
        httpCode = HttpStatus.SC_BAD_REQUEST;
    }

    @Override
    public void accepted(String message) {
        this.message = message;
        httpCode = HttpStatus.SC_ACCEPTED;
    }

    @Override
    public void notAcceptable(String message) {
        this.message = message;
        httpCode = HttpStatus.SC_NOT_ACCEPTABLE;
    }

    @Override
    public boolean isSuccessful() {
        return 200 <= httpCode && httpCode < 300; // I hate java
    }

    @Override
    public void connectionError(String message) {
        this.message = message;
        httpCode = HttpStatus.SC_BAD_REQUEST;
    }

    public int httpCode() {
        return httpCode;
    }

    public String message() {
        return message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        HttpLocalizedOperationResult that = (HttpLocalizedOperationResult) o;

        if (httpCode != that.httpCode) return false;
        if (healthStateType != null ? !healthStateType.equals(that.healthStateType) : that.healthStateType != null)
            return false;
        if (message != null ? !message.equals(that.message) : that.message != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = message != null ? message.hashCode() : 0;
        result = 31 * result + (healthStateType != null ? healthStateType.hashCode() : 0);
        result = 31 * result + httpCode;
        return result;
    }

    @Override
    public String toString() {
        return "HttpLocalizedOperationResult{" +
                "message=" + message +
                ", healthStateType=" + healthStateType +
                ", httpCode=" + httpCode +
                '}';
    }
}
