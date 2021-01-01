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

package com.thoughtworks.go.apiv1.webhook.request.payload.pr;

import com.thoughtworks.go.apiv1.webhook.request.payload.Payload;

import static java.lang.String.format;

public interface PrPayload extends Payload {
    enum State {
        OPEN,
        CLOSED
    }

    String identifier();

    State state();

    default String descriptor() {
        return format("%s[%s][%s][%s]",
                getClass().getSimpleName(),
                fullName(),
                identifier(),
                state()
        );
    }
}
