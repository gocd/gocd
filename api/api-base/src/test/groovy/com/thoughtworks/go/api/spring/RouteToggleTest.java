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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import spark.Route;
import spark.route.HttpMethod;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

class RouteToggleTest {
    @Mock
    FeatureToggleService features;

    @BeforeEach
    void setup() {
        initMocks(this);
    }

    @Test
    void matchesDescending() {
        final RouteToggle r = desc("/foo/", ApiVersion.v3, "me");
        assertTrue(r.matches(entry("/foo", ApiVersion.v3)));
        assertTrue(r.matches(entry("/foo/", ApiVersion.v3)));
        assertTrue(r.matches(entry("/foo/bar/baz", ApiVersion.v3)));

        // versions don't match
        assertFalse(r.matches(entry("/foo", ApiVersion.v1)));
        assertFalse(r.matches(entry("/foo/", ApiVersion.v1)));
        assertFalse(r.matches(entry("/foo/bar", ApiVersion.v1)));

        // not path or sub path
        assertFalse(r.matches(entry("/foobar", ApiVersion.v3)));
    }

    @Test
    void matchesExact() {
        final RouteToggle r = exact("/foo/", ApiVersion.v3, "me");
        assertTrue(r.matches(entry("/foo", ApiVersion.v3)));
        assertTrue(r.matches(entry("/foo/", ApiVersion.v3)));
        assertFalse(r.matches(entry("/foo/bar/baz", ApiVersion.v3)));

        // versions don't match
        assertFalse(r.matches(entry("/foo", ApiVersion.v1)));
        assertFalse(r.matches(entry("/foo/", ApiVersion.v1)));

        // not path or sub path
        assertFalse(r.matches(entry("/foobar", ApiVersion.v3)));
    }

    @Test
    void neverMatchesParents() {
        RouteToggle r = desc("/foo/bar", ApiVersion.v3, "me");
        assertFalse(r.matches(entry("/foo", ApiVersion.v3)));
        assertFalse(r.matches(entry("/foo/", ApiVersion.v3)));

        assertTrue(r.matches(entry("/foo/bar", ApiVersion.v3)));
        assertTrue(r.matches(entry("/foo/bar/", ApiVersion.v3)));
        assertTrue(r.matches(entry("/foo/bar/baz", ApiVersion.v3)));

        r = exact("/foo/bar", ApiVersion.v3, "me");
        assertFalse(r.matches(entry("/foo", ApiVersion.v3)));
        assertFalse(r.matches(entry("/foo/", ApiVersion.v3)));

        assertTrue(r.matches(entry("/foo/bar", ApiVersion.v3)));
        assertTrue(r.matches(entry("/foo/bar/", ApiVersion.v3)));
    }

    @Test
    void isToggleOn() {
        final RouteToggle r = desc("/foo", ApiVersion.v3, "me");

        assertFalse(r.isToggleOn(features));

        when(features.isToggleOn("me")).thenReturn(true);
        assertTrue(r.isToggleOn(features));

        verify(features, atLeastOnce()).isToggleOn("me");
    }

    @Test
    void generatesToggleNameWhenNotSpecified() {
        final RouteToggle r = desc("/foo", ApiVersion.v3, "");

        assertFalse(r.isToggleOn(features));

        when(features.isToggleOn("v3__foo")).thenReturn(true);
        assertTrue(r.isToggleOn(features));

        verify(features, atLeastOnce()).isToggleOn("v3__foo");
    }

    private RouteToggle desc(String path, ApiVersion v, String name) {
        return new RouteToggle(path, v, name, true);
    }

    private RouteToggle exact(String path, ApiVersion v, String name) {
        return new RouteToggle(path, v, name, false);
    }

    private RouteEntry entry(String path, ApiVersion v) {
        return new RouteEntry(HttpMethod.get, path, v.mimeType(), (Route) (rq, rs) -> v.name());
    }
}