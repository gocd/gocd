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
package com.thoughtworks.go.fixture;

import com.thoughtworks.go.config.StageConfig;
import com.thoughtworks.go.domain.DefaultSchedulingContext;
import com.thoughtworks.go.domain.JobResult;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.service.InstanceFactory;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.TimeProvider;

import java.nio.file.Path;

import static com.thoughtworks.go.util.ExceptionUtils.bombIf;

public class PipelineWithMultipleStages extends PipelineWithTwoStages implements PreCondition {
    private int stagesSize;
    private String[] stageNames;

    public PipelineWithMultipleStages(int stagesSize, MaterialRepository materialRepository, final TransactionTemplate transactionTemplate, Path tempDir) {
        super(materialRepository, transactionTemplate, tempDir);
        bombIf(stagesSize < 2, "Illegal stagesSize: " + stagesSize + ", at lease 2");
        this.stagesSize = stagesSize;
        this.stageNames = new String[stagesSize];
        this.stageNames[0] = devStage;
        this.stageNames[1] = ftStage;
    }

    @Override
    public void onSetUp() throws Exception {
        super.onSetUp();
        for (int i = 2; i < stagesSize; i++) {
            stageNames[i] = "Stage" + (i + 1);
            configHelper.addStageToPipeline(pipelineName, stageNames[i], "unit");
        }
    }

    public String stageName(int index) {
        ensureValidIndex(index);
        return stageNames[index - 1];
    }

    public StageConfig stageConfig(int index) {
        ensureValidIndex(index);
        return pipelineConfig().get(index - 1);
    }

    public void configStageAsManualApprovalWithApprovedUsers(String stageName, String... users) {
        configHelper.addAuthorizedUserForStage(pipelineName, stageName, users);
    }

    @Override
    protected void scheduleAndCompleteFollowingStages(Pipeline pipeline, JobResult result) {
        for (int index = 2; index <= stagesSize; index++) {
            StageConfig stageConfig = stageConfig(index);
            Stage instance = new InstanceFactory().createStageInstance(stageConfig, new DefaultSchedulingContext("anyone"), "md5-test", new TimeProvider());
            instance.setOrderId(index);
            dbHelper.getStageDao().saveWithJobs(pipeline, instance);
            dbHelper.completeStage(instance, result);
        }
    }

    private void ensureValidIndex(int index) {
        bombIf(index < 1 || index > stagesSize, "Illegal index: " + index + ", valid range: 1->" + stagesSize);
    }
}
