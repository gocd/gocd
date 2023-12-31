/*
 * Copyright 2024 Thoughtworks, Inc.
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
import com.thoughtworks.go.apiv1.webhook.request.json.BitbucketRepository;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static java.util.stream.Collectors.toCollection;

@SuppressWarnings({"MismatchedQueryAndUpdateOfCollection", "unused", "RedundantSuppression"})
public class BitbucketPush implements PushPayload {
    private Push push;

    private BitbucketRepository repository;

    @Override
    public Set<String> branches() {
        return this.push.changes.stream()
                .filter(change -> change.newCommit != null)
                .filter(change -> StringUtils.equalsIgnoreCase(change.newCommit.type, "branch"))
                .map(change -> change.newCommit.name)
                .collect(toCollection(TreeSet::new)); // if pushing a tag, this might be empty
    }

    @Override
    public String hostname() {
        return repository.hostname();
    }

    @Override
    public String fullName() {
        return repository.fullName();
    }

    @Override
    public String scmType() {
        return repository.scm();
    }

    private static class Push {
        private List<Change> changes;
    }

    private static class Change {
        @SerializedName("new")
        private Commit newCommit;
    }

    private static class Commit {
        private String name;

        private String type;
    }
}
