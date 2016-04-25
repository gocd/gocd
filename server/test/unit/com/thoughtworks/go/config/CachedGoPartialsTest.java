/*
 * Copyright 2016 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thoughtworks.go.config;

import com.thoughtworks.go.config.remote.PartialConfig;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class CachedGoPartialsTest {

    private CachedGoPartials partials;
    private PartialConfig part1;
    private PartialConfig part2;

    @Before
    public void setUp() throws Exception {
        partials = new CachedGoPartials();
        part1 = new PartialConfig();
        part2 = new PartialConfig();
        partials.addOrUpdate("fingerprint1", part1);
        partials.addOrUpdate("fingerprint2", part2);

    }

    @Test
    public void shouldMarkAPartialAsValid() {
        partials.markAsValid("fingerprint1", part1);
        assertThat(partials.lastValidPartials().contains(part1), is(true));
        assertThat(partials.lastValidPartials().contains(part2), is(false));
    }

    @Test
    public void shouldMarkAllKnownAsValid() {
        partials.markAllKnownAsValid();
        assertThat(partials.lastValidPartials().contains(part1), is(true));
        assertThat(partials.lastValidPartials().contains(part2), is(true));
    }

    @Test
    public void shouldRemoveValid() {
        partials.markAllKnownAsValid();
        partials.removeValid("fingerprint1");
        assertThat(partials.lastValidPartials().contains(part1), is(false));
        assertThat(partials.lastValidPartials().contains(part2), is(true));
    }

    @Test
    public void shouldRemoveKnown() {
        partials.removeKnown("fingerprint1");
        assertThat(partials.lastKnownPartials().contains(part1), is(false));
        assertThat(partials.lastKnownPartials().contains(part2), is(true));
    }
}