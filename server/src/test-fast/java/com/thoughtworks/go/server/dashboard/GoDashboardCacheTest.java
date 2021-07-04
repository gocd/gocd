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
package com.thoughtworks.go.server.dashboard;

import com.thoughtworks.go.config.CaseInsensitiveString;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.thoughtworks.go.server.dashboard.GoDashboardPipelineMother.pipeline;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

public class GoDashboardCacheTest {
    private GoDashboardCache cache;

    @BeforeEach
    public void setUp() throws Exception {
        cache = new GoDashboardCache(mock(TimeStampBasedCounter.class));
    }

    @Test
    public void shouldNotBeAbleToGetAPipelineWhichDoesNotExist() {
        assertNull(cache.allEntries().find(cis("pipeline1")));
    }

    @Test
    public void shouldBeAbleToPutAndGetAPipeline() {
        GoDashboardPipeline expectedPipeline = pipeline("pipeline1");

        cache.put(expectedPipeline);
        GoDashboardPipeline actualPipeline = cache.allEntries().find(cis("pipeline1"));

        assertThat(expectedPipeline, is(sameInstance(actualPipeline)));
    }

    @Test
    public void shouldBeAbleToReplaceAnItemInCache() {
        GoDashboardPipeline somePipelineWhichWillBeReplaced = pipeline("pipeline1");
        GoDashboardPipeline expectedPipeline = pipeline("pipeline1");

        cache.put(somePipelineWhichWillBeReplaced);
        cache.put(expectedPipeline);
        GoDashboardPipeline actualPipeline = cache.allEntries().find(cis("pipeline1"));

        assertThat(expectedPipeline, is(sameInstance(actualPipeline)));
    }

    @Test
    public void shouldBeAbleToClearExistingCacheAndReplaceAllItemsInIt() {
        GoDashboardPipeline pipeline1 = pipeline("pipeline1");
        GoDashboardPipeline pipeline2 = pipeline("pipeline2");
        GoDashboardPipeline pipeline3 = pipeline("pipeline3");
        GoDashboardPipeline pipeline4 = pipeline("pipeline4", "group1");
        GoDashboardPipeline newPipeline4 = pipeline("pipeline4", "group2");
        GoDashboardPipeline pipeline5 = pipeline("pipeline5");

        cache.put(pipeline1);
        cache.put(pipeline2);
        cache.put(pipeline3);
        cache.put(pipeline4);

        cache.replaceAllEntriesInCacheWith(asList(pipeline3, newPipeline4, pipeline5));

        assertThat(cache.allEntries().find(cis("pipeline1")), is(nullValue()));
        assertThat(cache.allEntries().find(cis("pipeline2")), is(nullValue()));
        assertThat(cache.allEntries().find(cis("pipeline3")), is(sameInstance(pipeline3)));
        assertThat(cache.allEntries().find(cis("pipeline4")), is(sameInstance(newPipeline4)));
        assertThat(cache.allEntries().find(cis("pipeline5")), is(sameInstance(pipeline5)));
    }

    private CaseInsensitiveString cis(String value) {
        return new CaseInsensitiveString(value);
    }
}
