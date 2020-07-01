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
import org.apache.commons.lang3.StringUtils;

import static org.apache.http.HttpStatus.SC_FORBIDDEN;

/**
 * @understands how to turn the problems that can occur during api calls into human-readable form and http codes
 * @deprecated Use LocalizedOperationResult interface instead
 */
public class HttpOperationResult implements OperationResult {

    private static final String BLANK_STRING = "";
    int httpCode = 200;
    private String message;
    final private ServerHealthStateOperationResult serverHealthStateOperationResult = new ServerHealthStateOperationResult();


    @Override
    public ServerHealthState success(HealthStateType healthStateType) {
        return serverHealthStateOperationResult.success(healthStateType);
    }

    @Override
    public ServerHealthState error(String message, String description, HealthStateType type) {
        httpCode = 400;
        this.message = message;
        return serverHealthStateOperationResult.error(message, description, type);
    }

    @Override
    public void insufficientStorage(String message, String description, HealthStateType type) {
        httpCode = 507;
        this.message = message;
        serverHealthStateOperationResult.error(message, description, type);
    }

    @Override
    public void badRequest(String message, String description, HealthStateType healthStateType) {
        error(message, description, healthStateType);
        httpCode = 400;
    }

    @Override
    public void unprocessibleEntity(String message, String description, HealthStateType healthStateType) {
        error(message, description, healthStateType);
        httpCode = 422;
    }

    public boolean isSuccess() {
        return httpCode >= 200 && httpCode <= 299;
    }

    @Override
    public ServerHealthState warning(String message, String description, HealthStateType type) {
        this.message = message;
        return serverHealthStateOperationResult.warning(message, description, type);
    }

    @Override
    public ServerHealthState getServerHealthState() {
        return serverHealthStateOperationResult.getServerHealthState();
    }

    @Override
    public boolean canContinue() {
        return serverHealthStateOperationResult.canContinue();
    }

    @Override
    public ServerHealthState forbidden(String message, String description, HealthStateType id) {
        this.message = message;
        httpCode = SC_FORBIDDEN;
        return serverHealthStateOperationResult.forbidden(message, description, id);
    }

    @Override
    public void conflict(String message, String description, HealthStateType healthStateType) {
        serverHealthStateOperationResult.conflict(message, description, healthStateType);
        this.message = message;
        httpCode = 409;
    }

    @Override
    public void notFound(String message, String description, HealthStateType healthStateType) {
        serverHealthStateOperationResult.notFound(message, description, healthStateType);
        httpCode = 404;
        this.message = message;
    }

    public int httpCode() {
        return httpCode;
    }

    public String message() {
        return message;
    }

    @Override
    public void accepted(String message, String description, HealthStateType healthStateType) {
        httpCode = 202;
        this.message = message;
    }

    @Override
    public void ok(String message) {
        httpCode = 200;
        this.message = message;
    }

    @Override
    public void notAcceptable(String message, final HealthStateType type) {
        notAcceptable(message, "", type);
    }

    @Override
    public void internalServerError(String message, HealthStateType type) {
        serverHealthStateOperationResult.internalServerError(message, type);
        httpCode = 500;
        this.message = message;
    }

    @Override
    public void notAcceptable(String message, String description, final HealthStateType type) {
        serverHealthStateOperationResult.notAcceptable(message, description, type);
        httpCode = 406;
        this.message = message;
    }

    public String fullMessage() {
        ServerHealthState serverHealthState = serverHealthStateOperationResult.getServerHealthState();
        String desc = BLANK_STRING;
        if(serverHealthState != null && !StringUtils.equals(serverHealthState.getDescription(), message)) {
            desc = serverHealthState.getDescription();
        }

        return StringUtils.isBlank(desc) ? message : String.format("%s { %s }", message, desc);
    }

    public String detailedMessage() {        //cache me if gc mandates so -jj
        return fullMessage() + "\n";
    }
}
