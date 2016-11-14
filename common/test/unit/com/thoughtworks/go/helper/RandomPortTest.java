/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.helper;

import com.thoughtworks.go.util.LogFixture;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;

import org.apache.log4j.Level;
import org.junit.After;

import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;

public class RandomPortTest {

    @Test
    public void shouldOpenRandomPort() {
        int port = RandomPort.find("foo");
        assertThat(port, is(not(RandomPort.find("foo"))));
    }

    @Test
    public void shouldLogPortsAllocated() {
        try (LogFixture logFixture = new LogFixture(RandomPort.class, Level.DEBUG)) {
            int port = RandomPort.find("foo");
            assertThat(logFixture.getLog(), containsString("RandomPort: Allocating port " + port + " for 'foo'"));
        }
    }

}
