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
import com.thoughtworks.go.serverhealth.ServerHealthState;

/**
 * @understands the current status of the Server Health.
 * @deprecated Use LocalizedOperationResult interface instead
 */
public class ServerHealthStateOperationResult implements OperationResult {
    private ServerHealthState lastHealthState = null;

    @Override
    public ServerHealthState success(HealthStateType healthStateType) {
        return add(ServerHealthState.success(healthStateType));
    }

    @Override
    public ServerHealthState error(String message, String description, HealthStateType type) {
        return add(ServerHealthState.error(message, description, type));
    }

    @Override
    public ServerHealthState warning(String message, String description, HealthStateType type) {
        return add(ServerHealthState.warning(message, description, type));
    }

    @Override
    public ServerHealthState getServerHealthState() {
        return lastHealthState;
    }

    @Override
    public boolean canContinue() {
        return lastHealthState == null ||  lastHealthState.isSuccess();
    }

    @Override
    public ServerHealthState forbidden(String message, String description, HealthStateType id) {
        return error(message, description, id);
    }

    @Override
    public void conflict(String message, String description, HealthStateType healthStateType) {
        error(message, description, healthStateType);
    }

    @Override
    public void notFound(String message, String description, HealthStateType healthStateType) {
        error(message, description, healthStateType);
    }

    @Override
    public void accepted(String message, String description, HealthStateType healthStateType) {
        success(healthStateType);
    }

    @Override
    public void ok(String message) {

    }

    @Override
    public void notAcceptable(String message, final HealthStateType type) {
        error(message, message, type);
    }

    @Override
    public void internalServerError(String message, HealthStateType type) {
        error(message, "", type);
    }

    @Override
    public void insufficientStorage(String message, String description, HealthStateType type) {
        error(message, description, type);
    }

    @Override
    public void notAcceptable(String message, String description, final HealthStateType type) {
        error(message, description, type);
    }

    @Override
    public void unprocessibleEntity(String message, String description, HealthStateType healthStateType) {
        error(message, description, healthStateType);
    }

    @Override
    public void badRequest(String message, String description, HealthStateType healthStateType) {
        error(message, description, healthStateType);
    }

    private ServerHealthState add(ServerHealthState newState) {
        if (lastHealthState == null) {
            lastHealthState = newState;
        }
        else {
            lastHealthState = lastHealthState.trump(newState);
        }
        return lastHealthState;
    }
}
