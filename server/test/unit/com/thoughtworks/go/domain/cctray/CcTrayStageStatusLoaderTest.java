package com.thoughtworks.go.domain.cctray;

import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.StageConfig;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.activity.ProjectStatus;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.helper.StageConfigMother;
import com.thoughtworks.go.helper.StageMother;
import com.thoughtworks.go.server.domain.StageIdentity;
import com.thoughtworks.go.server.service.StageService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class CcTrayStageStatusLoaderTest {
    @Mock
    private StageService stageService;
    @Mock
    private CcTrayStageStatusChangeHandler stageChangeHandler;

    private CcTrayStageStatusLoader loader;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        loader = new CcTrayStageStatusLoader(stageService, stageChangeHandler);
    }

    @Test
    public void shouldNotHaveAnyStatusesIfAStageCannotBeFoundInDB() throws Exception {
        setupStagesInDB(new StageIdentity("pipeline1", "stage1", 12L), new StageIdentity("pipeline2", "stage2", 14L));

        List<ProjectStatus> actualStatuses = loader.getStatusesForStageAndJobsOf(pipelineConfigFor("pipeline1"), stageConfigFor("non-existent-stage"));

        assertThat(actualStatuses, is(Collections.<ProjectStatus>emptyList()));
    }

    @Test
    public void shouldConvertToStatusesIfAStageIsFoundInDB() throws Exception {
        List<ProjectStatus> expectedStatuses = asList(new ProjectStatus("pipeline1 :: stage1", "Sleeping", "some-status", "some-label", new Date(), "some-url"));
        List<Stage> stages = setupStagesInDB(new StageIdentity("pipeline1", "stage1", 12L), new StageIdentity("pipeline2", "stage2", 14L));
        when(stageChangeHandler.statusesOfStageAndItsJobsFor(stages.get(0))).thenReturn(expectedStatuses);

        List<ProjectStatus> actualStatuses = loader.getStatusesForStageAndJobsOf(pipelineConfigFor("pipeline1"), stageConfigFor("stage1"));

        assertThat(actualStatuses, is(expectedStatuses));
    }

    @Test
    public void shouldCacheResultOfLatestStageInstancesOnce() throws Exception {
        setupStagesInDB(new StageIdentity("pipeline1", "stage1", 12L), new StageIdentity("pipeline2", "stage2", 14L));

        loader.getStatusesForStageAndJobsOf(pipelineConfigFor("pipeline1"), stageConfigFor("stage1"));
        loader.getStatusesForStageAndJobsOf(pipelineConfigFor("pipeline1"), stageConfigFor("stage2"));
        loader.getStatusesForStageAndJobsOf(pipelineConfigFor("pipeline1"), stageConfigFor("stage-some-nonexistent-one"));

        verify(stageService, times(1)).findLatestStageInstances();
    }

    private List<Stage> setupStagesInDB(StageIdentity... stageIdentities) {
        when(stageService.findLatestStageInstances()).thenReturn(asList(stageIdentities));

        List<Stage> stages = new ArrayList<Stage>();
        for (StageIdentity identity : stageIdentities) {
            Stage stage = StageMother.custom(identity.getPipelineName() + " - " + identity.getStageName());
            when(stageService.stageById(identity.getStageId())).thenReturn(stage);
            stages.add(stage);
        }

        return stages;
    }

    private PipelineConfig pipelineConfigFor(String pipelineName) {
        return PipelineConfigMother.pipelineConfig(pipelineName);
    }

    private StageConfig stageConfigFor(String stageName) {
        return StageConfigMother.custom(stageName);
    }
}