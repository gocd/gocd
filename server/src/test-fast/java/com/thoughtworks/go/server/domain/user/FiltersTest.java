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

import com.thoughtworks.go.config.CaseInsensitiveString;
import org.junit.jupiter.api.Test;

import java.util.*;

import static com.thoughtworks.go.server.domain.user.DashboardFilter.DEFAULT_NAME;
import static com.thoughtworks.go.server.domain.user.FilterValidator.*;
import static com.thoughtworks.go.server.domain.user.Filters.WILDCARD_FILTER;
import static org.junit.jupiter.api.Assertions.*;


class FiltersTest {

    private static final String TWENTY_CHAR = "0123456789abcdefghij";
    private static final String NAME_TOO_LONG = TWENTY_CHAR + TWENTY_CHAR + TWENTY_CHAR + TWENTY_CHAR;

    @Test
    void validatesNameFormatOnConstruction() {
        DashboardFilter a = namedIncludes("¯\\_(ツ)_/¯");

        Throwable e = assertThrows(FilterValidationException.class, () -> new Filters(Collections.singletonList(a)));
        assertEquals(MSG_NAME_FORMAT, e.getMessage());

        DashboardFilter b = namedIncludes(" filter");

        e = assertThrows(FilterValidationException.class, () -> new Filters(Collections.singletonList(b)));
        assertEquals(MSG_NO_LEADING_TRAILING_SPACES, e.getMessage());

        DashboardFilter c = namedIncludes("filter ");

        e = assertThrows(FilterValidationException.class, () -> new Filters(Collections.singletonList(c)));
        assertEquals(MSG_NO_LEADING_TRAILING_SPACES, e.getMessage());

        DashboardFilter d = namedIncludes(NAME_TOO_LONG);
        e = assertThrows(FilterValidationException.class, () -> new Filters(Collections.singletonList(d)));
        assertEquals(MSG_MAX_LENGTH, e.getMessage());
    }

    @Test
    void validatesNameFormatOnDeserialize() {
        final String json = "{ \"filters\": [" +
                "  {\"name\": \"¯\\\\_(\\\\u30C4)_/¯\", \"type\": \"whitelist\", \"pipelines\": []}" +
                "] }";
        Throwable e = assertThrows(FilterValidationException.class, () -> Filters.fromJson(json));
        assertEquals(MSG_NAME_FORMAT, e.getMessage());

        final String json1 = "{ \"filters\": [" +
                "  {\"name\": \" filter\", \"type\": \"whitelist\", \"pipelines\": []}" +
                "] }";
        e = assertThrows(FilterValidationException.class, () -> Filters.fromJson(json1));
        assertEquals(MSG_NO_LEADING_TRAILING_SPACES, e.getMessage());

        final String json2 = "{ \"filters\": [" +
                "  {\"name\": \"filter \", \"type\": \"whitelist\", \"pipelines\": []}" +
                "] }";
        e = assertThrows(FilterValidationException.class, () -> Filters.fromJson(json2));
        assertEquals(MSG_NO_LEADING_TRAILING_SPACES, e.getMessage());

        final String json4 = "{ \"filters\": [" +
                "  {\"name\": \"" + NAME_TOO_LONG + "\", \"type\": \"whitelist\", \"pipelines\": []}" +
                "] }";
        e = assertThrows(FilterValidationException.class, () -> Filters.fromJson(json4));
        assertEquals(MSG_MAX_LENGTH, e.getMessage());
    }

    @Test
    void validatesNamePresenceOnConstruction() {
        DashboardFilter a = namedIncludes("");

        Throwable e = assertThrows(FilterValidationException.class, () -> new Filters(Collections.singletonList(a)));
        assertEquals(MSG_MISSING_NAME, e.getMessage());

        DashboardFilter b = namedIncludes(" ");

        e = assertThrows(FilterValidationException.class, () -> new Filters(Collections.singletonList(b)));
        assertEquals(MSG_MISSING_NAME, e.getMessage());

        DashboardFilter c = namedIncludes(null);

        e = assertThrows(FilterValidationException.class, () -> new Filters(Collections.singletonList(c)));
        assertEquals(MSG_MISSING_NAME, e.getMessage());
    }

    @Test
    void validatesNamePresenceOnDeserialize() {
        final String json = "{ \"filters\": [" +
                "  {\"name\": \"\", \"type\": \"whitelist\", \"pipelines\": []}" +
                "] }";
        Throwable e = assertThrows(FilterValidationException.class, () -> Filters.fromJson(json));
        assertEquals(MSG_MISSING_NAME, e.getMessage());

        final String json1 = "{ \"filters\": [" +
                "  {\"name\": \" \", \"type\": \"whitelist\", \"pipelines\": []}" +
                "] }";
        e = assertThrows(FilterValidationException.class, () -> Filters.fromJson(json1));
        assertEquals(MSG_MISSING_NAME, e.getMessage());

        final String json2 = "{ \"filters\": [" +
                "  {\"type\": \"whitelist\", \"pipelines\": []}" +
                "] }";
        e = assertThrows(FilterValidationException.class, () -> Filters.fromJson(json2));
        assertEquals(MSG_MISSING_NAME, e.getMessage());
    }

