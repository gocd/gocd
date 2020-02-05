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

import com.thoughtworks.go.apiv1.webhook.request.mixins.HasAuth;
import org.h2.util.Utils;

import static org.apache.commons.lang3.StringUtils.isBlank;

public interface GitLabAuth extends HasAuth {
    default void validateAuth(String webhookSecret) {
        String token = request().headers("X-Gitlab-Token");

        if (isBlank(token)) {
            throw die("No token specified in the 'X-Gitlab-Token' header!");
        }

        if (!Utils.compareSecure(token.getBytes(), webhookSecret.getBytes())) {
            throw die("Token specified in the 'X-Gitlab-Token' header did not match!");
        }
    }
}
