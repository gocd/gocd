/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
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
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.domain.user;

import com.thoughtworks.go.config.CaseInsensitiveString;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;

import static com.thoughtworks.go.helper.PipelineConfigMother.createGroup;
import static com.thoughtworks.go.helper.PipelineConfigMother.pipelineConfig;
import static com.thoughtworks.go.server.domain.user.DashboardFilter.DEFAULT_NAME;
import static java.lang.String.format;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class PipelineSelectionsTest {
    @Test
    public void etagBasedOnFilterContent() {
        PipelineSelections ps = PipelineSelectionsHelper.with(Arrays.asList("p1", "p2"));
        assertEquals("148C3FDE87AD2AC597262975FECE58E64036A79CED54351A700232AC6FDA90B0", ps.etag());

        ps.update(Filters.defaults(), null, null);
        assertEquals("2356E5D4241F22987D8F8CB8920913FDA237385B423BD7EEC7E97B2A9EB1BE1A", ps.etag());

        ps.update(Filters.single(new WhitelistFilter(DEFAULT_NAME, CaseInsensitiveString.list("p1"), new HashSet<>())), null, null);
        assertEquals("9FFA0B91A0100DAC243E4A4F47303057B888E1E0B25CDB26AAF05C019F8E71EE", ps.etag());

        ps.setFilters(format("{\"filters\": [{\"name\": \"%s\", \"type\": \"blacklist\", \"state\": [], \"pipelines\": [\"foobar\"]}]}", DEFAULT_NAME));
        assertEquals("224C1949538A5A278C061A99C40797BF3849D13FCA7F3AEB249171851F2A384E", ps.etag());
    }

    @Test
    public void DEPRECATED_shouldIncludePipelineWhenGroupIsIncluded_whenBlacklistIsEnabled() {
        PipelineSelections pipelineSelections = PipelineSelectionsHelper.with(Arrays.asList("pipeline1"));
        assertThat(pipelineSelections.includesGroup(createGroup("group1", pipelineConfig("pipelineX"))), is(true));
        assertThat(pipelineSelections.includesGroup(createGroup("group2", pipelineConfig("pipeline2"), pipelineConfig("pipeline1"))), is(false));
        assertThat(pipelineSelections.includesPipeline(new CaseInsensitiveString("pipeline1")), is(false));
        assertThat(pipelineSelections.includesPipeline(new CaseInsensitiveString("pipeline2")), is(true));
    }

    @Test
    public void DEPRECATED_shouldIncludePipelinesCaseInsensitively_whenBlacklistIsEnabled() {
        PipelineSelections pipelineSelections = PipelineSelectionsHelper.with(Arrays.asList("pipeline1"));
        assertThat(pipelineSelections.includesPipeline(new CaseInsensitiveString("pipeline1")), is(false));
        assertThat(pipelineSelections.includesPipeline(new CaseInsensitiveString("Pipeline1")), is(false));
    }

    @Test
    public void DEPRECATED_shouldIncludePipelineWhenGroupIsIncluded_whenWhitelistIsEnabled() {
        PipelineSelections pipelineSelections = PipelineSelectionsHelper.with(Arrays.asList("pipeline1", "pipelineY"), null, null, false);
        assertThat(pipelineSelections.includesGroup(createGroup("group1", pipelineConfig("pipelineX"))), is(false));
        assertThat(pipelineSelections.includesGroup(createGroup("group2", pipelineConfig("pipeline2"), pipelineConfig("pipeline1"))), is(false));
        assertThat(pipelineSelections.includesGroup(createGroup("group3", pipelineConfig("pipeline2"), pipelineConfig("pipelineY"), pipelineConfig("pipeline1"))), is(false));
        assertThat(pipelineSelections.includesGroup(createGroup("group4", pipelineConfig("pipeline1"))), is(true));
        assertThat(pipelineSelections.includesGroup(createGroup("group5", pipelineConfig("pipeline1"), pipelineConfig("pipelineY"))), is(true));

        assertThat(pipelineSelections.includesPipeline(new CaseInsensitiveString("pipeline1")), is(true));
        assertThat(pipelineSelections.includesPipeline(new CaseInsensitiveString("pipeline2")), is(false));
    }
}
