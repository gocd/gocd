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

package com.thoughtworks.go.apiv1.webhook.request.mixins;

import org.springframework.util.MimeType;

import java.util.Set;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;

public interface RequestContents extends WrapsRequest {
    default MimeType contentType() {
        return MimeType.valueOf(request().contentType());
    }

    default String contents() {
        return request().body();
    }

    default Set<MimeType> supportedContentTypes() {
        return Set.of(APPLICATION_JSON, APPLICATION_JSON_UTF8);
    }
}
