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

package com.thoughtworks.go.apiv1.webhook.request.payload;

import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.RegExUtils;

import java.net.URI;
import java.net.URISyntaxException;

public class GitHubPayload implements Payload {
    @SerializedName("ref")
    private String ref;

    @SerializedName("zen")
    private String zen;

    @SerializedName("repository")
    private Repository repository;

    public GitHubPayload() {
    }

    public String getZen() {
        return zen;
    }

    @Override
    public String getBranch() {
        return RegExUtils.replaceAll(ref, "refs/heads/", "");
    }

    @Override
    public String getHostname() {
        return repository.getHostname();
    }

    @Override
    public String getFullName() {
        return repository.getFullName();
    }

    public static class Repository {
        @SerializedName("html_url")
        private String htmlUrl;

        @SerializedName("full_name")
        private String fullName;

        public String getHostname() {
            try {
                return new URI(htmlUrl).getHost();
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }

        public String getFullName() {
            return fullName;
        }
    }
}
