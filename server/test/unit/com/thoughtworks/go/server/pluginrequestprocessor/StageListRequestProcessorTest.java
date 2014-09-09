package com.thoughtworks.go.server.pluginrequestprocessor;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.StageConfigIdentifier;
import com.thoughtworks.go.domain.StageIdentifier;
import com.thoughtworks.go.helper.StageConfigMother;
import com.thoughtworks.go.helper.StageMother;
import com.thoughtworks.go.plugin.api.request.GoApiRequest;
import com.thoughtworks.go.plugin.api.response.GoApiResponse;
import com.thoughtworks.go.plugin.infra.DefaultGoApplicationAccessor;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.StageService;
import org.junit.Before;
import org.junit.Test;

import static com.thoughtworks.go.helper.PipelineConfigMother.pipelineConfig;
import static java.util.Arrays.asList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class StageListRequestProcessorTest {

    private StageListRequestProcessor requestProcessor;
    private StageService stageService;
    private DefaultGoApplicationAccessor defaultGoApplicationAccessor;
    private GoConfigService goConfigService;

    @Before
    public void setUp() throws Exception {
        defaultGoApplicationAccessor = mock(DefaultGoApplicationAccessor.class);
        stageService = mock(StageService.class);
        goConfigService = mock(GoConfigService.class);
        requestProcessor = new StageListRequestProcessor(defaultGoApplicationAccessor, stageService, goConfigService);
    }

    @Test
    public void shouldRegisterWithGoApplicationAccessor() throws Exception {
        verify(defaultGoApplicationAccessor).registerProcessorFor(StageListRequestProcessor.REQUEST, requestProcessor);
    }

    @Test
    public void shouldReturnSuccessResponse() throws Exception {
        GoApiRequest goApiRequest = mock(GoApiRequest.class);
        when(stageService.getAllDistinctStages()).thenReturn(asList(new StageConfigIdentifier("pipeline-one", "stage"), new StageConfigIdentifier("pipeline-two", "stage")));

        PipelineConfig pipelineConfigOne = pipelineConfig("pipeline-one", StageConfigMother.stageConfig("stage"));
        PipelineConfig pipelineConfigTwo = pipelineConfig("pipeline-two", StageConfigMother.stageConfig("diff-stage"));

        when(goConfigService.findGroupNameByPipeline(new CaseInsensitiveString("pipeline-one"))).thenReturn("g-one");
        when(goConfigService.pipelineConfigNamed(new CaseInsensitiveString("pipeline-one"))).thenReturn(pipelineConfigOne);

        when(goConfigService.findGroupNameByPipeline(new CaseInsensitiveString("pipeline-two"))).thenReturn("g-one");
        when(goConfigService.pipelineConfigNamed(new CaseInsensitiveString("pipeline-two"))).thenReturn(pipelineConfigTwo);

        GoApiResponse response = requestProcessor.process(goApiRequest);

        assertThat(response.responseCode(), is(200));
        assertThat(response.responseBody(),
                is("[{\"pipelineGroup\":\"g-one\",\"stageName\":\"stage\",\"existInConfig\":true,\"pipelineName\":\"pipeline-one\"},{\"pipelineGroup\":\"g-one\",\"stageName\":\"stage\",\"existInConfig\":false,\"pipelineName\":\"pipeline-two\"}]"));
    }

    @Test
    public void shouldMarkAllStageAsDoNotExistInConfigWhenPipelineNotFoundExceptionIsThrown() throws Exception {
        GoApiRequest goApiRequest = mock(GoApiRequest.class);
        when(stageService.getAllDistinctStages()).thenReturn(asList(
                new StageConfigIdentifier("pipeline-one", "stage"), new StageConfigIdentifier("pipeline-two", "stage")
        ));

        PipelineConfig pipelineConfig = pipelineConfig("pipeline-one", StageConfigMother.stageConfig("stage"));
        when(goConfigService.findGroupNameByPipeline(new CaseInsensitiveString("pipeline-one"))).thenReturn("g-one");
        when(goConfigService.pipelineConfigNamed(new CaseInsensitiveString("pipeline-one"))).thenReturn(pipelineConfig);
        when(goConfigService.pipelineConfigNamed(new CaseInsensitiveString("pipeline-two"))).thenThrow(new PipelineNotFoundException(""));

        GoApiResponse response = requestProcessor.process(goApiRequest);

        assertThat(response.responseCode(), is(200));
        assertThat(response.responseBody(),
                is("[{\"pipelineGroup\":\"g-one\",\"stageName\":\"stage\",\"existInConfig\":true,\"pipelineName\":\"pipeline-one\"},{\"stageName\":\"stage\",\"existInConfig\":false,\"pipelineName\":\"pipeline-two\"}]"));
    }

    @Test
    public void shouldCacheStageDetails() throws Exception {

        GoApiRequest goApiRequest = mock(GoApiRequest.class);
        when(stageService.getAllDistinctStages()).thenReturn(asList(
                new StageConfigIdentifier("pipeline-one", "stage"), new StageConfigIdentifier("pipeline-two", "stage")
        ));

        PipelineConfig pipelineConfig = pipelineConfig("pipeline-one", StageConfigMother.stageConfig("stage"));
        when(goConfigService.findGroupNameByPipeline(new CaseInsensitiveString("pipeline-one"))).thenReturn("g-one");
        when(goConfigService.pipelineConfigNamed(new CaseInsensitiveString("pipeline-one"))).thenReturn(pipelineConfig);

        requestProcessor.process(goApiRequest);
        requestProcessor.process(goApiRequest);

        verify(stageService, times(1)).getAllDistinctStages();
        verify(goConfigService, times(1)).findGroupNameByPipeline(new CaseInsensitiveString("pipeline-one"));
        verify(goConfigService, times(1)).pipelineConfigNamed(new CaseInsensitiveString("pipeline-one"));
    }

    @Test
    public void shouldUpdateCacheWhenStageStatusChanges() throws Exception {
        GoApiRequest goApiRequest = mock(GoApiRequest.class);
        when(stageService.getAllDistinctStages()).thenReturn(asList(new StageConfigIdentifier("pipeline-one", "stage")));

        //first call
        PipelineConfig pipelineConfig = pipelineConfig("pipeline-one", StageConfigMother.stageConfig("stage"));
        when(goConfigService.findGroupNameByPipeline(new CaseInsensitiveString("pipeline-one"))).thenReturn("g-one");
        when(goConfigService.pipelineConfigNamed(new CaseInsensitiveString("pipeline-one"))).thenReturn(pipelineConfig);
        requestProcessor.process(goApiRequest);

        //stage status changed
        Stage newStage = StageMother.custom("stage");
        newStage.setIdentifier(new StageIdentifier("pipeline-two", 1, "new-stage", "2"));
        when(goConfigService.findGroupNameByPipeline(new CaseInsensitiveString("pipeline-two"))).thenReturn("g-one");
        requestProcessor.stageStatusChanged(newStage);

        //second call
        GoApiResponse response = requestProcessor.process(goApiRequest);

        assertThat(response.responseCode(), is(200));
        assertThat(response.responseBody(),
                is("[{\"pipelineGroup\":\"g-one\",\"stageName\":\"stage\",\"existInConfig\":true,\"pipelineName\":\"pipeline-one\"},{\"pipelineGroup\":\"g-one\",\"stageName\":\"stage\",\"existInConfig\":true,\"pipelineName\":\"pipeline-two\"}]"));

    }

    @Test
    public void shouldUpdateStagesMapOnConfigChange() throws Exception {

        GoApiRequest goApiRequest = mock(GoApiRequest.class);
        when(stageService.getAllDistinctStages()).thenReturn(asList(new StageConfigIdentifier("pipeline-one", "stage"),
                new StageConfigIdentifier("pipeline-two", "stage"),
                new StageConfigIdentifier("pipeline-three", "stage")));

        PipelineConfig pipelineConfigOne = pipelineConfig("pipeline-one", StageConfigMother.stageConfig("stage"));
        PipelineConfig pipelineConfigTwo = pipelineConfig("pipeline-two", StageConfigMother.stageConfig("stage"));
        PipelineConfig pipelineConfigThree = pipelineConfig("pipeline-three", StageConfigMother.stageConfig("stage"));

        when(goConfigService.findGroupNameByPipeline(new CaseInsensitiveString("pipeline-one"))).thenReturn("g-one").thenReturn("g-two");
        when(goConfigService.findGroupNameByPipeline(new CaseInsensitiveString("pipeline-two"))).thenReturn("g-one");
        when(goConfigService.findGroupNameByPipeline(new CaseInsensitiveString("pipeline-three"))).thenReturn("g-one");

        when(goConfigService.pipelineConfigNamed(new CaseInsensitiveString("pipeline-one"))).thenReturn(pipelineConfigOne);
        when(goConfigService.pipelineConfigNamed(new CaseInsensitiveString("pipeline-two"))).thenReturn(pipelineConfigTwo);
        when(goConfigService.pipelineConfigNamed(new CaseInsensitiveString("pipeline-three"))).thenReturn(pipelineConfigThree);
        requestProcessor.process(goApiRequest);

        CruiseConfig updatedConfig = mock(CruiseConfig.class);
        PipelineConfig newPipelineOneConfig = mock(PipelineConfig.class);
        when(newPipelineOneConfig.findBy(new CaseInsensitiveString("stage"))).thenReturn(new StageConfig());
        PipelineConfig newPipelineTwoConfig = mock(PipelineConfig.class);
        when(newPipelineTwoConfig.findBy(new CaseInsensitiveString("stage"))).thenReturn(null);

        when(updatedConfig.pipelineConfigByName(new CaseInsensitiveString("pipeline-one"))).thenReturn(newPipelineOneConfig);
        when(updatedConfig.pipelineConfigByName(new CaseInsensitiveString("pipeline-two"))).thenThrow(new PipelineNotFoundException(""));
        when(updatedConfig.pipelineConfigByName(new CaseInsensitiveString("pipeline-three"))).thenReturn(newPipelineTwoConfig);
        requestProcessor.onConfigChange(updatedConfig);
        GoApiResponse response = requestProcessor.process(goApiRequest);
        assertThat(response.responseCode(), is(200));
        assertThat(response.responseBody(),
                is("[{\"pipelineGroup\":\"\",\"stageName\":\"stage\",\"existInConfig\":false,\"pipelineName\":\"pipeline-three\"}," +
                        "{\"pipelineGroup\":\"g-two\",\"stageName\":\"stage\",\"existInConfig\":true,\"pipelineName\":\"pipeline-one\"}," +
                        "{\"pipelineGroup\":\"\",\"stageName\":\"stage\",\"existInConfig\":false,\"pipelineName\":\"pipeline-two\"}]"));
    }
}