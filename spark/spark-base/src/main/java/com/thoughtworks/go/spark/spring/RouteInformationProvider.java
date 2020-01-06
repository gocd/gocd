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

package com.thoughtworks.go.spark.spring;

import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;
import spark.Service;
import spark.Spark;
import spark.route.Routes;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class RouteInformationProvider {
    private static final List<RouteEntry> routes = new ArrayList<>();

    public RouteInformationProvider() {
    }

    public void cacheRouteInformation() {
        routes.clear();
        routeList().forEach(routeEntry -> {
            routes.add(new RouteEntry(
                getField(routeEntry, "httpMethod"),
                getField(routeEntry, "path"),
                getField(routeEntry, "acceptedType"),
                getField(routeEntry, "target")
            ));
        });
    }

    public List<RouteEntry> getRoutes() {
        return Collections.unmodifiableList(routes);
    }

    private List<?> routeList() {
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

    public Service getService() {
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
}
