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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import spark.*;
import spark.route.HttpMethod;

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

    @Override
    public void registerLatest() {
        List<RouteEntry> routes = routeInformationProvider.getRoutes();
        Multimap<String, ApiVersion> pathToVersionsMap = LinkedHashMultimap.create();

        routes.forEach(entry -> {
            if (shouldIgnore(entry.getAcceptedType())) {
                return;
            }

            ApiVersion version = ApiVersion.parse(entry.getAcceptedType());
            pathToVersionsMap.put(entry.getPath(), version);
        });


        routes.forEach(routeEntry -> {
            HttpMethod httpMethod = routeEntry.getHttpMethod();
            String path = routeEntry.getPath();
            String acceptedType = routeEntry.getAcceptedType();
            Object target = routeEntry.getTarget();

            if (shouldIgnore(acceptedType)) {
                return;
            }

            // get the api versions for this path
            Collection<ApiVersion> apiVersions = pathToVersionsMap.get(path);
            // get the max version supported by this path
            ApiVersion maxVersion = apiVersions.stream().max(Comparator.naturalOrder()).orElseThrow(() -> new IllegalArgumentException("Unable to lookup apiversion for " + path));

            // if this route corresponds to the latest version, then also register that route with latest version.
            if (maxVersion.mimeType().equals(acceptedType)) {
                Service service = routeInformationProvider.getService();
                if (target instanceof Route) {
                    service.addRoute(httpMethod, RouteImpl.create(path, LATEST_VERSION_MIMETYPE, (Route) target));
                } else if (target instanceof Filter) {
                    service.addFilter(httpMethod, new FilterImpl(path, LATEST_VERSION_MIMETYPE, ((Filter) target)) {
                        @Override
                        public void handle(Request request, Response response) throws Exception {
                            ((Filter) target).handle(request, response);
                        }
                    });
                } else {
                    throw new IllegalArgumentException("Unexpected target type " + target.getClass());
                }
            }

        });
        routeInformationProvider.cacheRouteInformation();
    }
}
