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

package com.thoughtworks.go.config.elastic;

import org.junit.Test;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

public class ElasticProfilesTest {

    @Test
    public void shouldFindProfileById() throws Exception {
        assertThat(new ElasticProfiles().find("foo"), is(nullValue()));
        ElasticProfile profile = new ElasticProfile("foo", "docker");
        assertThat(new ElasticProfiles(profile).find("foo"), is(profile));

    }

    @Test
    public void returnsACopyOfProfile() throws Exception {
        ElasticProfile profile = new ElasticProfile("foo", "docker");
        assertThat(new ElasticProfiles(profile).find("foo"), is(not(sameInstance(profile))));
    }
}
