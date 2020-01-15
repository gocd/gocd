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

package com.thoughtworks.go.apiv1.webhook.request;

import com.thoughtworks.go.api.util.GsonTransformer;
import com.thoughtworks.go.apiv1.webhook.request.payload.Payload;
import com.thoughtworks.go.config.exceptions.BadRequestException;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import spark.Request;

import java.lang.reflect.ParameterizedType;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static java.lang.String.format;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

public abstract class WebhookRequest<T extends Payload> {
    private final String event;
    private final T payload;
    private final String body;

    public WebhookRequest(Request request) {
        event = parseEvent(request);
        body = request.body();
        payload = parsePayload(request.contentType());
    }

    public String getEvent() {
        return event;
    }

    public T getPayload() {
        return payload;
    }

    public abstract void validate(String webhookSecret);

    public abstract List<String> webhookUrls();

    public String getRawBody() {
        return body;
    }

    protected abstract String parseEvent(Request request);

    protected List<String> supportedContentType() {
        return List.of(APPLICATION_FORM_URLENCODED_VALUE, APPLICATION_JSON_VALUE);
    }

    private T parsePayload(String contentType) {
        if (!supportedContentType().contains(contentType)) {
            throw new BadRequestException(format("Could not understand the content type '%s'!", contentType));
        }

        if (StringUtils.equals(contentType, APPLICATION_JSON_VALUE)) {
            return GsonTransformer.getInstance().fromJson(body, getParameterClass());
        }

        if (StringUtils.equals(contentType, APPLICATION_FORM_URLENCODED_VALUE)) {
            List<NameValuePair> formData = URLEncodedUtils.parse(body, StandardCharsets.UTF_8);
            return GsonTransformer.getInstance().fromJson(formData.get(0).getValue(), getParameterClass());
        }

        throw new BadRequestException("Could not understand the payload!");
    }

    @SuppressWarnings("unchecked")
    protected Class<T> getParameterClass() {
        return ((Class<T>) ((ParameterizedType) getClass().getGenericSuperclass())
            .getActualTypeArguments()[0]);
    }
}
