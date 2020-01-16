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
import org.springframework.util.MimeType;
import spark.Request;

import java.lang.reflect.ParameterizedType;
import java.util.List;

import static java.lang.String.format;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;

public abstract class WebhookRequest<T extends Payload> {
    private final String event;
    private final String body;
    private final MimeType contentType;
    private T payload;

    public WebhookRequest(Request request) {
        event = parseEvent(request);
        body = request.body();
        contentType = MimeType.valueOf(request.contentType());
    }

    public String getEvent() {
        return event;
    }

    public T getPayload() {
        if (payload == null) {
            this.payload = parsePayload(contentType);
        }
        return payload;
    }

    public abstract void validate(String webhookSecret);

    public abstract List<String> webhookUrls();

    public String getRawBody() {
        return body;
    }

    protected abstract String parseEvent(Request request);

    protected List<MimeType> supportedContentType() {
        return List.of(APPLICATION_JSON, APPLICATION_JSON_UTF8);
    }

    protected T parsePayload(MimeType contentType) {
        if (!supportedContentType().contains(contentType)) {
            throw new BadRequestException(format("Could not understand the content type '%s'!", contentType));
        }

        if (contentType.equals(APPLICATION_JSON) || contentType.equals(APPLICATION_JSON_UTF8)) {
            return GsonTransformer.getInstance().fromJson(body, getParameterClass());
        }

        throw new BadRequestException("Could not understand the payload!");
    }

    @SuppressWarnings("unchecked")
    protected Class<T> getParameterClass() {
        return ((Class<T>) ((ParameterizedType) getClass().getGenericSuperclass())
            .getActualTypeArguments()[0]);
    }
}
