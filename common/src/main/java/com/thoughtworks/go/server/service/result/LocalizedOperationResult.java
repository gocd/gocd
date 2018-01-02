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

/**
 * @understands the current status of a given task.
 */
public interface LocalizedOperationResult {
    void unauthorized(Localizable message, HealthStateType id);

    void stale(Localizable message);

    void notFound(Localizable message, HealthStateType healthStateType);

    boolean isSuccessful();

    void connectionError(Localizable message);

    void conflict(Localizable message);

    void badRequest(Localizable message);

    void notAcceptable(Localizable localizable);

    void internalServerError(Localizable message);

    void setMessage(Localizable message);

    boolean hasMessage();

    void notImplemented(Localizable localizable);

    void unprocessableEntity(Localizable localizable);
}
