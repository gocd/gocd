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
import com.thoughtworks.go.server.service.support.toggle.FeatureToggleService;
import com.thoughtworks.go.spark.RerouteLatestApis;
import com.thoughtworks.go.spark.spring.RouteEntry;
import com.thoughtworks.go.spark.spring.RouteInformationProvider;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import spark.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiFunction;

import static com.thoughtworks.go.api.ApiVersion.LATEST_VERSION_MIMETYPE;

@Component
public class RerouteLatestApisImpl implements RerouteLatestApis, ApplicationContextAware {
    private final RouteInformationProvider routeInformationProvider;
    private final FeatureToggleService features;
    private ApplicationContext applicationContext;
    private RouteToggles routeToggles;

    @Autowired
    public RerouteLatestApisImpl(RouteInformationProvider routeInformationProvider, FeatureToggleService features) {
        this.routeInformationProvider = routeInformationProvider;
        this.features = features;
    }

    // ignore special paths and those that are toggled off
    private boolean shouldIgnore(RouteEntry entry) {
        if (routeToggles.matches(entry)) {
            return !routeToggles.isToggledOn(entry);
        }

        return entry.getAcceptedType().equals("*/*");
    }

    // ignore feed API paths
    private boolean ignoreFeedApis(String path) {
        return StringUtils.startsWithIgnoreCase(path, "/api/feed/");
    }

    @Override
    public void registerLatest() {
        List<RouteEntry> routes = routeInformationProvider.getRoutes();
        Multimap<String, ApiVersion> pathToVersionsMap = LinkedHashMultimap.create();

        routeToggles = resolveRouteToggles();

        routes.forEach(entry -> {
            if (shouldIgnore(entry) || ignoreFeedApis(entry.getPath())) {
                return;
            }

            ApiVersion version = ApiVersion.parse(entry.getAcceptedType());
            pathToVersionsMap.put(entry.getPath(), version);
        });

        routes.forEach(routeEntry -> {
            if (shouldIgnore(routeEntry) || ignoreFeedApis(routeEntry.getPath())) {
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

    private RouteToggles resolveRouteToggles() {
        return new RouteToggles(applicationContext.getBeansWithAnnotation(ToggleRegisterLatest.class).values().stream().
                reduce(new ArrayList<>(), (BiFunction<List<RouteToggle>, Object, List<RouteToggle>>) (all, o) -> {
                    for (ToggleRegisterLatest meta : o.getClass().getAnnotationsByType(ToggleRegisterLatest.class)) {
                        all.add(new RouteToggle(meta.controllerPath(), meta.apiVersion(), meta.as(), meta.includeDescendants()));
                    }
                    return all;
                }, (l, r) -> r) /* ignored, not parallel */,
                features);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
