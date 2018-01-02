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

package com.thoughtworks.studios.shine.cruise;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.thoughtworks.go.domain.StageFinder;
import com.thoughtworks.go.domain.StageIdentifier;
import com.thoughtworks.go.domain.StageResult;
import com.thoughtworks.go.domain.feed.stage.StageFeedEntry;
import com.thoughtworks.go.server.dao.FeedModifier;
import com.thoughtworks.go.server.dao.StageDao;
import com.thoughtworks.go.server.service.PipelineInstanceLoader;
import com.thoughtworks.studios.shine.cruise.stage.feeds.StageAtomFeedsReader;
import com.thoughtworks.studios.shine.cruise.stage.feeds.StageFeedHandler;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StageAtomFeedsReaderTest {
    private StubStageFeedHandler stageFeedHandler;
    private StageDao stageDao;
    private PipelineInstanceLoader pipelineInstanceLoader;

    @Before
    public void setup() {
        this.stageFeedHandler = new StubStageFeedHandler();
        this.stageDao = mock(StageDao.class);
        pipelineInstanceLoader = mock(PipelineInstanceLoader.class);
    }


    private StageFeedEntry feedEntry(int stageId) {
        return new StageFeedEntry(stageId, 1, new StageIdentifier("RPNCaculator", stageId * 10, "test", "1"), stageId * 100, new Date(), StageResult.Passed);
    }


    @Test
    public void canGetAllStageUrlsInAStagesFeed() throws Exception {

        when(stageDao.findAllCompletedStages(FeedModifier.Latest, -1, StageAtomFeedsReader.PAGE_SIZE)).thenReturn(Arrays.asList(
                feedEntry(2),
                feedEntry(1)
        ));
        new StageAtomFeedsReader(stageDao).readFromLatest(stageFeedHandler, pipelineInstanceLoader);

        List<StageFeedEntry> list = stageFeedHandler.handledFeeds();
        assertEquals(2, list.size());
        assertEquals(2, list.get(0).getId());
        assertEquals(1, list.get(1).getId());
    }


    @Test
    public void canPaginateToGetStages() throws Exception {

        when(stageDao.findAllCompletedStages(FeedModifier.Latest, -1, StageAtomFeedsReader.PAGE_SIZE)).thenReturn(Arrays.asList(
                feedEntry(4),
                feedEntry(3)
        ));

        when(stageDao.findAllCompletedStages(FeedModifier.Before, 3, StageAtomFeedsReader.PAGE_SIZE)).thenReturn(Arrays.asList(
                feedEntry(2),
                feedEntry(1)
        ));

        new StageAtomFeedsReader(stageDao).readFromLatest(stageFeedHandler, pipelineInstanceLoader);

        List<StageFeedEntry> list = stageFeedHandler.handledFeeds();

        assertEquals(4, list.size());
        assertEquals(4, list.get(0).getId());
        assertEquals(3, list.get(1).getId());
        assertEquals(2, list.get(2).getId());
        assertEquals(1, list.get(3).getId());
    }

    @Test
    public void checkWeFollowPagingUntilWeHitADuplicate() throws Exception {
        StageFeedEntry entry1 = feedEntry(1);
        StageFeedEntry entry2 = feedEntry(2);
        StageFeedEntry entry3 = feedEntry(3);
        StageFeedEntry entry4 = feedEntry(4);
        when(stageDao.findAllCompletedStages(FeedModifier.Latest, -1, StageAtomFeedsReader.PAGE_SIZE)).thenReturn(Arrays.asList( entry4));
        when(stageDao.findAllCompletedStages(FeedModifier.Before, 4, StageAtomFeedsReader.PAGE_SIZE)).thenReturn(Arrays.asList(entry3, entry2, entry1));

        stageFeedHandler.previousHandled(entry1);
        stageFeedHandler.previousHandled(entry2);
        new StageAtomFeedsReader(stageDao).readFromLatest(stageFeedHandler, pipelineInstanceLoader);

        List<StageFeedEntry> list = stageFeedHandler.handledFeeds();

        assertEquals(2, list.size());
        assertEquals(4, list.get(0).getId());
        assertEquals(3, list.get(1).getId());
    }

    private class StubStageFeedHandler implements StageFeedHandler {
        private ArrayList<StageFeedEntry> handled;
        private ArrayList<StageFeedEntry> previousHandled;

        private StubStageFeedHandler() {
            this.handled = new ArrayList<>();
            this.previousHandled = new ArrayList<>();
        }

        public void handle(StageFeedEntry stageFeed, final PipelineInstanceLoader pipelineInstanceLoader) {
            handled.add(stageFeed);
        }

        public boolean shouldStopHandling(StageFeedEntry stageFeed) {
            return previousHandled.contains(stageFeed) || handled.contains(stageFeed);
        }

        public List<StageFeedEntry> handledFeeds() {
            return handled;
        }

        public void previousHandled(StageFeedEntry feedEntry) {
            previousHandled.add(feedEntry);
        }
    }
}
