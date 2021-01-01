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

package com.thoughtworks.go.apiv1.webhook.controller.validation;

import com.thoughtworks.go.apiv1.webhook.request.WebhookRequest;
import com.thoughtworks.go.config.exceptions.BadRequestException;

import java.util.Set;

import static com.thoughtworks.go.util.Iters.sortJoin;
import static java.lang.String.format;

public interface ValidateEvent {
    BadRequestException fail(String message);

    default void validateEvent(WebhookRequest request, Set<String> allowedEvents) {
        final String event = request.event();

        if (!allowedEvents.contains(request.event())) {
            final String events = sortJoin(allowedEvents, ", ");
            throw fail(format("Invalid event type `%s`. Allowed events are [%s].", event, events));
        }
    }

    default boolean is(Set<String> matching, WebhookRequest req) {
        return matching.contains(req.event());
    }
}
