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

package com.thoughtworks.go.apiv1.webhook.requests;

import com.google.gson.reflect.TypeToken;
import com.thoughtworks.go.api.util.GsonTransformer;
import com.thoughtworks.go.apiv1.webhook.requests.models.Payload;
import com.thoughtworks.go.config.exceptions.BadRequestException;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import spark.Request;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static java.lang.String.format;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

public abstract class WebhookRequest<T extends Payload> {
    protected final String event;
    protected final T payload;

    public WebhookRequest(Request request) {
        event = parseEvent(request);
        payload = parsePayload(request);
    }


    public abstract String parseEvent(Request request);

    public List<String> supportedContentType() {
        return List.of(APPLICATION_FORM_URLENCODED_VALUE, APPLICATION_JSON_VALUE);
    }

    private T parsePayload(Request request) {
        if (!supportedContentType().contains(request.contentType())) {
            throw new BadRequestException(format("Could not understand the content type '%s'!", request.contentType()));
        }

        Type payloadType = new TypeToken<T>() {
        }.getType();

        if (StringUtils.equals(request.contentType(), APPLICATION_JSON_VALUE)) {
            return GsonTransformer.getInstance().fromJson(request.body(), payloadType);
        }

        if (StringUtils.equals(request.contentType(), APPLICATION_FORM_URLENCODED_VALUE)) {
            List<NameValuePair> formData = URLEncodedUtils.parse(request.body(), StandardCharsets.UTF_8);
            return GsonTransformer.getInstance().fromJson(formData.get(0).getValue(), payloadType);
        }

        throw new BadRequestException("Could not understand the payload!");
    }
}
