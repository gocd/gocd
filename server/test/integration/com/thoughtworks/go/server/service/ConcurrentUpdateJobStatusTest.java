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

package com.thoughtworks.go.server.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.UUID;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.domain.DefaultSchedulingContext;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.JobInstances;
import com.thoughtworks.go.domain.JobState;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.fixture.PipelineWithTwoStages;
import com.thoughtworks.go.helper.PipelineMother;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.GoConfigFileHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import static com.thoughtworks.go.helper.ModificationsMother.modifySomeFiles;
import static com.thoughtworks.go.util.ExceptionUtils.bombIf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
@Ignore
public class ConcurrentUpdateJobStatusTest {
    @Autowired private BuildRepositoryService buildRepositoryService;
    @Autowired private ScheduleService scheduleService;
    @Autowired private DatabaseAccessHelper dbHelper;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private TransactionTemplate transactionTemplate;
    
    private static GoConfigFileHelper configHelper = new GoConfigFileHelper();
    private PipelineWithTwoStages fixture;

    @Before
    public void setUp() throws Exception {
        configHelper.onSetUp();
        fixture = new PipelineWithTwoStages(materialRepository, transactionTemplate);
        fixture.usingConfigHelper(configHelper).usingDbHelper(dbHelper).onSetUp();
    }

    @After
    public void tearDown() throws Exception {
        fixture.onTearDown();
        dbHelper.onTearDown();
    }

    @Test
    public void shouldCancelStage() throws SQLException, InterruptedException {
        Pipeline pipeline = fixture.createPipelineWithFirstStageAssigned();
        final Stage firstStage = pipeline.getFirstStage();
        final JobInstance instance = firstStage.getJobInstances().first();
        final ArrayList failures = new ArrayList();

        Runnable agentWork = new AgentUpdateWork(instance);
        Runnable cancelWork = new UserCancelWork(firstStage, instance, failures);

        ArrayList<Thread> threads = mixAgentWorkAndCancelWork(agentWork, cancelWork);

        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }

        assertThat(failures.toString(), failures.size(), is(0));
    }

    // TTL Support
    @Test
    public void shouldUpdateStatusOfRunOnAllJobs() throws SQLException, InterruptedException {
        for(int i=0;i < 100 ; i++){
            configHelper.addAgent("some_host", String.format("agent-%s-%s", i, UUID.randomUUID()));
        }
        String pipelineName = "myPipeline";
        String stageName = "firstStage";
        String jobName = "firstJob";
        configHelper.addPipeline(pipelineName, stageName, jobName);
        configHelper.makeJobRunOnAllAgents(pipelineName, stageName, jobName);
        PipelineConfig pipelineConfig = configHelper.currentConfig().pipelineConfigByName(new CaseInsensitiveString(pipelineName));
        Pipeline pipeline = createPipelineWithFirstStageAssigned(pipelineConfig);

        JobInstances jobInstances = pipeline.findStage(stageName).getJobInstances();

        //Create update and cancel threads for each instance
        ArrayList<Thread> threads =  createThreadsForRunOnAllJobInstances(pipeline.findStage(stageName),jobInstances);
        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }

    }

    private ArrayList<Thread> createThreadsForRunOnAllJobInstances(Stage stage, JobInstances jobInstances) {
        ArrayList<Thread> threads = new ArrayList<Thread>();
        final ArrayList failures = new ArrayList();
        for (JobInstance jobInstance:jobInstances) {
           Runnable agentWork = new AgentUpdateWork(jobInstance);
           Runnable cancelWork = new UserCancelWork(stage, jobInstance, failures);
           threads.add(new Thread(agentWork));
      //     threads.add(new Thread(cancelWork));
        }
        return threads;

    }

    private Pipeline createPipelineWithFirstStageAssigned(PipelineConfig pipelineConfig) {
        Pipeline mostRecent = dbHelper.getPipelineDao().mostRecentPipeline(pipelineConfig.name().toString());
        bombIf(mostRecent.getStages().byName(pipelineConfig.getFirstStageConfig().name().toString()).isActive(),
                "Can not schedule new pipeline: the first stage is still running");

        Pipeline pipeline = schedulePipeline(pipelineConfig);
        dbHelper.savePipelineWithStagesAndMaterials(pipeline);
        pipeline.getStages().byName(pipelineConfig.getFirstStageConfig().name().toString()).getJobInstances();
        return dbHelper.getPipelineDao().loadPipeline(pipeline.getId());
    }

    private Pipeline schedulePipeline(PipelineConfig pipelineConfig) {
        return schedulePipeline(modifySomeFiles(pipelineConfig), pipelineConfig);
    }

    private Pipeline schedulePipeline(final BuildCause buildCause, final PipelineConfig pipelineConfig) {
        return (Pipeline) transactionTemplate.execute(new TransactionCallback() {
            public Object doInTransaction(TransactionStatus status) {
                materialRepository.save(buildCause.getMaterialRevisions());
                return PipelineMother.scheduleWithContext(pipelineConfig, buildCause, new DefaultSchedulingContext("admin", configHelper.currentConfig().agents()));
            }
        });
    }

    private ArrayList<Thread> mixAgentWorkAndCancelWork(Runnable agentWork, Runnable cancelWork) {
        ArrayList<Thread> threads = new ArrayList<Thread>();
        for (int i = 0; i < 100; i++) {
            if (i % 4 == 0) {
                threads.add(new Thread(cancelWork));
            } else {
                threads.add(new Thread(agentWork));

            }
        }
        return threads;
    }

    private class AgentUpdateWork implements Runnable {
        private final JobInstance instance;

        public AgentUpdateWork(JobInstance instance) {
            this.instance = instance;
        }

        public void run() {
            try {
                buildRepositoryService.updateStatusFromAgent(instance.getIdentifier(), JobState.Building, instance.getAgentUuid());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private class UserCancelWork implements Runnable {
        private final Stage firstStage;
        private final JobInstance instance;
        private final ArrayList failures;

        public UserCancelWork(Stage firstStage, JobInstance instance, ArrayList failures) {
            this.firstStage = firstStage;
            this.instance = instance;
            this.failures = failures;
        }

        public void run() {
            try {
                scheduleService.cancelAndTriggerRelevantStages(firstStage.getId(), null, null);
                JobInstance loaded = dbHelper.getBuildInstanceDao().buildByIdWithTransitions(instance.getId());
                if (loaded.getState() != JobState.Completed) {
                    failures.add("Expected: Completed, got: " + loaded.getState() + "\n");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
