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

package com.thoughtworks.go.apiv1.webhook.request.payload.push;

import com.thoughtworks.go.apiv1.webhook.request.json.GitLabProject;

import java.util.Set;

import static java.lang.String.format;
import static java.util.Collections.unmodifiableSet;
import static org.apache.commons.collections4.SetUtils.union;
import static org.apache.commons.lang3.StringUtils.removeStart;

@SuppressWarnings({"unused", "RedundantSuppression"})
public class GitLabPush implements PushPayload {
    private String ref;

    private GitLabProject project;

    @Override
    public String branch() {
        return ref.startsWith("refs/heads/") ? removeStart(ref, "refs/heads/") : "";
    }

    @Override
    public String hostname() {
        return project.hostname();
    }

    @Override
    public String fullName() {
        return project.fullName();
    }

    @Override
    public Set<String> repoUrls() {
        return unmodifiableSet(union(
                possibleUrls(hostname(), fullName()),
                Set.of(
                        format("gitlab@%s:%s", hostname(), fullName()),
                        format("gitlab@%s:%s/", hostname(), fullName()),
                        format("gitlab@%s:%s.git", hostname(), fullName()),
                        format("gitlab@%s:%s.git/", hostname(), fullName())
                )
        ));
    }
}
