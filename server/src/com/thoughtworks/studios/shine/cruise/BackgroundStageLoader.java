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

package com.thoughtworks.studios.shine.cruise;

import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.StageIdentifier;
import com.thoughtworks.go.domain.feed.stage.StageFeedEntry;
import com.thoughtworks.go.server.domain.StageStatusListener;
import com.thoughtworks.go.server.service.PipelineInstanceLoader;
import com.thoughtworks.go.server.service.StageService;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.studios.shine.cruise.stage.details.StageResourceImporter;
import com.thoughtworks.studios.shine.cruise.stage.details.StageStorage;
import com.thoughtworks.studios.shine.cruise.stage.feeds.StageAtomFeedsReader;
import com.thoughtworks.studios.shine.cruise.stage.feeds.StageFeedHandler;
import com.thoughtworks.studios.shine.semweb.grddl.XSLTTransformerRegistry;
import com.thoughtworks.studios.shine.semweb.sesame.InMemoryTempGraphFactory;
import com.thoughtworks.studios.shine.time.Clock;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BackgroundStageLoader implements StageFeedHandler {
    private final static Logger LOGGER = Logger.getLogger(BackgroundStageLoader.class);

    private StageAtomFeedsReader stageFeedsReader;
    private StageResourceImporter stageResourceImporter;
    private StageStorage stageStorage;
    private final PipelineInstanceLoader pipelineInstanceLoader;
    private final StageService stageService;
    private final XSLTTransformerRegistry transformerRegistry;

    @Autowired
    public BackgroundStageLoader(StageAtomFeedsReader stageFeedsReader, StageResourceImporter stageResourceImporter, StageStorage stageStorage, PipelineInstanceLoader pipelineInstanceLoader,
                                 StageService stageService, SystemEnvironment systemEnvironment) {
        this.stageFeedsReader = stageFeedsReader;
        this.stageResourceImporter = stageResourceImporter;
        this.stageStorage = stageStorage;
        this.pipelineInstanceLoader = pipelineInstanceLoader;
        this.stageService = stageService;
        transformerRegistry = new XSLTTransformerRegistry();
        this.stageService.addStageStatusListener(new BackgroundStageLoaderStageStatusListener(this, systemEnvironment));
    }

    public void load() {
        stageFeedsReader.readFromLatest(this, pipelineInstanceLoader);
    }

    public void handle(StageFeedEntry feedEntry, final PipelineInstanceLoader pipelineInstanceLoader) {
        StageIdentifier stageIdentifier = feedEntry.getStageIdentifier();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("handling stage:<" + stageIdentifier + ">...");
        }
        try {
            stageStorage.save(stageResourceImporter.load(stageIdentifier, new InMemoryTempGraphFactory(), transformerRegistry));
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("importing stage:<" + stageIdentifier + "> finished.");
            }
        } catch (GoIntegrationException e) {
            LOGGER.error("Can not import stage <" + stageIdentifier + ">, will skip to next...", e);
        }
    }

    public boolean shouldStopHandling(StageFeedEntry feedEntry) {
        return stageStorage.isStageStored(feedEntry.getStageIdentifier()) || stageUpdatedMoreThanOneWeekAgo(feedEntry);
    }

    private boolean stageUpdatedMoreThanOneWeekAgo(StageFeedEntry feedEntry) {
        return feedEntry.getUpdatedDate().before(Clock.nowUTC().minusDays(7).toDate());
    }
}


class BackgroundStageLoaderStageStatusListener implements StageStatusListener {
    private final BackgroundStageLoader backgroundStageLoader;
    private final SystemEnvironment systemEnvironment;

    public BackgroundStageLoaderStageStatusListener(BackgroundStageLoader backgroundStageLoader, SystemEnvironment systemEnvironment) {
        this.backgroundStageLoader = backgroundStageLoader;
        this.systemEnvironment = systemEnvironment;
    }

    @Override
    public void stageStatusChanged(Stage stage) {

        if (systemEnvironment.isShineEnabled()) {
            if (stage.isCompleted()) {
                backgroundStageLoader.load();
            }
        }
    }
}
