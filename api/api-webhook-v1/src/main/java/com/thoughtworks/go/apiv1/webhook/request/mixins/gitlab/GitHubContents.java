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

package com.thoughtworks.go.apiv1.webhook.request.mixins.gitlab;

import com.thoughtworks.go.apiv1.webhook.request.mixins.RequestContents;
import org.apache.http.client.utils.URLEncodedUtils;
import org.springframework.util.MimeType;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.springframework.http.MediaType.*;

public interface GitHubContents extends RequestContents {
    @Override
    default List<MimeType> supportedContentTypes() {
        return List.of(APPLICATION_JSON, APPLICATION_JSON_UTF8, APPLICATION_FORM_URLENCODED);
    }

    @Override
    default String contents() {
        String raw = request().body();

        if (APPLICATION_FORM_URLENCODED.equals(contentType())) {
            return URLEncodedUtils.parse(raw, StandardCharsets.UTF_8).get(0).getValue();
        }

        return raw;
    }
}
