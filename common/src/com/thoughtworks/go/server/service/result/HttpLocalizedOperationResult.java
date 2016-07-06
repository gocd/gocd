/*
 * Copyright 2016 ThoughtWorks, Inc.
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

import com.thoughtworks.go.i18n.Localizable;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.i18n.Localizer;
import com.thoughtworks.go.serverhealth.HealthStateType;
import org.apache.http.HttpStatus;

/**
 * @understands localized operation result for http
 * We suck at this
 */
public class HttpLocalizedOperationResult implements LocalizedOperationResult {
    private Localizable message;
    private HealthStateType healthStateType;
    private int httpCode = 200;

    public static LocalizedOperationResult badRequest(String messageKey){
        LocalizedOperationResult result = successfulResult();
        result.badRequest(LocalizedMessage.string(messageKey));
        return result;
    }

    public static LocalizedOperationResult successfulResult(){
        return new HttpLocalizedOperationResult();
    }

    public void setMessage(Localizable message) {
        this.message = message;
    }

    @Override
    public boolean hasMessage() {
        return message != null;
    }

    public void notImplemented(Localizable message) {
        this.message = message;
        httpCode = HttpStatus.SC_NOT_IMPLEMENTED;
    }

    @Override
    public void unprocessableEntity(Localizable message) {
        this.message = message;
        this.httpCode = HttpStatus.SC_UNPROCESSABLE_ENTITY;
    }

    public void unauthorized(Localizable message, HealthStateType healthStateType) {
        this.message = message;
        this.healthStateType = healthStateType;
        httpCode = HttpStatus.SC_UNAUTHORIZED;
    }

    @Override
    public void stale(Localizable message) {
        this.message = message;
        httpCode = HttpStatus.SC_PRECONDITION_FAILED;
    }

    public void notFound(Localizable message, HealthStateType healthStateType) {
        this.message = message;
        this.healthStateType = healthStateType;
        httpCode = HttpStatus.SC_NOT_FOUND;
    }

    public void conflict(Localizable message) {
        this.message = message;
        httpCode = HttpStatus.SC_CONFLICT;
    }

    public void internalServerError(Localizable message) {
        this.message = message;
        httpCode = HttpStatus.SC_INTERNAL_SERVER_ERROR;
    }

    public void badRequest(Localizable message) {
        this.message = message;
        httpCode = HttpStatus.SC_BAD_REQUEST;
    }

    public void accepted(Localizable message) {
        this.message = message;
        httpCode = HttpStatus.SC_ACCEPTED;
    }

    public void notAcceptable(Localizable message) {
        this.message = message;
        httpCode = HttpStatus.SC_NOT_ACCEPTABLE;
    }

    public boolean isSuccessful() {
        return 200 <= httpCode && httpCode < 300; // I hate java
    }

    public void connectionError(Localizable message) {
        this.message = message;
        httpCode = HttpStatus.SC_BAD_REQUEST;
    }

    public int httpCode() {
        return httpCode;
    }

    public String message(Localizer localizer) {
        if (message == null) return null;
        return message.localize(localizer);
    }

    /**
     * Used only in tests
     */
    public Localizable localizable() {
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
