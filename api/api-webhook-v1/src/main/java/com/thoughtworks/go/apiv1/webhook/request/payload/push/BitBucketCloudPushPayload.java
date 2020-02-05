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
import com.thoughtworks.go.apiv1.webhook.request.json.BitBucketCloudRepository;
import com.thoughtworks.go.config.exceptions.BadRequestException;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class BitBucketCloudPushPayload implements PushPayload {
    @SerializedName("push")
    private Push push;

    @SerializedName("repository")
    private BitBucketCloudRepository repository;

    public BitBucketCloudPushPayload() {
    }

    @Override
    public String getBranch() {
        return this.push.changes.stream()
                .filter(change -> change.newCommit != null)
                .filter(change -> StringUtils.equalsIgnoreCase(change.newCommit.type, "branch"))
                .map(change -> change.newCommit.name)
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Payload must contain branch name!!"));
    }

    @Override
    public String getHostname() {
        return repository.getHostname();
    }

    @Override
    public String getFullName() {
        return repository.getFullName();
    }

    public String getScmType() {
        return repository.scm();
    }

    public static class Push {
        @SerializedName("changes")
        private List<Change> changes;
    }

    public static class Change {
        @SerializedName("new")
        private Commit newCommit;
    }

    public static class Commit {
        @SerializedName("name")
        private String name;

        @SerializedName("type")
        private String type;
    }
}
