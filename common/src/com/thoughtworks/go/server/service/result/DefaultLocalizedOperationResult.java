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
import com.thoughtworks.go.serverhealth.HealthStateType;


public class DefaultLocalizedOperationResult implements LocalizedOperationResult {

    public void unauthorized(Localizable message, HealthStateType id) {
    }

    @Override
    public void stale(Localizable message) {
    }

    public void notFound(Localizable message, HealthStateType healthStateType) {
    }

    public boolean isSuccessful() {
        return true;
    }

    public void connectionError(Localizable message) {
    }

    public void conflict(Localizable message) {
    }

    public void badRequest(Localizable message) {
    }

    public void notAcceptable(Localizable localizable) {
    }

    public void internalServerError(Localizable message) {
    }

    public void setMessage(Localizable message) {
    }

    @Override
    public boolean hasMessage() {
        return false;
    }

    @Override
    public void notImplemented(Localizable localizable) {
    }

    @Override
    public void unprocessableEntity(Localizable localizable) {

    }
}
