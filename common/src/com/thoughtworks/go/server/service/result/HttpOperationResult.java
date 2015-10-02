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
import com.thoughtworks.go.serverhealth.ServerHealthState;
import com.thoughtworks.go.util.StringUtil;

/**
 * @understands how to turn the problems that can occur during api calls into human-readable form and http codes
 * @deprecated Use LocalizedOperationResult interface instead
 */
public class HttpOperationResult implements OperationResult {

    private static final String BLANK_STRING = "";
    int httpCode = 200;
    private String message;
    final private ServerHealthStateOperationResult serverHealthStateOperationResult = new ServerHealthStateOperationResult();


    public ServerHealthState success(HealthStateType healthStateType) {
        return serverHealthStateOperationResult.success(healthStateType);
    }

    public ServerHealthState error(String message, String description, HealthStateType type) {
        httpCode = 400;
        this.message = message;
        return serverHealthStateOperationResult.error(message, description, type);
    }

    public void badRequest(String message, String description, HealthStateType healthStateType) {
        error(message, description, healthStateType);
        httpCode = 400;
    }

    public void unprocessibleEntity(String message, String description, HealthStateType healthStateType) {
        error(message, description, healthStateType);
        httpCode = 422;
    }

    public boolean isSuccess(){
        return httpCode >= 200 && httpCode <= 299;
    }

    public ServerHealthState warning(String message, String description, HealthStateType type) {
        this.message = message;
        return serverHealthStateOperationResult.warning(message, description, type);
    }

    public ServerHealthState getServerHealthState() {
        return serverHealthStateOperationResult.getServerHealthState();
    }

    public boolean canContinue() {
        return serverHealthStateOperationResult.canContinue();
    }

    public ServerHealthState paymentRequired(String message, String description, HealthStateType type) {
        this.message = message;
        return serverHealthStateOperationResult.paymentRequired(message, description, type);
    }

    public ServerHealthState unauthorized(String message, String description, HealthStateType id) {
        this.message = message;
        httpCode = 401;
        return serverHealthStateOperationResult.unauthorized(message, description, id);
    }

    public void conflict(String message, String description, HealthStateType healthStateType) {
        serverHealthStateOperationResult.conflict(message, description, healthStateType);
        this.message = message;
        httpCode = 409;
    }

    public void notFound(String message, String description, HealthStateType healthStateType) {
        serverHealthStateOperationResult.notFound(message, description, healthStateType);
        httpCode = 404;
        this.message = message;
    }

    public int httpCode() {
        return httpCode;
    }

    public String message(){
        return message;
    }

    public void accepted(String message, String description, HealthStateType healthStateType) {
        httpCode = 202;
        this.message = message;
    }

    public void ok(String message) {
        httpCode = 200;
        this.message = message;
    }

    public void notAcceptable(String message, final HealthStateType type) {
        notAcceptable(message, "", type);
    }

    @Override
    public void internalServerError(String message, HealthStateType type) {
        serverHealthStateOperationResult.internalServerError(message, type);
        httpCode = 500;
        this.message = message;
    }

    public void notAcceptable(String message, String description, final HealthStateType type) {
        serverHealthStateOperationResult.notAcceptable(message, description, type);
        httpCode = 406;
        this.message = message;
    }

    public String detailedMessage() {//cache me if gc mandates so -jj
        ServerHealthState serverHealthState = serverHealthStateOperationResult.getServerHealthState();
        String desc = serverHealthState == null ? BLANK_STRING : serverHealthState.getDescription();
        return StringUtil.isBlank(desc) ? message + "\n" : String.format("%s { %s }\n", message, desc);
    }
}
