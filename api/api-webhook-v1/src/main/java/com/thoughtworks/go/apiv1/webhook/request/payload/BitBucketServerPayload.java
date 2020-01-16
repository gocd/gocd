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
import com.thoughtworks.go.config.exceptions.BadRequestException;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.removeEnd;

public class BitBucketServerPayload implements Payload {
    @SerializedName("changes")
    private List<Change> changes;

    @SerializedName("repository")
    private Repository repository;

    public BitBucketServerPayload() {
    }

    @Override
    public String getBranch() {
        return this.changes.stream()
            .filter(change -> change.ref != null)
            .filter(change -> equalsIgnoreCase(change.ref.type, "branch"))
            .map(change -> change.ref.displayId)
            .findFirst()
            .orElseThrow(() -> new BadRequestException("Payload must contain branch name!!"));
    }

    @Override
    public String getHostname() {
        return repository.getHostname();
    }

    @Override
    public String getFullName() {
        return removeEnd(repository.getHtmlUrl().getPath(), ".git");
    }

    public String getScmType() {
        return repository.scm;
    }

    public List<String> getCloneUrls() {
        return repository.links.cloneLinks.stream()
            .map(withoutCredentials())
            .collect(Collectors.toList());
    }

    private Function<Link, String> withoutCredentials() {
        return link -> {
            try {
                URI actual = new URI(link.href);
                return new URI(actual.getScheme(),
                    null,
                    actual.getHost(),
                    actual.getPort(),
                    actual.getPath(),
                    actual.getQuery(),
                    actual.getFragment()).toString();
            } catch (URISyntaxException e) {
                return link.href;
            }
        };
    }

    public static class Change {
        @SerializedName("ref")
        private Ref ref;
    }

    public static class Ref {
        @SerializedName("displayId")
        private String displayId;

        @SerializedName("type")
        private String type;
    }

    public static class Repository {
        @SerializedName("scmId")
        private String scm;

        @SerializedName("links")
        private Links links;

        @SerializedName("name")
        private String name;

        public String getHostname() {
            return getHtmlUrl().getHost();
        }

        public URL getHtmlUrl() {
            try {
                Link htmlLink = links.cloneLinks.stream()
                    .filter(link -> equalsIgnoreCase(link.name, "http"))
                    .findFirst()
                    .orElseThrow();
                return new URL(htmlLink.href);
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static class Links {
        @SerializedName("clone")
        private List<Link> cloneLinks;
    }

    private static class Link {
        @SerializedName("name")
        private String name;

        @SerializedName("href")
        private String href;
    }
}
