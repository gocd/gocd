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
package com.thoughtworks.go.server.sweepers;

import com.thoughtworks.go.domain.PipelineIdentifier;
import com.thoughtworks.go.domain.PipelineTimelineEntry;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.StageIdentifier;
import com.thoughtworks.go.helper.StageMother;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.ui.ViewCacheKey;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

import static com.thoughtworks.go.util.DataStructureUtils.a;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
public class FailedBuildHistoryCacheSweeperTest {
    @Autowired private GoCache goCache;
    @Autowired private FailedBuildHistoryCacheSweeper sweeper;
    private ViewCacheKey key = new ViewCacheKey();
    private PipelineTimelineEntry newlyAddedEntry;
    private PipelineTimelineEntry entryBeforeNew;
    private PipelineTimelineEntry entryAfterNew;
    private TreeSet<PipelineTimelineEntry> timeline;

    @Before
    public void setUp() {
        HashMap<String, List<PipelineTimelineEntry.Revision>> modificationTimes = new HashMap<>();
        modificationTimes.put("hg", a(new PipelineTimelineEntry.Revision(new Date(), "123", "hg", 10)));
        newlyAddedEntry = new PipelineTimelineEntry("cruise", 100, 10, modificationTimes, 5.0);
        entryBeforeNew = new PipelineTimelineEntry("cruise", 98, 9, modificationTimes, 4.0);
        entryAfterNew = new PipelineTimelineEntry("cruise", 99, 11, modificationTimes, 6.0);
        timeline = new TreeSet<>();
        timeline.add(entryBeforeNew);
        timeline.add(entryAfterNew);
        timeline.add(newlyAddedEntry);
    }

    @Test
    public void shouldClearFbhForPipelinesAfterUpdatedPipeline() {
        PipelineIdentifier pipelineIdentifier = entryAfterNew.getPipelineLocator();
        Stage stage = StageMother.cancelledStage("dev", "rspec");
        stage.setIdentifier(new StageIdentifier(pipelineIdentifier, "dev", "2"));

        String pipelineKey = key.forFbhOfStagesUnderPipeline(pipelineIdentifier);
        String stageKey = key.forFailedBuildHistoryStage(stage, "html");
        goCache.put(pipelineKey, stageKey, "fragment");

        sweeper.added(newlyAddedEntry, timeline);

        assertThat(goCache.get(pipelineKey), is(nullValue()));
    }
}
