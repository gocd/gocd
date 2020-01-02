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
package com.thoughtworks.go.server.dao;

import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.helper.JobInstanceMother;
import com.thoughtworks.go.helper.PipelineMother;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.materials.DependencyMaterialUpdateNotifier;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.service.InstanceFactory;
import com.thoughtworks.go.server.transaction.AfterCompletionCallback;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.TimeProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionSynchronization;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.thoughtworks.go.helper.ModificationsMother.modifyOneFile;
import static com.thoughtworks.go.server.dao.DatabaseAccessHelper.assertIsInserted;
import static com.thoughtworks.go.server.dao.DatabaseAccessHelper.assertNotInserted;
import static com.thoughtworks.go.util.GoConstants.DEFAULT_APPROVED_BY;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
public class PipelineStateDaoIntegrationTest {

    @Autowired private PipelineStateDao pipelineStateDao;
    @Autowired private PipelineSqlMapDao pipelineSqlMapDao;
    @Autowired private DatabaseAccessHelper dbHelper;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private GoCache goCache;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private GoConfigDao goConfigDao;
    @Autowired private InstanceFactory instanceFactory;
    @Autowired private DependencyMaterialUpdateNotifier notifier;

    @Before
    public void setup() throws Exception {
        dbHelper.onSetUp();
        goCache.clear();
        GoConfigFileHelper configHelper = new GoConfigFileHelper();
        configHelper.usingCruiseConfigDao(goConfigDao);
        notifier.disableUpdates();
    }

    @After
    public void teardown() throws Exception {
        notifier.enableUpdates();
        dbHelper.onTearDown();

    }

    @Test
    public void shouldFindLockedPipelinesCaseInsensitively() throws Exception {
        Pipeline minglePipeline = schedulePipelineWithStages(PipelineMother.twoBuildPlansWithResourcesAndMaterials("mingle", "stage1", "stage2"));
        pipelineStateDao.lockPipeline(minglePipeline);
        PipelineState lockedPipelineState = pipelineStateDao.pipelineStateFor("mingle");
        assertThat(lockedPipelineState.getLockedBy().pipelineIdentifier(), is(minglePipeline.getIdentifier()));
        assertThat(lockedPipelineState.getLockedByPipelineId(), is(minglePipeline.getId()));
        assertThat(lockedPipelineState.getLockedByPipelineId(), is(not(0L)));
        lockedPipelineState = pipelineStateDao.pipelineStateFor("mInGlE");
        assertThat(lockedPipelineState.getLockedBy().pipelineIdentifier(), is(minglePipeline.getIdentifier()));
        assertThat(lockedPipelineState.getLockedByPipelineId(), is(minglePipeline.getId()));
        assertThat(lockedPipelineState.getLockedByPipelineId(), is(not(0L)));
    }

    @Test
    public void shouldBombWhenLockingPipelineThatHasAlreadyBeenLocked() throws Exception {
        String pipelineName = UUID.randomUUID().toString();
        Pipeline minglePipeline1 = schedulePipelineWithStages(PipelineMother.twoBuildPlansWithResourcesAndMaterials(pipelineName, "defaultStage"));
        Pipeline minglePipeline2 = schedulePipelineWithStages(PipelineMother.twoBuildPlansWithResourcesAndMaterials(pipelineName, "defaultStage"));

        pipelineStateDao.lockPipeline(minglePipeline1);

        assertThat(pipelineStateDao.pipelineStateFor(pipelineName).getLockedBy(), is(minglePipeline1.getFirstStage().getIdentifier()));

        try {
            pipelineStateDao.lockPipeline(minglePipeline2);
            fail("Should not be able to lock a different instance of an already locked pipeline");
        } catch (Exception e) {
            assertThat(e.getMessage(), is(String.format("Pipeline '%s' is already locked (counter = 1)", pipelineName)));
        }
    }

    @Test
    public void shouldNotBombWhenLockingTheSamePipelineInstanceThatHasAlreadyBeenLocked() throws Exception {
        String pipelineName = "pipeline";
        Pipeline minglePipeline1 = schedulePipelineWithStages(PipelineMother.twoBuildPlansWithResourcesAndMaterials(pipelineName, "defaultStage"));

        pipelineStateDao.lockPipeline(minglePipeline1);

        assertThat(pipelineStateDao.pipelineStateFor(pipelineName).getLockedBy(), is(minglePipeline1.getFirstStage().getIdentifier()));

        try {
            pipelineStateDao.lockPipeline(minglePipeline1);
        } catch (Exception e) {
            fail("Should not bomb trying to lock a locked pipeline instance but got: " + e.getMessage());
        }
    }

