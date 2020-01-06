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

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.spark.RerouteLatestApis;
import com.thoughtworks.go.spark.spring.RouteEntry;
import com.thoughtworks.go.spark.spring.RouteInformationProvider;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.*;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import static com.thoughtworks.go.api.ApiVersion.LATEST_VERSION_MIMETYPE;

@Component
public class RerouteLatestApisImpl implements RerouteLatestApis {
    private final RouteInformationProvider routeInformationProvider;

    @Autowired
    public RerouteLatestApisImpl(RouteInformationProvider routeInformationProvider) {
        this.routeInformationProvider = routeInformationProvider;
    }

    // ignore some "special" paths
    private boolean shouldIgnore(String acceptHeader) {
        return acceptHeader.equals("*/*");
    }

    // ignore some "special" paths
    private boolean ignoreFeedApis(String path) {
        return StringUtils.startsWithIgnoreCase(path, "/api/feed/");
    }

    @Override
    public void registerLatest() {
        List<RouteEntry> routes = routeInformationProvider.getRoutes();
        Multimap<String, ApiVersion> pathToVersionsMap = LinkedHashMultimap.create();

        routes.forEach(entry -> {
            if (shouldIgnore(entry.getAcceptedType()) || ignoreFeedApis(entry.getPath())) {
                return;
            }

            ApiVersion version = ApiVersion.parse(entry.getAcceptedType());
            pathToVersionsMap.put(entry.getPath(), version);
        });


        routes.forEach(routeEntry -> {

            if (shouldIgnore(routeEntry.getAcceptedType()) || ignoreFeedApis(routeEntry.getPath())) {
                return;
            }

            // get the api versions for this path
            Collection<ApiVersion> apiVersions = pathToVersionsMap.get(routeEntry.getPath());
            // get the max version supported by this path
            ApiVersion maxVersion = apiVersions.stream().max(Comparator.naturalOrder()).orElseThrow(() -> new IllegalArgumentException("Unable to lookup apiversion for " + routeEntry.getPath()));

            // if this route corresponds to the latest version, then also register that route with latest version.
            if (maxVersion.mimeType().equals(routeEntry.getAcceptedType())) {
                Service service = routeInformationProvider.getService();
                if (routeEntry.getTarget() instanceof Route) {
                    service.addRoute(routeEntry.getHttpMethod(), RouteImpl.create(routeEntry.getPath(), LATEST_VERSION_MIMETYPE, (Route) routeEntry.getTarget()));
                } else if (routeEntry.getTarget() instanceof Filter) {
                    service.addFilter(routeEntry.getHttpMethod(), new FilterImpl(routeEntry.getPath(), LATEST_VERSION_MIMETYPE, ((Filter) routeEntry.getTarget())) {
                        @Override
                        public void handle(Request request, Response response) throws Exception {
                            ((Filter) routeEntry.getTarget()).handle(request, response);
                        }
                    });
                } else {
                    throw new IllegalArgumentException("Unexpected target type " + routeEntry.getTarget().getClass());
                }
            }

        });
        routeInformationProvider.cacheRouteInformation();
    }
}
