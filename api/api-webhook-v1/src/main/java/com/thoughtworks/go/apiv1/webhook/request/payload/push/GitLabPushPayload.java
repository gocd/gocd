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

package com.thoughtworks.go.apiv1.webhook.request.payload.push;

import com.google.gson.annotations.SerializedName;
import com.thoughtworks.go.apiv1.webhook.request.json.GitLabProject;
import org.apache.commons.lang3.RegExUtils;

public class GitLabPushPayload implements PushPayload {
    @SerializedName("ref")
    private String ref;

    @SerializedName("project")
    private GitLabProject project;

    public GitLabPushPayload() {
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

}
