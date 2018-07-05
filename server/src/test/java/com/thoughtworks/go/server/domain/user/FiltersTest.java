/*
 * Copyright 2018 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.CaseInsensitiveString;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FiltersTest {

    @Test
    void fromJson() {
        String json = "{ \"filters\": [{\"name\": \"My Filter\", \"type\": \"whitelist\", \"pipelines\": [\"p1\"]}] }";
        final Filters filters = Filters.fromJson(json);
        assertEquals(1, filters.filters().size());
        final DashboardFilter first = filters.filters().get(0);
        assertEquals(first.name(), "My Filter");
        assertTrue(first instanceof WhitelistFilter);
        assertEquals(1, ((WhitelistFilter) first).pipelines().size());
        assertTrue(((WhitelistFilter) first).pipelines().contains(new CaseInsensitiveString("p1")));
    }

    @Test
    void toJson() {
        List<DashboardFilter> views = new ArrayList<>();
        final List<CaseInsensitiveString> pipelines = Collections.singletonList(new CaseInsensitiveString("Pipely McPipe"));
        final BlacklistFilter first = new BlacklistFilter("Cool Pipelines", null, pipelines);
        views.add(first);
        final Filters filters = new Filters(views);

        assertEquals("{\"filters\":[{\"name\":\"Cool Pipelines\",\"pipelines\":[\"Pipely McPipe\"],\"type\":\"blacklist\"}]}", Filters.toJson(filters));
    }
}