/*
 * Copyright Thoughtworks, Inc.
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

import java.util.List;

import static com.thoughtworks.go.server.dashboard.GoDashboardPipelineMother.pipeline;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

public class GoDashboardCacheTest {
    private GoDashboardCache cache;

    @BeforeEach
    public void setUp() {
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

        assertThat(expectedPipeline).isSameAs(actualPipeline);
    }

    @Test
    public void shouldBeAbleToReplaceAnItemInCache() {
        GoDashboardPipeline somePipelineWhichWillBeReplaced = pipeline("pipeline1");
        GoDashboardPipeline expectedPipeline = pipeline("pipeline1");

        cache.put(somePipelineWhichWillBeReplaced);
        cache.put(expectedPipeline);
        GoDashboardPipeline actualPipeline = cache.allEntries().find(cis("pipeline1"));

        assertThat(expectedPipeline).isSameAs(actualPipeline);
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

        cache.replaceAllEntriesInCacheWith(List.of(pipeline3, newPipeline4, pipeline5));

        assertThat(cache.allEntries().find(cis("pipeline1"))).isNull();
        assertThat(cache.allEntries().find(cis("pipeline2"))).isNull();
        assertThat(cache.allEntries().find(cis("pipeline3"))).isSameAs(pipeline3);
        assertThat(cache.allEntries().find(cis("pipeline4"))).isSameAs(newPipeline4);
        assertThat(cache.allEntries().find(cis("pipeline5"))).isSameAs(pipeline5);
    }

    private CaseInsensitiveString cis(String value) {
        return new CaseInsensitiveString(value);
    }
}
