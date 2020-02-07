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

import java.net.URI;
import java.net.URISyntaxException;

public class BitBucketCloudRepository {
    @SerializedName("scm")
    private String scm;

    @SerializedName("links")
    private Links links;

    @SerializedName("full_name")
    private String fullName;

    public String getHostname() {
        try {
            return new URI(links.html.href).getHost();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public String getFullName() {
        return fullName;
    }

    public String scm() {
        return scm;
    }

    public static class Links {
        @SerializedName("html")
        private Link html;
    }

    public static class Link {
        @SerializedName("href")
        private String href;
    }
}
