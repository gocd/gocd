/*
 * Copyright 2021 ThoughtWorks, Inc.
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

import java.util.Arrays;

import static com.thoughtworks.go.api.ApiVersion.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

class RouteTogglesTest {
    @Mock
    FeatureToggleService features;

    @BeforeEach
    void setUp() {
        initMocks(this);
    }

    @Test
    void matches() {
        final RouteToggles rts = new RouteToggles(Arrays.asList(
                desc("/foo/bar", v5, "one"),
                desc("/foo/baz", v5, "two"),
                desc("/ping", v2, "ping")
        ), features);

        assertTrue(rts.matches(entry("/foo/bar", v5)));
        assertTrue(rts.matches(entry("/foo/baz", v5)));

        // will not implicitly match parents
        assertFalse(rts.matches(entry("/foo", v5)));

        // version must match
        assertFalse(rts.matches(entry("/foo/baz", v2)));

        // ignores anything else
        assertFalse(rts.matches(entry("/anything", v1)));

        // matches sub paths
        assertTrue(rts.matches(entry("/ping", v2)));
        assertTrue(rts.matches(entry("/ping/pong", v2)));
        assertTrue(rts.matches(entry("/ping/foo", v2)));

        // must be the path or a child, not just prefix
        assertFalse(rts.matches(entry("/ping-a-ling", v2)));
    }

    @Test
    void matchesCanHandleExactPaths() {
        final RouteToggles rts = new RouteToggles(Arrays.asList(
                exact("/foo", v5, "a"),
                exact("/foo/bar", v5, "b"),
                desc("/foo/bar/baz/quu", v5, "c")
        ), features);

        // exact matches
        assertTrue(rts.matches(entry("/foo", v5)));
        assertTrue(rts.matches(entry("/foo/", v5)));
        assertTrue(rts.matches(entry("/foo/bar", v5)));
        assertTrue(rts.matches(entry("/foo/bar/", v5)));

        // baseline failures to show difference from descendant matching
        assertFalse(rts.matches(entry("/foo/bar/any", v5)));
        assertFalse(rts.matches(entry("/foo/any", v5)));

        // won't match child paths unless explicitly added
        assertFalse(rts.matches(entry("/foo/bar/baz", v5)));
        assertFalse(rts.matches(entry("/foo/bar/baz/", v5)));
        assertFalse(rts.matches(entry("/foo/bar/baz/any", v5)));

        // does not interfere with descendant matchers of sub paths
        assertTrue(rts.matches(entry("/foo/bar/baz/quu", v5)));
        assertTrue(rts.matches(entry("/foo/bar/baz/quu/", v5)));
        assertTrue(rts.matches(entry("/foo/bar/baz/quu/whatever", v5)));
    }

    @Test
    void isToggledOn() {
        when(features.isToggleOn("one")).thenReturn(true);
        when(features.isToggleOn("two")).thenReturn(false);
        when(features.isToggleOn("three")).thenReturn(true);

        RouteToggles rts = new RouteToggles(Arrays.asList(
                desc("/foo/bar", v5, "one"),
                desc("/foo/baz", v5, "two")
        ), features);

        assertTrue(rts.isToggledOn(entry("/foo/bar", v5)));
        assertFalse(rts.isToggledOn(entry("/foo/baz", v5)));

        // two rules that match are AND'ed together in case you need a compound condition
        rts = new RouteToggles(Arrays.asList(
                desc("/foo/bar", v5, "one"),
                desc("/foo/bar", v5, "two")
        ), features);

        assertFalse(rts.isToggledOn(entry("/foo/bar", v5)));

        // fine when both toggle keys are true, of course
        rts = new RouteToggles(Arrays.asList(
                desc("/foo/bar", v5, "one"),
                desc("/foo/bar", v5, "three")
        ), features);

        assertTrue(rts.isToggledOn(entry("/foo/bar", v5)));

        // using exact and desc together allow fine-grained toggling control
        rts = new RouteToggles(Arrays.asList(
                desc("/foo", v5, "one"),
                exact("/foo/bar/baz", v5, "two")
        ), features);

        assertTrue(rts.isToggledOn(entry("/foo", v5)));
        assertTrue(rts.isToggledOn(entry("/foo/bar", v5)));
        assertTrue(rts.isToggledOn(entry("/foo/blah", v5)));

        assertFalse(rts.isToggledOn(entry("/foo/bar/baz", v5)));
        assertFalse(rts.isToggledOn(entry("/foo/bar/baz/", v5)));

        assertTrue(rts.isToggledOn(entry("/foo/bar/baz/anything", v5)));
        assertTrue(rts.isToggledOn(entry("/foo/bar/baz/anything/else", v5)));
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