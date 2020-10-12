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
import com.thoughtworks.go.apiv1.webhook.request.mixins.RequestContents;
import com.thoughtworks.go.apiv1.webhook.request.mixins.ValidatingRequest;
import com.thoughtworks.go.apiv1.webhook.request.payload.Payload;
import com.thoughtworks.go.config.exceptions.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.MimeType;
import spark.QueryParamsMap;
import spark.Request;

import java.lang.reflect.ParameterizedType;
import java.util.List;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

public abstract class WebhookRequest<T extends Payload> implements ValidatingRequest, RequestContents {
    public static final String KEY_SCM_NAME = "SCM_NAME";
    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    private final Request request;
    private T payload;

    public WebhookRequest(Request request) {
        this.request = request;
    }

    public Request request() {
        return request;
    }

    public T getPayload() {
        if (payload == null) {
            this.payload = parsePayload();
        }
        return payload;
    }

    public BadRequestException die(String message) {
        LOGGER.error("[WebHook] " + message);
        return new BadRequestException(message);
    }

    protected T parsePayload() {
        MimeType contentType = contentType();

        if (!supportedContentTypes().contains(contentType)) {
            throw die(format("Could not understand the Content-Type: %s!", contentType));
        }

        String body = contents();

        try {
            LOGGER.debug(format("[WebHook] Parsing '%s' payload!", contentType));
            return GsonTransformer.getInstance().fromJson(body, getParameterClass());
        } catch (Throwable e) {
            throw die(format("Failed to deserialize payload. Error: %s; Body: %s; Content-Type: %s", e.getMessage(), body, contentType));
        }
    }

    protected Class<T> getParameterClass() {
        return ((Class<T>) ((ParameterizedType) getClass().getGenericSuperclass())
                .getActualTypeArguments()[0]);
    }

    public List<String> getScmNames() {
        QueryParamsMap queryMap = this.request.queryMap();
        return queryMap.hasKey(KEY_SCM_NAME) ? asList(queryMap.get(KEY_SCM_NAME).values()) : emptyList();
    }

}
