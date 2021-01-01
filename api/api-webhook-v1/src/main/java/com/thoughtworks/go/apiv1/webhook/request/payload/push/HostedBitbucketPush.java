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

import com.thoughtworks.go.apiv1.webhook.request.json.HostedBitbucketRepository;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;

@SuppressWarnings({"MismatchedQueryAndUpdateOfCollection", "unused", "RedundantSuppression"})
public class HostedBitbucketPush implements PushPayload {
    private List<Change> changes;

    private HostedBitbucketRepository repository;

    @Override
    public String branch() {
        return this.changes.stream()
                .filter(change -> change.ref != null)
                .filter(change -> equalsIgnoreCase(change.ref.type, "branch"))
                .map(change -> change.ref.displayId)
                .findFirst()
                .orElse(""); // if pushing a tag, this might be blank
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

    @Override
    public Set<String> repoUrls() {
        return repository.cloneLinks().stream()
                .map(withoutCredentials())
                .collect(Collectors.toSet());
    }

    private Function<String, String> withoutCredentials() {
        return href -> {
            try {
                final URI actual = new URI(href);
                return new URI(actual.getScheme(),
                        null,
                        actual.getHost(),
                        actual.getPort(),
                        actual.getPath(),
                        actual.getQuery(),
                        actual.getFragment()).toString();
            } catch (URISyntaxException e) {
                return href;
            }
        };
    }

    private static class Change {
        private Ref ref;
    }

    private static class Ref {
        private String displayId;

        private String type;
    }
}
