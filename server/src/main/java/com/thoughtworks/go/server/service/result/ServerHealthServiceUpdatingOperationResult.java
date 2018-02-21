/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
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
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.service.result;

import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;

/**
 * @understands how to report results of common operations to the user
 * @deprecated Use LocalizedOperationResult interface instead
 */
public class ServerHealthServiceUpdatingOperationResult implements OperationResult {

    private final ServerHealthService serverHealthService;
    boolean canContinue = true;
    private ServerHealthState state;

    private ServerHealthState kickMe(ServerHealthState whatWentWrong, boolean isEverythingOk) {
        canContinue = isEverythingOk;
        serverHealthService.update(whatWentWrong);
        state = whatWentWrong;
        return whatWentWrong;
    }

    public ServerHealthServiceUpdatingOperationResult(ServerHealthService serverHealthService) {
        this.serverHealthService = serverHealthService;
    }

    public ServerHealthState error(String message, String description, HealthStateType type) {
        return kickMe(ServerHealthState.error(message, description, type), false);
    }

    public ServerHealthState warning(String message, String description, HealthStateType type) {
        return kickMe(ServerHealthState.warning(message, description, type), false);
    }

    public ServerHealthState getServerHealthState() {
        return state;
    }

    public boolean canContinue() {
        return canContinue;
    }

    public ServerHealthState success(HealthStateType healthStateType) {
        return kickMe(ServerHealthState.success(healthStateType), true);
    }

    public ServerHealthState paymentRequired(String message, String description, HealthStateType type) {
        throw new RuntimeException("Not yet implemented");
    }

    public ServerHealthState unauthorized(String message, String description, HealthStateType id) {
        throw new RuntimeException("Not yet implemented");
    }

    public void conflict(String message, String description, HealthStateType healthStateType) {
        throw new RuntimeException("Not yet implemented");
    }

    public void notFound(String message, String description, HealthStateType healthStateType) {
        throw new RuntimeException("Not yet implemented");
    }

    public void accepted(String message, String description, HealthStateType healthStateType) {
        throw new RuntimeException("Not yet implemented");
    }

    public void ok(String message) {
        throw new RuntimeException("Not yet implemented");
    }

    public void notAcceptable(String message, final HealthStateType type) {
        throw new RuntimeException("Not yet implemented");
    }

    @Override
    public void internalServerError(String message, HealthStateType type) {
        throw new RuntimeException("Not yet implemented");
    }

    public void notAcceptable(String message, String description, final HealthStateType type) {
        throw new RuntimeException("Not yet implemented");
    }

    @Override
    public void unprocessibleEntity(String message, String description, HealthStateType healthStateType) {
        error(message, description, healthStateType);
    }

    public void badRequest(String message, String description, HealthStateType healthStateType) {
        error(message, description, healthStateType);
    }
}
