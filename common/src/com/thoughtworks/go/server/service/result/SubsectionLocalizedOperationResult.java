/*
 * Copyright 2015 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thoughtworks.go.server.service.result;

import com.thoughtworks.go.i18n.Localizable;
import com.thoughtworks.go.i18n.Localizer;
import com.thoughtworks.go.serverhealth.HealthStateType;

public class SubsectionLocalizedOperationResult implements LocalizedOperationResult {
    private Localizable message;

    public void unauthorized(Localizable message, HealthStateType id) {
        throw new UnsupportedOperationException("not supported");
    }

    @Override
    public void stale(Localizable message) {
        throw new UnsupportedOperationException("not supported");
    }

    public void notFound(Localizable message, HealthStateType healthStateType) {
        throw new UnsupportedOperationException("not supported");
    }

    public boolean isSuccessful() {
        return message == null;
    }

    public void connectionError(Localizable message) {
        this.message = message;
    }

    public void conflict(Localizable message) {
        throw new UnsupportedOperationException("not supported");
    }

    public void badRequest(Localizable message) {
        throw new UnsupportedOperationException("not supported");
    }

    public void notAcceptable(Localizable localizable) {
        throw new UnsupportedOperationException("not supported");
    }

    public void internalServerError(Localizable message) {
        throw new UnsupportedOperationException("not supported");
    }

    public void setMessage(Localizable message) {
        this.message=message;
    }

    @Override
    public boolean hasMessage() {
        return this.message == null;
    }

    @Override
    public void notImplemented(Localizable localizable) {
        throw new UnsupportedOperationException("not supported");
    }

    @Override
    public void unprocessableEntity(Localizable localizable) {
        throw new UnsupportedOperationException("not supported");
    }

    public String replacementContent(Localizer localizer) {
        return message.localize(localizer);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SubsectionLocalizedOperationResult)) {
            return false;
        }

        SubsectionLocalizedOperationResult result = (SubsectionLocalizedOperationResult) o;

        if (message != null ? !message.equals(result.message) : result.message != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return message != null ? message.hashCode() : 0;
    }
}
