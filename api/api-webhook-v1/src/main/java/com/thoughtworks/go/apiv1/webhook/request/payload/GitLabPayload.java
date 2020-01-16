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

public class GitLabPayload implements Payload {
    @SerializedName("ref")
    private String ref;

    @SerializedName("project")
    private Project project;

    public GitLabPayload() {
    }

    @Override
    public String getBranch() {
        return RegExUtils.replaceAll(ref, "refs/heads/", "");
    }

    @Override
    public String getHostname() {
        return project.getHostname();
    }

    @Override
    public String getFullName() {
        return project.getFullName();
    }

    public static class Project {
        @SerializedName("http_url")
        private String httpUrl;

        @SerializedName("path_with_namespace")
        private String fullName;

        public String getHostname() {
            try {
                return new URI(httpUrl).getHost();
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }

        public String getFullName() {
            return fullName;
        }
    }
}