    @Test
    void validatesDuplicateNamesOnConstruction() {
        DashboardFilter a = namedIncludes("one");
        DashboardFilter b = namedExcludes("one");

        Throwable e = assertThrows(FilterValidationException.class, () -> new Filters(Arrays.asList(a, b)));
        assertEquals("Duplicate filter name: one", e.getMessage());
    }

    @Test
    void validatesDuplicateNamesOnDeserialize() {
        String json = "{ \"filters\": [" +
                "  {\"name\": \"one\", \"type\": \"whitelist\", \"pipelines\": [], \"state\": []}," +
                "  {\"name\": \"one\", \"type\": \"whitelist\", \"pipelines\": [], \"state\": []}" +
                "] }";
        Throwable e = assertThrows(FilterValidationException.class, () -> Filters.fromJson(json));
        assertEquals("Duplicate filter name: one", e.getMessage());
    }

    @Test
    void validatesStateOnDeserialize() {
        String json = "{ \"filters\": [" +
                "  {\"name\": \"one\", \"type\": \"whitelist\", \"pipelines\": [], \"state\": [\"pi\", \"failing\"]}," +
                "  {\"name\": \"default\", \"type\": \"whitelist\", \"pipelines\": [], \"state\": []}" +
                "] }";
        Throwable e = assertThrows(FilterValidationException.class, () -> Filters.fromJson(json));
        assertEquals(MSG_INVALID_STATES, e.getMessage());
    }

    @Test
    void validatesPresenceOfAtLeastOneFilterOnConstruction() {
        assertDoesNotThrow(() -> new Filters(Collections.singletonList(namedIncludes("foo", "p1"))));

        Throwable e = assertThrows(FilterValidationException.class, () -> new Filters(Collections.emptyList()));
        assertEquals(MSG_NO_DEFAULT_FILTER, e.getMessage());
    }

    @Test
    void validatesPresenceOfAtLeastOneFilterOnDeserialize() {
        assertDoesNotThrow(() -> Filters.fromJson("{ \"filters\": [{\"name\": \"foo\", \"state\":[], \"pipelines\":[], \"type\": \"whitelist\"}] }"));

        Throwable e = assertThrows(FilterValidationException.class, () -> Filters.fromJson("{ \"filters\": [] }"));
        assertEquals(MSG_NO_DEFAULT_FILTER, e.getMessage());
    }

    @Test
    void fromJson() {
        String json = "{ \"filters\": [{\"name\": \"Default\", \"type\": \"whitelist\", \"pipelines\": [\"p1\"], \"state\": []}] }";
        final Filters filters = Filters.fromJson(json);
        assertEquals(1, filters.filters().size());
        final DashboardFilter first = filters.filters().get(0);
        assertEquals(first.name(), DEFAULT_NAME);
        assertTrue(first instanceof IncludesFilter);
        assertEquals(1, ((IncludesFilter) first).pipelines().size());
        assertTrue(((IncludesFilter) first).pipelines().contains(new CaseInsensitiveString("p1")));
    }

    @Test
    void toJson() {
        List<DashboardFilter> views = new ArrayList<>();
        views.add(WILDCARD_FILTER);
        views.add(namedExcludes("Cool Pipelines", "Pipely McPipe"));
        final Filters filters = new Filters(views);

        assertEquals("{\"filters\":[" +
                "{\"name\":\"" + DEFAULT_NAME + "\",\"state\":[],\"pipelines\":[],\"type\":\"blacklist\"}," +
                "{\"name\":\"Cool Pipelines\",\"state\":[],\"pipelines\":[\"Pipely McPipe\"],\"type\":\"blacklist\"}" +
                "]}", Filters.toJson(filters));
    }

    @Test
    void equalsIsStructuralEquality() {
        final Filters a = Filters.single(excludes("p1", "p2"));
        final Filters b = Filters.single(excludes("p1", "p2"));
        final Filters c = Filters.single(excludes("p1", "p3"));

        assertEquals(a, b);
        assertNotEquals(a, c);

        final Filters d = Filters.single(includes("p1", "p2"));
        final Filters e = Filters.single(includes("p1", "p2"));
        final Filters f = Filters.single(includes("p1", "p3"));

        assertEquals(d, e);
        assertNotEquals(d, f);

        assertNotEquals(a, d);

        assertEquals(Filters.defaults(), Filters.single(excludes()));
    }


    private DashboardFilter namedIncludes(String name, String... pipelines) {
        return new IncludesFilter(name, CaseInsensitiveString.list(pipelines), new HashSet<>());
    }

    private DashboardFilter includes(String... pipelines) {
        return namedIncludes(DEFAULT_NAME, pipelines);
    }

    private DashboardFilter namedExcludes(String name, String... pipelines) {
        return new ExcludesFilter(name, CaseInsensitiveString.list(pipelines), new HashSet<>());
    }

    private DashboardFilter excludes(String... pipelines) {
        return namedExcludes(DEFAULT_NAME, pipelines);
    }
}