    @Test
    public void shouldUnlockPipelineInstance() throws Exception {
        String pipelineName = UUID.randomUUID().toString();
        Pipeline minglePipeline = schedulePipelineWithStages(PipelineMother.twoBuildPlansWithResourcesAndMaterials(pipelineName, "defaultStage"));
        TestAfterCompletionCallback afterLockCallback = new TestAfterCompletionCallback();

        pipelineStateDao.lockPipeline(minglePipeline, afterLockCallback);
        PipelineState pipelineState = pipelineStateDao.pipelineStateFor(pipelineName);
        assertThat(pipelineState.getLockedBy(), is(minglePipeline.getFirstStage().getIdentifier()));
        assertThat(pipelineState.getLockedByPipelineId(), is(minglePipeline.getId()));
        afterLockCallback.assertCalledWithStatus(TransactionSynchronization.STATUS_COMMITTED);

        TestAfterCompletionCallback unlockCallback = new TestAfterCompletionCallback();
        pipelineStateDao.unlockPipeline(pipelineName, unlockCallback);
        PipelineState pipelineState1 = pipelineStateDao.pipelineStateFor(pipelineName);
        assertThat(pipelineState1.getLockedBy(), is(nullValue()));
        assertThat(pipelineState1.getLockedByPipelineId(), is(0L));
        unlockCallback.assertCalledWithStatus(TransactionSynchronization.STATUS_COMMITTED);
    }

    @Test
    public void shouldReturnListOfAllLockedPipelines() throws Exception {
        Pipeline minglePipeline = schedulePipelineWithStages(PipelineMother.twoBuildPlansWithResourcesAndMaterials("mingle", "defaultStage"));
        Pipeline twistPipeline = schedulePipelineWithStages(PipelineMother.twoBuildPlansWithResourcesAndMaterials("twist", "defaultStage"));
        pipelineStateDao.lockPipeline(minglePipeline);
        pipelineStateDao.lockPipeline(twistPipeline);
        List<String> lockedPipelines = pipelineStateDao.lockedPipelines();
        assertThat(lockedPipelines.size(), is(2));
        assertThat(lockedPipelines, hasItem("mingle"));
        assertThat(lockedPipelines, hasItem("twist"));

        pipelineStateDao.unlockPipeline("mingle");
        lockedPipelines = pipelineStateDao.lockedPipelines();
        assertThat(lockedPipelines.size(), is(1));
        assertThat(lockedPipelines, hasItem("twist"));
    }
    @Test
    public void lockPipeline_shouldEnsureOnlyOneThreadCanLockAPipelineSuccessfully() throws Exception {
        List<Thread> threads = new ArrayList<>();
        final int[] errors = new int[1];
        for (int i = 0; i < 10; i++) {
            JobInstances jobInstances = new JobInstances(JobInstanceMother.completed("job"));
            Stage stage = new Stage("stage-1", jobInstances, "shilpa", null, "auto", new TimeProvider());
            final Pipeline pipeline = PipelineMother.pipeline("mingle", stage);
            pipeline.setCounter(i + 1);
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        pipelineStateDao.lockPipeline(pipeline);
                    } catch (Exception e) {
                        errors[0]++;
                    }
                }
            }, "thread-" + i);
            threads.add(thread);
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        assertThat(errors[0], is(9));
    }

    private Pipeline schedulePipelineWithStages(PipelineConfig pipelineConfig) throws Exception {
        BuildCause buildCause = BuildCause.createWithModifications(modifyOneFile(pipelineConfig), "");
        Pipeline pipeline = instanceFactory.createPipelineInstance(pipelineConfig, buildCause, new DefaultSchedulingContext(DEFAULT_APPROVED_BY), "md5-test", new TimeProvider());
        assertNotInserted(pipeline.getId());
        savePipeline(pipeline);
        assertIsInserted(pipeline.getId());
        return pipeline;
    }

    private void savePipeline(final Pipeline pipeline) {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                materialRepository.save(pipeline.getBuildCause().getMaterialRevisions());
                pipelineSqlMapDao.saveWithStages(pipeline);
            }
        });
    }

    private class TestAfterCompletionCallback implements AfterCompletionCallback {

        boolean called = false;
        Integer status = null;

        @Override
        public void execute(int status) {
            called = true;
            this.status = status;
        }

        void assertCalledWithStatus(int status) {
            assertTrue(called);
            assertThat(this.status, equalTo(status));
        }
    }
}
