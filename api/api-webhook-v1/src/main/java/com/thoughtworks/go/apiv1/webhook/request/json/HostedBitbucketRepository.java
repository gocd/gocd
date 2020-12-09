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

package com.thoughtworks.go.apiv1.webhook.request.json;

import com.google.gson.annotations.SerializedName;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;

@SuppressWarnings({"unused", "RedundantSuppression"})
public class HostedBitbucketRepository {
    @SerializedName("scmId")
    private String scm;

    private Links links;

    private String slug;

    private Project project;

    public String hostname() {
        return getHttpUrl().getHost();
    }

    public String fullName() {
        return format("%s/%s", project.key.toLowerCase(), slug);
    }

    private URL getHttpUrl() {
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

    public String scm() {
        return scm;
    }

    public List<String> cloneLinks() {
        return links.cloneLinks.stream().map(Link::href).collect(toList());
    }

    private static class Project {
        private String key;
    }

    private static class Links {
        @SerializedName("clone")
        private List<Link> cloneLinks;

        public List<Link> cloneLinks() {
            return cloneLinks;
        }
    }

    public static class Link {
        private String name;

        private String href;

        public String name() {
            return name;
        }

        public String href() {
            return href;
        }
    }
}
