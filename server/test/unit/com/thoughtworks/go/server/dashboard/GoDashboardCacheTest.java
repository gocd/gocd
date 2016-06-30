/*
 * Copyright 2017 ThoughtWorks, Inc.
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
import com.thoughtworks.go.config.security.Permissions;
import com.thoughtworks.go.config.security.users.Everyone;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineModel;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static com.thoughtworks.go.domain.PipelinePauseInfo.notPaused;
import static java.util.Arrays.asList;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;

public class GoDashboardCacheTest {

    private GoDashboardCache cache;

    @Before
    public void setUp() throws Exception {
        cache = new GoDashboardCache();
    }

    @Test
    public void shouldNotBeAbleToGetAPipelineWhichDoesNotExist() throws Exception {
        assertNull(cache.get(cis("pipeline1")));
    }

    @Test
    public void shouldBeAbleToPutAndGetAPipeline() throws Exception {
        GoDashboardPipeline expectedPipeline = pipeline("pipeline1");

        cache.put(expectedPipeline);
        GoDashboardPipeline actualPipeline = cache.get(cis("pipeline1"));

        assertThat(expectedPipeline, is(sameInstance(actualPipeline)));
    }

    @Test
    public void shouldBeAbleToReplaceAnItemInCache() throws Exception {
        GoDashboardPipeline somePipelineWhichWillBeReplaced = pipeline("pipeline1");
        GoDashboardPipeline expectedPipeline = pipeline("pipeline1");

        cache.put(somePipelineWhichWillBeReplaced);
        cache.put(expectedPipeline);
        GoDashboardPipeline actualPipeline = cache.get(cis("pipeline1"));

        assertThat(expectedPipeline, is(sameInstance(actualPipeline)));
    }

    @Test
    public void shouldBeAbleToClearExistingCacheAndReplaceAllItemsInIt() throws Exception {
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

        assertThat(cache.get(cis("pipeline1")), is(nullValue()));
        assertThat(cache.get(cis("pipeline2")), is(nullValue()));
        assertThat(cache.get(cis("pipeline3")), is(sameInstance(pipeline3)));
        assertThat(cache.get(cis("pipeline4")), is(sameInstance(newPipeline4)));
        assertThat(cache.get(cis("pipeline5")), is(sameInstance(pipeline5)));
    }

    @Test
    public void shouldProvideAnOrderedListOfAllItemsInCache() throws Exception {
        GoDashboardPipeline pipeline1 = pipeline("pipeline1");
        GoDashboardPipeline pipeline2 = pipeline("pipeline2");
        GoDashboardPipeline pipeline3 = pipeline("pipeline3");

        cache.replaceAllEntriesInCacheWith(asList(pipeline1, pipeline2, pipeline3));
        List<GoDashboardPipeline> allPipelines = cache.allEntriesInOrder();

        assertThat(allPipelines.size(), is(3));
        assertThat(allPipelines.get(0), is(sameInstance(pipeline1)));
        assertThat(allPipelines.get(1), is(sameInstance(pipeline2)));
        assertThat(allPipelines.get(2), is(sameInstance(pipeline3)));
    }

    @Test
    public void shouldContainChangedEntryInOrderedListAfterAPut() throws Exception {
        GoDashboardPipeline pipeline1 = pipeline("pipeline1");
        GoDashboardPipeline pipeline2 = pipeline("pipeline2", "group1");
        GoDashboardPipeline newPipeline2 = pipeline("pipeline2", "group2");
        GoDashboardPipeline pipeline3 = pipeline("pipeline3");

        cache.replaceAllEntriesInCacheWith(asList(pipeline1, pipeline2, pipeline3));
        List<GoDashboardPipeline> allPipelinesBeforePut = cache.allEntriesInOrder();
        assertThat(allPipelinesBeforePut.get(1), is(sameInstance(pipeline2)));

        cache.put(newPipeline2);
        List<GoDashboardPipeline> allPipelinesAfterPut = cache.allEntriesInOrder();

        assertThat(allPipelinesAfterPut.size(), is(3));
        assertThat(allPipelinesAfterPut.get(0), is(sameInstance(pipeline1)));
        assertThat(allPipelinesAfterPut.get(1), is(sameInstance(newPipeline2)));
        assertThat(allPipelinesAfterPut.get(2), is(sameInstance(pipeline3)));
    }

    private GoDashboardPipeline pipeline(String pipelineName) {
        return pipeline(pipelineName, "group1");
    }

    private GoDashboardPipeline pipeline(String pipelineName, String groupName) {
        return new GoDashboardPipeline(new PipelineModel(pipelineName, false, false, notPaused()), somePermissions(), groupName);
    }

    private Permissions somePermissions() {
        return new Permissions(Everyone.INSTANCE, Everyone.INSTANCE, Everyone.INSTANCE, Everyone.INSTANCE);
    }

    private CaseInsensitiveString cis(String value) {
        return new CaseInsensitiveString(value);
    }
}