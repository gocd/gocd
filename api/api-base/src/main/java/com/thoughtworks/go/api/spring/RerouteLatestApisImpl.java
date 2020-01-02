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
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;
import spark.*;
import spark.route.HttpMethod;
import spark.route.Routes;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import static com.thoughtworks.go.api.ApiVersion.LATEST_VERSION_MIMETYPE;

@Component
public class RerouteLatestApisImpl implements RerouteLatestApis {

    public List<?> routeList() {
        Service service = getService();

        // reflection equivalent of:
        // Routes routes = service.routes
        Routes routes = getField(service, "routes");

        // reflection equivalent of:
        // List<RouteEntry> routesList = routes.routes
        List<?> routesList = getField(routes, "routes");

        // return a copy of the array, instead of the reference
        return new ArrayList<>(routesList);
    }

    // ignore some "special" paths
    private boolean shouldIgnore(String acceptHeader) {
        return acceptHeader.equals("*/*");
    }

    private Service getService() {
        // reflection equivalent of:
        // Service service = Spark.getInstance()
        Method getInstanceMethod = ReflectionUtils.findMethod(Spark.class, "getInstance");
        ReflectionUtils.makeAccessible(getInstanceMethod);
        return (Service) ReflectionUtils.invokeMethod(getInstanceMethod, null);
    }

    private static <T> T getField(Object o, String fieldName) {
        Field field = ReflectionUtils.findField(o.getClass(), fieldName);
        ReflectionUtils.makeAccessible(field);
        return (T) ReflectionUtils.getField(field, o);
    }

    @Override
    public void registerLatest() {
        List<?> routes = routeList();

        // get all known paths and versions corresponding to each path
        Multimap<String, ApiVersion> pathToVersionsMap = LinkedHashMultimap.create();

        routes.forEach(eachRouteEntry /* of type RouteEntry*/ -> {
            String path = getField(eachRouteEntry, "path");
            String acceptedType = getField(eachRouteEntry, "acceptedType");
            if (shouldIgnore(acceptedType)) {
                return;
            }

            ApiVersion version = ApiVersion.parse(acceptedType);

            pathToVersionsMap.put(path, version);
        });


        routes.forEach(eachRouteEntry -> {
            HttpMethod httpMethod = getField(eachRouteEntry, "httpMethod");
            String path = getField(eachRouteEntry, "path");
            String acceptedType = getField(eachRouteEntry, "acceptedType");
            Object target = getField(eachRouteEntry, "target");

            if (shouldIgnore(acceptedType)) {
                return;
            }

            // get the api versions for this path
            Collection<ApiVersion> apiVersions = pathToVersionsMap.get(path);
            // get the max version supported by this path
            ApiVersion maxVersion = apiVersions.stream().max(Comparator.naturalOrder()).orElseThrow(() -> new IllegalArgumentException("Unable to lookup apiversion for " + path));

            // if this route corresponds to the latest version, then also register that route with latest version.
            if (maxVersion.mimeType().equals(acceptedType)) {
                Service service = getService();
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
    }
}
