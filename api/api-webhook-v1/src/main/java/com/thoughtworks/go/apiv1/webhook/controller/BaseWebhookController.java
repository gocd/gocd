/*
 * Copyright 2021 ThoughtWorks, Inc.
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

package com.thoughtworks.go.apiv1.webhook.controller;

import com.thoughtworks.go.api.ApiController;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.apiv1.webhook.controller.validation.WebhookValidation;
import com.thoughtworks.go.spark.Routes;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.springframework.http.HttpStatus;
import spark.Response;

public abstract class BaseWebhookController extends ApiController implements SparkSpringController, WebhookValidation {
    public static final String PING_RESPONSE = "pong";

    protected BaseWebhookController(ApiVersion apiVersion) {
        super(apiVersion);
    }

    @Override
    public String controllerBasePath() {
        return Routes.Webhook.BASE;
    }

    protected String success(final Response response) {
        return accepted(response, "OK!");
    }

    protected String acknowledge(final Response response) {
        return accepted(response, PING_RESPONSE);
    }

    protected String accepted(Response response, String message) {
        return renderMessage(response, HttpStatus.ACCEPTED.value(), message);
    }
}
