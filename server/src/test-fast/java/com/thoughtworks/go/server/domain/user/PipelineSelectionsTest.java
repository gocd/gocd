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
        assertEquals("E8435DD87D7E62616157CF2F71A5FCB7DAE9224A6EBE4F47CF20D0583D5897C5", ps.etag());

        ps.update(Filters.defaults(), null, null);
        assertEquals("1269CFEF75B2DA5D73263B78BE4EA8EA9663104992327642B44F28A5C66E36F2", ps.etag());

        ps.update(Filters.single(new WhitelistFilter(DEFAULT_NAME, CaseInsensitiveString.list("p1"), null)), null, null);
        assertEquals("852D88B3C5D6A9CFC481DE35FB4A90A79E5A96A10A80947F7EFC8D345C10024E", ps.etag());

        ps.setFilters(format("{\"filters\": [{\"name\": \"%s\", \"type\": \"blacklist\", \"pipelines\": [\"foobar\"]}]}", DEFAULT_NAME));
        assertEquals("87CDD48CD9C281C6D5B8C28D4FD0F61E1F18D280D8B247B9CAC96161978D2580", ps.etag());
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
