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

package com.thoughtworks.go.api.spring;

import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.server.service.support.toggle.FeatureToggleService;
import com.thoughtworks.go.spark.spring.RouteEntry;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;

import java.net.URISyntaxException;

class RouteToggle {
    String prefix;
    ApiVersion version;
    String toggleName;
    boolean descend;

    RouteToggle(String prefix, ApiVersion version, String toggleName, boolean descend) {
        this.prefix = normalize(prefix);
        this.version = version;
        this.descend = descend;
        if (StringUtils.isBlank((toggleName))) {
            this.toggleName = version.toString() + "_" + prefix.replaceAll("/", "_");
        } else {
            this.toggleName = toggleName;
        }
    }

    boolean matches(RouteEntry entry) {
        return entry.getAcceptedType().equals(version.mimeType()) &&
                this.withinPath(entry.getPath());
    }

    boolean isToggleOn(FeatureToggleService s) {
        return s.isToggleOn(this.toggleName);
    }

    private boolean withinPath(String path) {
        return path.equals(this.prefix) || (descend ? path.startsWith(this.prefix + "/") : path.equals(this.prefix + "/"));
    }

    private String normalize(String path) {
        try {
            return new URIBuilder().setPath(path).build().normalize().getPath().replaceAll("/$", "");
        } catch (URISyntaxException e) {
            return path;
        }
    }
}
