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
package com.thoughtworks.go.fixture;

import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.config.StageConfig;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.dao.StageDao;
import com.thoughtworks.go.server.service.ScheduleService;

public class SchedulerFixture {
    private DatabaseAccessHelper dbHelper;
    private ScheduleService scheduleService;
    private StageDao stageDao;

    public SchedulerFixture(DatabaseAccessHelper dbHelper, StageDao stageDao,
                            ScheduleService scheduleService) {
        this.stageDao = stageDao;
        this.dbHelper = dbHelper;
        this.scheduleService = scheduleService;
    }

    public void rerunAndPassStage(Pipeline pipeline, StageConfig stageConfig) {
        scheduleService.rerunStage(pipeline, stageConfig, "anyone");
        Stage stage = stageDao.mostRecentWithBuilds(pipeline.getName(), stageConfig);
        dbHelper.passStage(stage);
    }
}
