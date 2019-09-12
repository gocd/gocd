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
package com.thoughtworks.go.server.domain.user;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.thoughtworks.go.config.CaseInsensitiveString;
import lombok.EqualsAndHashCode;

import java.util.*;

import static com.thoughtworks.go.server.domain.user.DashboardFilter.DEFAULT_NAME;
import static com.thoughtworks.go.server.domain.user.Marshaling.*;

@EqualsAndHashCode
public class Filters {
    private static final Gson GSON = new GsonBuilder().
            registerTypeAdapter(Filters.class, new FiltersDeserializer()).
            registerTypeAdapter(Filters.class, new FiltersSerializer()).
            registerTypeAdapter(DashboardFilter.class, new DashboardFilterDeserializer()).
            registerTypeAdapter(DashboardFilter.class, new DashboardFilterSerializer()).
            registerTypeAdapter(CaseInsensitiveString.class, new CaseInsensitiveStringDeserializer()).
            registerTypeAdapter(CaseInsensitiveString.class, new CaseInsensitiveStringSerializer()).
            create();

    public static final DashboardFilter WILDCARD_FILTER = new BlacklistFilter(DEFAULT_NAME, Collections.emptyList(), new HashSet<>());

    public static Filters fromJson(String json) {
        final Filters filters = GSON.fromJson(json, Filters.class);
        filters.updateIndex();
        return filters;
    }

    public static String toJson(Filters filters) {
        return GSON.toJson(filters);
    }

    public static Filters single(DashboardFilter filter) {
        return new Filters(Collections.singletonList(filter));
    }

    public static Filters defaults() {
        return single(WILDCARD_FILTER);
    }

    private List<DashboardFilter> filters;

    private Map<String, DashboardFilter> filterMap; // optimize for find by name

    public Filters(List<DashboardFilter> filters) {
        this.filters = filters;
        updateIndex();
    }

    public DashboardFilter named(String name) {
        FilterValidator.validateNamePresent(name);
        return this.filterMap.getOrDefault(name.toLowerCase(), filters.get(0));
    }

    public List<DashboardFilter> filters() {
        return Collections.unmodifiableList(filters);
    }

    private void updateIndex() {
        this.filterMap = new HashMap<>();
        this.filters.forEach((f) -> {
            FilterValidator.validateFilter(filterMap, f);
            filterMap.put(f.name().toLowerCase(), f);
        });

        FilterValidator.validateDefaultIsPresent(filterMap);
    }
}
