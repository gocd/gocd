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

package com.thoughtworks.studios.shine.cruise.stage.feeds;

import java.util.ArrayList;
import java.util.List;

import com.thoughtworks.go.domain.feed.FeedEntry;
import com.thoughtworks.go.domain.feed.stage.StageFeedEntry;
import com.thoughtworks.go.server.dao.FeedModifier;
import com.thoughtworks.go.server.dao.StageDao;
import com.thoughtworks.go.server.service.PipelineInstanceLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class StageAtomFeedsReader {

    public final static int PAGE_SIZE = 100;

    private StageDao stageDao;

    @Autowired
    public StageAtomFeedsReader(StageDao stageDao) {
        this.stageDao = stageDao;
    }

    public void readFromLatest(StageFeedHandler stageFeedHandler, final PipelineInstanceLoader pipelineInstanceLoader) {

        List<FeedEntry> stageFeeds = lastPageOfFeeds();

        while (!stageFeeds.isEmpty()) {
            for (FeedEntry feedEntry : stageFeeds) {
                if(stageFeedHandler.shouldStopHandling((StageFeedEntry) feedEntry)) {
                    return;
                };
                stageFeedHandler.handle((StageFeedEntry) feedEntry, pipelineInstanceLoader);
            }
            stageFeeds = pageBefore(lastFeed(stageFeeds).getId());
        }

    }

    private FeedEntry lastFeed(List<FeedEntry> stageFeeds) {
        return stageFeeds.get(stageFeeds.size() - 1);
    }

    private List<FeedEntry> pageBefore(long stageId) {
        return new ArrayList<>(stageDao.findAllCompletedStages(FeedModifier.Before, stageId, PAGE_SIZE));
    }

    private List<FeedEntry> lastPageOfFeeds() {
        return new ArrayList<>(stageDao.findAllCompletedStages(FeedModifier.Latest, -1, PAGE_SIZE));
    }
}