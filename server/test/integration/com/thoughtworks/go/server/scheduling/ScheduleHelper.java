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

package com.thoughtworks.go.server.scheduling;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.materials.Materials;
import com.thoughtworks.go.domain.DefaultSchedulingContext;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.SchedulingContext;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.helper.MaterialsMother;
import com.thoughtworks.go.helper.ScheduleCheckMatcher;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.materials.MaterialDatabaseUpdater;
import com.thoughtworks.go.server.messaging.StubScheduleCheckCompletedListener;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.service.*;
import com.thoughtworks.go.server.service.result.ServerHealthStateOperationResult;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import com.thoughtworks.go.util.TimeProvider;
import com.thoughtworks.go.utils.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import java.util.HashMap;
import java.util.Map;

import static com.thoughtworks.go.utils.Assertions.assertAlwaysHappens;
import static com.thoughtworks.go.utils.Assertions.assertWillHappen;
import static com.thoughtworks.go.utils.Timeout.TEN_SECONDS;
import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.fail;

@Component
public class ScheduleHelper {
    private StubScheduleCheckCompletedListener scheduleCompleteListener;
    private PipelineScheduler pipelineScheduler;
    private final GoConfigService goConfigService;
    private PipelineService pipelineService;
    private MaterialRepository materialRepository;
    private final PipelineScheduleQueue pipelineScheduleQueue;
    private final TransactionTemplate transactionTemplate;
    static private long waitTime;
    private static final long ONE_SECOND = 1000;
    private MaterialDatabaseUpdater materialDatabaseUpdater;
    private InstanceFactory instanceFactory;

    @Autowired
    public ScheduleHelper(ScheduleCheckCompletedTopic scheduleCheckCompletedTopic, PipelineScheduler pipelineScheduler,
                          GoConfigService goConfigService, MaterialDatabaseUpdater materialDatabaseUpdater,
                          PipelineService pipelineService, MaterialRepository materialRepository,
                          PipelineScheduleQueue pipelineScheduleQueue, TransactionTemplate transactionTemplate, InstanceFactory instanceFactory) {
        this.pipelineScheduler = pipelineScheduler;
        this.goConfigService = goConfigService;
        this.materialDatabaseUpdater = materialDatabaseUpdater;
        this.pipelineService = pipelineService;
        this.materialRepository = materialRepository;
        this.pipelineScheduleQueue = pipelineScheduleQueue;
        this.transactionTemplate = transactionTemplate;
        this.instanceFactory = instanceFactory;
        this.scheduleCompleteListener = new StubScheduleCheckCompletedListener();
        scheduleCheckCompletedTopic.addListener(scheduleCompleteListener);
    }

    public Pipeline schedule(final PipelineConfig pipelineConfig, final BuildCause buildCause, final String approvedBy) {
        return schedule(pipelineConfig, buildCause, approvedBy, new DefaultSchedulingContext(approvedBy));
    }

    public Pipeline schedule(final PipelineConfig pipelineConfig, final BuildCause buildCause, final String approvedBy, final SchedulingContext schedulingContext) {
        return (Pipeline) transactionTemplate.execute(new TransactionCallback() {
            public Object doInTransaction(TransactionStatus status) {
                materialRepository.save(buildCause.getMaterialRevisions());
                Pipeline pipeline = instanceFactory.createPipelineInstance(pipelineConfig, buildCause, schedulingContext, "md5-test", new TimeProvider());
                pipeline = pipelineService.save(pipeline);
                return pipeline;
            }
        });
    }

    public ServerHealthState manuallySchedulePipelineWithRealMaterials(String pipeline, Username username) throws Exception {
        final HashMap<String, String> revisions = new HashMap<String, String>();
        return manuallySchedulePipelineWithRealMaterials(pipeline, username, revisions);
    }

    public ServerHealthState manuallySchedulePipelineWithRealMaterials(String pipeline, Username username, Map<String, String> pegging) throws Exception {
        updateMaterials(pipeline);
        ServerHealthStateOperationResult result = new ServerHealthStateOperationResult();
        final HashMap<String, String> environmentVariables = new HashMap<String, String>();
        HashMap<String, String> secureEnvironmentVariables = new HashMap<String, String>();
        pipelineScheduler.manualProduceBuildCauseAndSave(pipeline, username, new ScheduleOptions(pegging, environmentVariables, secureEnvironmentVariables), result);
        if (result.canContinue()) {
            waitForAnyScheduled(30);
        }
        return result.getServerHealthState();
    }

    public Map<String, BuildCause> waitForAnyScheduled(int seconds) {
        int count = 0;
        Map<String, BuildCause> afterLoad = pipelineScheduleQueue.toBeScheduled();
        while (afterLoad.isEmpty()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            afterLoad = pipelineScheduleQueue.toBeScheduled();
            if (count++ > seconds) {
                fail("Never scheduled");
            }
        }
        return afterLoad;
    }
    public void waitForNotScheduled(int seconds,String pipelineName) {
        int count = 0;
        Map<String, BuildCause> afterLoad = pipelineScheduleQueue.toBeScheduled();
        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            afterLoad = pipelineScheduleQueue.toBeScheduled();
            if (count++ > seconds) {
                return ;
            }

            BuildCause cause = afterLoad.get(pipelineName);
            assertNull(cause);
        }
    }

    public void autoSchedulePipelinesWithRealMaterials(String... pipelines) throws Exception {
        updateMaterials(pipelines);
        long startTime = System.currentTimeMillis();
        pipelineScheduler.onTimer();
        if (pipelines.length == 0) {
            assertAlwaysHappens(pipelines, ScheduleCheckMatcher.scheduleCheckCompleted(scheduleCompleteListener),
                    waitTime());
        } else {
            assertWillHappen(pipelines, ScheduleCheckMatcher.scheduleCheckCompleted(scheduleCompleteListener),
                    Timeout.TWENTY_SECONDS);

            long endTime = System.currentTimeMillis();
            setWaitTime(endTime - startTime);
        }

        scheduleCompleteListener.reset();
    }

    private void updateMaterials(String... pipelines) throws Exception {
        for (String pipeline : pipelines) {
            Materials materials = MaterialsMother.createMaterialsFromMaterialConfigs(goConfigService.getCurrentConfig().pipelineConfigByName(new CaseInsensitiveString(pipeline)).materialConfigs());
            for (Material material : materials) {
                materialDatabaseUpdater.updateMaterial(material);
            }
        }
    }

    private void setWaitTime(long time) {
        if(time > waitTime){
            waitTime = time;
        }
    }

    private long waitTime() {
        long result;

        if (waitTime > 0) {
            result = (waitTime > ONE_SECOND? waitTime:ONE_SECOND) * 2;
        } else {
            result = TEN_SECONDS.inMillis();
        }
        return result;
    }
}
