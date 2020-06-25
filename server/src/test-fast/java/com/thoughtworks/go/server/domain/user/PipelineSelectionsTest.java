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
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;

import static com.thoughtworks.go.helper.PipelineConfigMother.createGroup;
import static com.thoughtworks.go.helper.PipelineConfigMother.pipelineConfig;
import static com.thoughtworks.go.server.domain.user.DashboardFilter.DEFAULT_NAME;
import static java.lang.String.format;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class PipelineSelectionsTest {
    @Test
    public void etagBasedOnFilterContent() {
        PipelineSelections ps = PipelineSelectionsHelper.with(Arrays.asList("p1", "p2"));
        assertEquals("148c3fde87ad2ac597262975fece58e64036a79ced54351a700232ac6fda90b0", ps.etag());

        ps.update(Filters.defaults(), null, null);
        assertEquals("2356e5d4241f22987d8f8cb8920913fda237385b423bd7eec7e97b2a9eb1be1a", ps.etag());

        ps.update(Filters.single(new IncludesFilter(DEFAULT_NAME, CaseInsensitiveString.list("p1"), new HashSet<>())), null, null);
        assertEquals("9ffa0b91a0100dac243e4a4f47303057b888e1e0b25cdb26aaf05c019f8e71ee", ps.etag());

        ps.setFilters(format("{\"filters\": [{\"name\": \"%s\", \"type\": \"blacklist\", \"state\": [], \"pipelines\": [\"foobar\"]}]}", DEFAULT_NAME));
        assertEquals("224c1949538a5a278c061a99c40797bf3849d13fca7f3aeb249171851f2a384e", ps.etag());
    }
}
